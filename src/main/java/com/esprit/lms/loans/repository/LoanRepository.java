package com.esprit.lms.loans.repository;

import com.esprit.lms.loans.entity.Loan;
import com.esprit.lms.loans.entity.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    Page<Loan> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Loan> findByStatusOrderByCreatedAtDesc(LoanStatus status, Pageable pageable);

    int countByUserIdAndStatusIn(UUID userId, List<LoanStatus> statuses);

    /**
     * Find all RESERVED loans older than expiry threshold (for cancellation job).
     */
    List<Loan> findByStatusAndReservedAtBefore(LoanStatus status, LocalDateTime threshold);

    /**
     * Find all ACTIVE loans past due date (for overdue detection job).
     */
    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.dueAt < :now")
    List<Loan> findOverdueLoans(@Param("now") LocalDateTime now);

    /**
     * Find loans expiring soon (for reminder emails).
     */
    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.dueAt BETWEEN :now AND :soon AND l.expiryReminderSent = false")
    List<Loan> findExpiringSoon(@Param("now") LocalDateTime now, @Param("soon") LocalDateTime soon);
}
