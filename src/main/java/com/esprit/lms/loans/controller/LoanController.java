package com.esprit.lms.loans.controller;

import com.esprit.lms.loans.entity.Loan;
import com.esprit.lms.loans.entity.LoanStatus;
import com.esprit.lms.loans.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    /**
     * POST /api/v1/loans/reserve/{itemId} — Reserve a book copy.
     */
    @PostMapping("/reserve/{itemId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Loan reserve(@PathVariable UUID itemId) {
        return loanService.reserve(itemId);
    }

    /**
     * POST /api/v1/loans/{id}/checkout — Librarian checks out a reservation.
     */
    @PostMapping("/{id}/checkout")
    public Loan checkout(@PathVariable UUID id) {
        return loanService.checkout(id);
    }

    /**
     * POST /api/v1/loans/{id}/return — Librarian processes a return.
     */
    @PostMapping("/{id}/return")
    public Loan returnBook(@PathVariable UUID id) {
        return loanService.returnBook(id);
    }

    /**
     * POST /api/v1/loans/{id}/extend — Extend loan by renewal period.
     */
    @PostMapping("/{id}/extend")
    public Loan extend(@PathVariable UUID id) {
        return loanService.extend(id);
    }

    /**
     * GET /api/v1/loans/my — Student's own loans.
     */
    @GetMapping("/my")
    public Page<Loan> getMyLoans(@PageableDefault(size = 20) Pageable pageable) {
        return loanService.getMyLoans(pageable);
    }

    /**
     * GET /api/v1/loans — Librarian view of all loans.
     */
    @GetMapping
    public Page<Loan> getAllLoans(
            @RequestParam(required = false) LoanStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return loanService.getAllLoans(status, pageable);
    }
}
