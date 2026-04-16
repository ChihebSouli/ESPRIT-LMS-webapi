package com.esprit.lms.catalog.repository;

import com.esprit.lms.catalog.entity.PhysicalCopy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhysicalCopyRepository extends JpaRepository<PhysicalCopy, UUID> {

    List<PhysicalCopy> findByItemId(UUID itemId);

    int countByItemIdAndIsAvailableTrue(UUID itemId);

    Optional<PhysicalCopy> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

    /**
     * Find first available copy for an item (used by reservation).
     */
    Optional<PhysicalCopy> findFirstByItemIdAndIsAvailableTrue(UUID itemId);
}
