package com.esprit.lms.catalog.dto;

import com.esprit.lms.catalog.entity.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request DTO for creating/updating catalog items.
 * File and cover are sent as separate multipart parts.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CatalogCreateRequest {

    @NotNull(message = "Item type is required")
    private ItemType type;

    @NotBlank(message = "Title is required")
    private String title;

    private String author;
    private String subject;
    private String language;
    private Integer year;
    private BigDecimal price;
}
