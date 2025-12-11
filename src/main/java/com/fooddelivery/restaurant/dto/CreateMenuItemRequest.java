package com.fooddelivery.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating a menu item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create menu item request")
public class CreateMenuItemRequest {

    @Schema(description = "Category ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @Schema(description = "Item name", example = "Margherita Pizza", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Item name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @Schema(description = "Item description", example = "Classic pizza with tomato sauce and mozzarella")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Schema(description = "Price", example = "12.99", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @Schema(description = "Original price (for showing discounts)", example = "14.99")
    @DecimalMin(value = "0.01", message = "Original price must be greater than 0")
    private BigDecimal originalPrice;

    @Schema(description = "Image URL")
    private String imageUrl;

    @Schema(description = "Preparation time in minutes", example = "20")
    @Min(value = 1, message = "Prep time must be at least 1 minute")
    @Max(value = 180, message = "Prep time must not exceed 180 minutes")
    private Integer prepTimeMinutes;

    @Schema(description = "Calories", example = "850")
    @Min(value = 0, message = "Calories must be non-negative")
    private Integer calories;

    @Schema(description = "Is vegetarian", example = "true")
    @Builder.Default
    private Boolean vegetarian = false;

    @Schema(description = "Is vegan", example = "false")
    @Builder.Default
    private Boolean vegan = false;

    @Schema(description = "Is gluten-free", example = "false")
    @Builder.Default
    private Boolean glutenFree = false;

    @Schema(description = "Is spicy", example = "false")
    @Builder.Default
    private Boolean spicy = false;

    @Schema(description = "Allergen information", example = "Contains dairy, gluten")
    @Size(max = 500, message = "Allergens must not exceed 500 characters")
    private String allergens;

    @Schema(description = "Is featured item", example = "false")
    @Builder.Default
    private Boolean featured = false;

    @Schema(description = "Sort order")
    private Integer sortOrder;

    @Schema(description = "Variants (sizes, etc.)")
    private List<CreateItemVariantRequest> variants;

    @Schema(description = "Options (add-ons, etc.)")
    private List<CreateItemOptionRequest> options;
}
