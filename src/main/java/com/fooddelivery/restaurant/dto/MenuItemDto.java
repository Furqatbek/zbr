package com.fooddelivery.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for menu item information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Menu item information")
public class MenuItemDto {

    @Schema(description = "Item ID")
    private Long id;

    @Schema(description = "Category ID")
    private Long categoryId;

    @Schema(description = "Category name")
    private String categoryName;

    @Schema(description = "Item name")
    private String name;

    @Schema(description = "Item description")
    private String description;

    @Schema(description = "Base price")
    private BigDecimal price;

    @Schema(description = "Price with platform margin")
    private BigDecimal priceWithMargin;

    @Schema(description = "Original price (if on sale)")
    private BigDecimal originalPrice;

    @Schema(description = "Effective price (used for display)")
    private BigDecimal effectivePrice;

    @Schema(description = "Is on sale")
    private Boolean onSale;

    @Schema(description = "Discount percentage")
    private Integer discountPercentage;

    @Schema(description = "Image URL")
    private String imageUrl;

    @Schema(description = "Image file path on server")
    private String imagePath;

    @Schema(description = "Original image filename")
    private String imageName;

    @Schema(description = "Image file size in bytes")
    private Long imageSize;

    @Schema(description = "Image content type (MIME type)")
    private String imageContentType;

    @Schema(description = "Is in stock")
    private Boolean inStock;

    @Schema(description = "Is featured")
    private Boolean featured;

    @Schema(description = "Sort order")
    private Integer sortOrder;

    @Schema(description = "Preparation time in minutes")
    private Integer prepTimeMinutes;

    @Schema(description = "Calories")
    private Integer calories;

    @Schema(description = "Is vegetarian")
    private Boolean vegetarian;

    @Schema(description = "Is vegan")
    private Boolean vegan;

    @Schema(description = "Is gluten-free")
    private Boolean glutenFree;

    @Schema(description = "Is spicy")
    private Boolean spicy;

    @Schema(description = "Allergen information")
    private String allergens;

    @Schema(description = "Is active")
    private Boolean active;

    @Schema(description = "Available variants (sizes, etc.)")
    private List<ItemVariantDto> variants;

    @Schema(description = "Available options (add-ons, etc.)")
    private List<ItemOptionDto> options;
}
