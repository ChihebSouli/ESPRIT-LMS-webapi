package com.esprit.lms.purchases.repository;

import com.esprit.lms.purchases.entity.PaymentStatus;
import com.esprit.lms.purchases.entity.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {

    Page<Purchase> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<Purchase> findByGatewaySessionId(String sessionId);

    boolean existsByUserIdAndItemIdAndPaymentStatus(UUID userId, UUID itemId, PaymentStatus status);

    Page<Purchase> findByPaymentStatusOrderByCreatedAtDesc(PaymentStatus status, Pageable pageable);
}
