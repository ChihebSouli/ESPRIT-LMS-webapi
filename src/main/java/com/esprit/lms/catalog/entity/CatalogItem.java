package com.esprit.lms.catalog.entity;

import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "catalog_items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType type;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 500)
    private String author;

    @Column(length = 255)
    private String subject;

    @Column(length = 10)
    @Builder.Default
    private String language = "FR";

    private Integer year;

    @Column(name = "cover_image_url", length = 1000)
    private String coverImageUrl;

    /**
     * MinIO private object key — NEVER exposed in any DTO or API response.
     */
    @Column(name = "file_blob_key", length = 500)
    private String fileBlobKey;

    @Column(precision = 8, scale = 3)
    private BigDecimal price;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Type(StringArrayType.class)
    @Column(name = "ai_tags", columnDefinition = "text[]")
    private String[] aiTags;

    // Embedding stored as float[] — pgvector VECTOR(1024)
    // Handled via native queries for vector operations
    @Column(name = "embedding", insertable = false, updatable = false)
    private transient float[] embedding;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_status", nullable = false)
    @Builder.Default
    private AiStatus aiStatus = AiStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Returns true if this item is free (no price or price = 0).
     */
    public boolean isFree() {
        return price == null || price.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Returns true if this item is a digital type (can be read online).
     */
    public boolean isDigital() {
        return type == ItemType.DIGITAL_BOOK || type == ItemType.ARTICLE || type == ItemType.PFE_REPORT;
    }
}
