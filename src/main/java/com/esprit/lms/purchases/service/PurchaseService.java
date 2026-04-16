package com.esprit.lms.purchases.service;

import com.esprit.lms.catalog.entity.CatalogItem;
import com.esprit.lms.catalog.repository.CatalogItemRepository;
import com.esprit.lms.purchases.entity.PaymentMethod;
import com.esprit.lms.purchases.entity.PaymentStatus;
import com.esprit.lms.purchases.entity.Purchase;
import com.esprit.lms.purchases.repository.PurchaseRepository;
import com.esprit.lms.shared.exception.ApiException;
import com.esprit.lms.users.entity.User;
import com.esprit.lms.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepo;
    private final CatalogItemRepository catalogRepo;
    private final UserService userService;

    /**
     * Check if a user has purchased (PAID) a specific item.
     */
    public boolean hasAccess(UUID userId, UUID itemId) {
        return purchaseRepo.existsByUserIdAndItemIdAndPaymentStatus(userId, itemId, PaymentStatus.PAID);
    }

    /**
     * Initiate online payment (Flouci or Stripe).
     * Returns the gateway session ID and payment URL.
     */
    @Transactional
    public Map<String, String> initiateOnlinePayment(UUID itemId, PaymentMethod method) {
        User user = userService.getOrCreateCurrentUser();
        CatalogItem item = catalogRepo.findById(itemId)
                .orElseThrow(() -> ApiException.notFound("Item not found: " + itemId));

        if (item.isFree()) {
            throw ApiException.badRequest("This item is free — no payment required.");
        }

        // TODO: Call Flouci/Stripe API to create a payment session
        // For now, create a PENDING purchase record
        String sessionId = "session_" + UUID.randomUUID();

        Purchase purchase = Purchase.builder()
                .user(user)
                .item(item)
                .amount(item.getPrice())
                .paymentMethod(method)
                .paymentStatus(PaymentStatus.PENDING)
                .gatewaySessionId(sessionId)
                .build();

        purchaseRepo.save(purchase);
        log.info("Online payment initiated: user={}, item={}, method={}", user.getEmail(), itemId, method);

        return Map.of(
                "sessionId", sessionId,
                "paymentUrl", "https://payment-gateway.example.com/" + sessionId  // TODO: real URL
        );
    }

    /**
     * Process payment webhook from Flouci/Stripe.
     * HMAC validation should happen BEFORE calling this method.
     */
    @Transactional
    public void processWebhook(String gatewaySessionId, boolean success, String transactionRef) {
        Purchase purchase = purchaseRepo.findByGatewaySessionId(gatewaySessionId)
                .orElseThrow(() -> ApiException.notFound("Unknown payment session: " + gatewaySessionId));

        if (purchase.getPaymentStatus() == PaymentStatus.PAID) {
            log.warn("Duplicate webhook for already-paid session: {}", gatewaySessionId);
            return; // Idempotent — already processed
        }

        purchase.setPaymentStatus(success ? PaymentStatus.PAID : PaymentStatus.FAILED);
        purchase.setTransactionRef(transactionRef);
        if (success) {
            purchase.setPurchasedAt(LocalDateTime.now());
        }

        purchaseRepo.save(purchase);
        log.info("Payment {} for session {}: {}", success ? "SUCCESS" : "FAILED", gatewaySessionId, transactionRef);
    }

    /**
     * Create a desk payment order (student initiates, librarian confirms later).
     */
    @Transactional
    public Purchase createDeskOrder(UUID itemId) {
        User user = userService.getOrCreateCurrentUser();
        CatalogItem item = catalogRepo.findById(itemId)
                .orElseThrow(() -> ApiException.notFound("Item not found: " + itemId));

        Purchase purchase = Purchase.builder()
                .user(user)
                .item(item)
                .amount(item.getPrice())
                .paymentMethod(PaymentMethod.DESK)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        return purchaseRepo.save(purchase);
    }

    /**
     * Librarian confirms desk payment.
     */
    @Transactional
    public Purchase confirmDeskPayment(UUID purchaseId) {
        Purchase purchase = purchaseRepo.findById(purchaseId)
                .orElseThrow(() -> ApiException.notFound("Purchase not found: " + purchaseId));

        if (purchase.getPaymentMethod() != PaymentMethod.DESK) {
            throw ApiException.badRequest("Only desk payments can be confirmed here.");
        }

        purchase.setPaymentStatus(PaymentStatus.PAID);
        purchase.setPurchasedAt(LocalDateTime.now());

        log.info("Desk payment confirmed: {}", purchaseId);
        return purchaseRepo.save(purchase);
    }

    // ── Queries ──

    public Page<Purchase> getMyPurchases(Pageable pageable) {
        User user = userService.getOrCreateCurrentUser();
        return purchaseRepo.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
    }

    public Page<Purchase> getPendingDeskOrders(Pageable pageable) {
        return purchaseRepo.findByPaymentStatusOrderByCreatedAtDesc(PaymentStatus.PENDING, pageable);
    }
}
