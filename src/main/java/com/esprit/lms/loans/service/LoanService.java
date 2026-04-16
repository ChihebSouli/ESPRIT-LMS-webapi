package com.esprit.lms.loans.service;

import com.esprit.lms.admin.service.SystemConfigService;
import com.esprit.lms.catalog.entity.PhysicalCopy;
import com.esprit.lms.catalog.repository.PhysicalCopyRepository;
import com.esprit.lms.loans.entity.Loan;
import com.esprit.lms.loans.entity.LoanStatus;
import com.esprit.lms.loans.repository.LoanRepository;
import com.esprit.lms.shared.exception.ApiException;
import com.esprit.lms.users.entity.User;
import com.esprit.lms.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepo;
    private final PhysicalCopyRepository copyRepo;
    private final UserService userService;
    private final SystemConfigService configService;

    /**
     * Reserve a physical book copy.
     * Checks borrow limit → finds available copy → creates RESERVED loan.
     */
    @Transactional
    public Loan reserve(UUID itemId) {
        User user = userService.getOrCreateCurrentUser();

        // Check borrow limit
        int limit = user.getBorrowLimit() != null
                ? user.getBorrowLimit()
                : configService.getDefaultBorrowLimit();

        int activeCount = loanRepo.countByUserIdAndStatusIn(user.getId(),
                List.of(LoanStatus.RESERVED, LoanStatus.ACTIVE));

        if (activeCount >= limit) {
            throw ApiException.conflict("Borrow limit reached (" + limit + "). Return items before reserving more.");
        }

        // Find first available copy (atomic with SELECT FOR UPDATE)
        PhysicalCopy copy = copyRepo.findFirstByItemIdAndIsAvailableTrue(itemId)
                .orElseThrow(() -> ApiException.conflict("No copies currently available for this item."));

        // Mark copy as unavailable
        copy.setIsAvailable(false);
        copyRepo.save(copy);

        // Create reservation
        Loan loan = Loan.builder()
                .user(user)
                .copy(copy)
                .status(LoanStatus.RESERVED)
                .build();

        loan = loanRepo.save(loan);
        log.info("Reservation created: user={}, item={}, loan={}", user.getEmail(), itemId, loan.getId());

        return loan;
    }

    /**
     * Checkout: RESERVED → ACTIVE. Sets due date.
     * Called by librarian when student picks up the book.
     */
    @Transactional
    public Loan checkout(UUID loanId) {
        Loan loan = findLoan(loanId);
        if (loan.getStatus() != LoanStatus.RESERVED) {
            throw ApiException.badRequest("Can only checkout a RESERVED loan. Current status: " + loan.getStatus());
        }

        loan.setStatus(LoanStatus.ACTIVE);
        loan.setBorrowedAt(LocalDateTime.now());
        loan.setDueAt(LocalDateTime.now().plusDays(configService.getLoanDurationDays()));

        log.info("Loan checked out: {}", loanId);
        return loanRepo.save(loan);
    }

    /**
     * Return: ACTIVE/OVERDUE → RETURNED. Releases the physical copy.
     */
    @Transactional
    public Loan returnBook(UUID loanId) {
        Loan loan = findLoan(loanId);
        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.OVERDUE) {
            throw ApiException.badRequest("Can only return ACTIVE or OVERDUE loans. Current: " + loan.getStatus());
        }

        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnedAt(LocalDateTime.now());

        // Release the physical copy
        PhysicalCopy copy = loan.getCopy();
        copy.setIsAvailable(true);
        copyRepo.save(copy);

        log.info("Book returned: loan={}", loanId);
        return loanRepo.save(loan);
    }

    /**
     * Extend: adds renewal days. One extension per loan.
     */
    @Transactional
    public Loan extend(UUID loanId) {
        Loan loan = findLoan(loanId);
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw ApiException.badRequest("Can only extend ACTIVE loans.");
        }
        if (Boolean.TRUE.equals(loan.getRenewed())) {
            throw ApiException.conflict("This loan has already been extended.");
        }

        loan.setDueAt(loan.getDueAt().plusDays(configService.getRenewalDays()));
        loan.setRenewed(true);

        log.info("Loan extended: {} → new due date {}", loanId, loan.getDueAt());
        return loanRepo.save(loan);
    }

    // ── Query ──

    public Page<Loan> getMyLoans(Pageable pageable) {
        User user = userService.getOrCreateCurrentUser();
        return loanRepo.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
    }

    public Page<Loan> getAllLoans(LoanStatus status, Pageable pageable) {
        if (status != null) {
            return loanRepo.findByStatusOrderByCreatedAtDesc(status, pageable);
        }
        return loanRepo.findAll(pageable);
    }

    // ── Scheduled Jobs ──

    /**
     * Hourly: Cancel RESERVED loans older than 48h and release copies.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireReservations() {
        int hours = configService.getReservationExpiryHours();
        LocalDateTime threshold = LocalDateTime.now().minusHours(hours);

        List<Loan> expired = loanRepo.findByStatusAndReservedAtBefore(LoanStatus.RESERVED, threshold);
        for (Loan loan : expired) {
            loan.setStatus(LoanStatus.CANCELLED);
            loan.getCopy().setIsAvailable(true);
            copyRepo.save(loan.getCopy());
            loanRepo.save(loan);
        }

        if (!expired.isEmpty()) {
            log.info("Expired {} reservations older than {}h", expired.size(), hours);
        }
    }

    /**
     * Daily 8AM: Detect overdue loans.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void detectOverdue() {
        List<Loan> overdue = loanRepo.findOverdueLoans(LocalDateTime.now());
        for (Loan loan : overdue) {
            loan.setStatus(LoanStatus.OVERDUE);
            loanRepo.save(loan);
            // TODO Sprint 10: trigger overdue email notification
        }

        if (!overdue.isEmpty()) {
            log.info("Marked {} loans as OVERDUE", overdue.size());
        }
    }

    private Loan findLoan(UUID id) {
        return loanRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Loan not found: " + id));
    }
}
