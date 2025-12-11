package com.fooddelivery.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for item option information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Item option information (add-ons, etc.)")
public class ItemOptionDto {

    @Schema(description = "Option ID")
    private Long id;

    @Schema(description = "Menu item ID")
    private Long menuItemId;

    @Schema(description = "Option group name", example = "Toppings")
    private String groupName;

    @Schema(description = "Option name", example = "Extra Cheese")
    private String name;

    @Schema(description = "Price difference", example = "1.50")
    private BigDecimal priceDelta;

    @Schema(description = "Is default selection")
    private Boolean isDefault;

    @Schema(description = "Maximum selections allowed")
    private Integer maxSelections;

    @Schema(description = "Is required")
    private Boolean required;

    @Schema(description = "Is in stock")
    private Boolean inStock;

    @Schema(description = "Sort order")
    private Integer sortOrder;

    @Schema(description = "Is active")
    private Boolean active;
}
