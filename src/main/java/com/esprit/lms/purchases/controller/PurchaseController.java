package com.esprit.lms.purchases.controller;

import com.esprit.lms.purchases.entity.PaymentMethod;
import com.esprit.lms.purchases.entity.Purchase;
import com.esprit.lms.purchases.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    /**
     * POST /api/v1/purchases/online — Initiate online payment.
     */
    @PostMapping("/online")
    public Map<String, String> initiateOnlinePayment(
            @RequestParam UUID itemId,
            @RequestParam(defaultValue = "FLOUCI") PaymentMethod method) {
        return purchaseService.initiateOnlinePayment(itemId, method);
    }

    /**
     * POST /api/v1/purchases/webhook — Payment gateway webhook.
     * This endpoint is publicly accessible (permitAll in SecurityConfig).
     * TODO: Add HMAC signature validation before processing.
     */
    @PostMapping("/webhook")
    public Map<String, String> handleWebhook(@RequestBody Map<String, Object> payload) {
        String sessionId = (String) payload.get("sessionId");
        boolean success = Boolean.TRUE.equals(payload.get("success"));
        String transactionRef = (String) payload.get("transactionRef");

        // TODO: Validate HMAC signature from headers BEFORE processing
        purchaseService.processWebhook(sessionId, success, transactionRef);
        return Map.of("status", "processed");
    }

    /**
     * POST /api/v1/purchases/desk — Student creates a desk payment order.
     */
    @PostMapping("/desk")
    @ResponseStatus(HttpStatus.CREATED)
    public Purchase createDeskOrder(@RequestParam UUID itemId) {
        return purchaseService.createDeskOrder(itemId);
    }

    /**
     * PUT /api/v1/purchases/desk/{id}/pay — Librarian confirms desk payment.
     */
    @PutMapping("/desk/{id}/pay")
    public Purchase confirmDeskPayment(@PathVariable UUID id) {
        return purchaseService.confirmDeskPayment(id);
    }

    /**
     * GET /api/v1/purchases/my — Student's purchase history.
     */
    @GetMapping("/my")
    public Page<Purchase> getMyPurchases(@PageableDefault(size = 20) Pageable pageable) {
        return purchaseService.getMyPurchases(pageable);
    }

    /**
     * GET /api/v1/purchases/desk/pending — Librarian: pending desk orders.
     */
    @GetMapping("/desk/pending")
    public Page<Purchase> getPendingDeskOrders(@PageableDefault(size = 20) Pageable pageable) {
        return purchaseService.getPendingDeskOrders(pageable);
    }
}
