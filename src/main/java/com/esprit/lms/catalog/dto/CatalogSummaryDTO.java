package com.esprit.lms.catalog.dto;

import com.esprit.lms.catalog.entity.AiStatus;
import com.esprit.lms.catalog.entity.ItemType;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Summary DTO for catalog list/search results.
 * Lightweight — no file_blob_key, no embedding.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CatalogSummaryDTO {
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
    private AiStatus aiStatus;
    private String[] aiTags;
}
