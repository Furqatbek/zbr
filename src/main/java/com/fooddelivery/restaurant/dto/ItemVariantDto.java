package com.fooddelivery.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for item variant information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Item variant information (sizes, etc.)")
public class ItemVariantDto {

    @Schema(description = "Variant ID")
    private Long id;

    @Schema(description = "Menu item ID")
    private Long menuItemId;

    @Schema(description = "Variant name", example = "Large")
    private String name;

    @Schema(description = "Price difference from base", example = "3.00")
    private BigDecimal priceDelta;

    @Schema(description = "Total price including base", example = "15.99")
    private BigDecimal totalPrice;

    @Schema(description = "Is in stock")
    private Boolean inStock;

    @Schema(description = "Sort order")
    private Integer sortOrder;

    @Schema(description = "Is active")
    private Boolean active;
}
