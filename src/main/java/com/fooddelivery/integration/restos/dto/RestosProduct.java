package com.fooddelivery.integration.restos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Product DTO from Restos API.
 * Used by endpoints #2, #3, #4, #6.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestosProduct {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private BigDecimal priceWithMargin;
    private BigDecimal costPrice;
    private BigDecimal marginPercentage;
    private String itemType;
    private String status;
    private Integer sortOrder;
    private Boolean inStock;
    private Boolean featured;
    private Boolean isFeatured;
    private Boolean available;
    private Boolean hasVariants;

    /**
     * The sizes this product is sold in. Previously not read at all, which is
     * why every variant-bearing dish arrived here flattened to its base price
     * with no size for a customer to choose.
     */
    @lombok.Builder.Default
    private java.util.List<RestosVariant> variants = new java.util.ArrayList<>();

    // Weight-based items
    private Boolean isSoldByWeight;
    private String weightUnit;
    private BigDecimal minWeight;
    private BigDecimal maxWeight;

    // Category info (from endpoint #6)
    private Long categoryId;
    private String categoryName;

    /**
     * Whether this product is published upstream — on sale to anyone at all.
     *
     * <p>Restos's current build only ever exposes LIVE products on the partner
     * menu, so this never had to be asked. An older deployment's public menu
     * serves DRAFT alongside LIVE, and importing those puts dishes a venue has
     * not finished writing on sale here. A DRAFT is not "sold out" and not
     * "retired"; it is not a product yet.
     *
     * <p>A missing status means published: a partner that does not track one at
     * all must not have its entire menu treated as unfinished.
     */
    public boolean isPublished() {
        if (status == null || status.isBlank()) return true;
        return !("ARCHIVED".equalsIgnoreCase(status) || "DRAFT".equalsIgnoreCase(status));
    }

    public boolean isAvailable() {
        if (available != null) return available;
        if (inStock != null) return inStock;
        return isPublished();
    }

    public boolean isFeaturedProduct() {
        if (isFeatured != null) return isFeatured;
        if (featured != null) return featured;
        return false;
    }
}
