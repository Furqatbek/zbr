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

    // Weight-based items
    private Boolean isSoldByWeight;
    private String weightUnit;
    private BigDecimal minWeight;
    private BigDecimal maxWeight;

    // Category info (from endpoint #6)
    private Long categoryId;
    private String categoryName;

    public boolean isAvailable() {
        if (available != null) return available;
        if (inStock != null) return inStock;
        return !"ARCHIVED".equalsIgnoreCase(status);
    }

    public boolean isFeaturedProduct() {
        if (isFeatured != null) return isFeatured;
        if (featured != null) return featured;
        return false;
    }
}
