package com.esprit.lms.catalog.dto;

import com.esprit.lms.catalog.entity.AiStatus;
import com.esprit.lms.catalog.entity.ItemType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Full detail DTO for catalog item page.
 * Includes ai_summary and tags, but NEVER file_blob_key.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CatalogDetailDTO {
    private UUID id;
    private ItemType type;
    private String title;
    private String author;
    private String subject;
    private String language;
    private Integer year;
    private String coverImageUrl;
    private BigDecimal price;
    private boolean free;
    private boolean digital;
    private AiStatus aiStatus;
    private String aiSummary;
    private String[] aiTags;
    private boolean hasAccess;       // true if user owns it or it's free
    private int availableCopies;     // for physical books
    private LocalDateTime createdAt;
}
