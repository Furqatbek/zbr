package com.fooddelivery.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for order item information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order item information")
public class OrderItemDto {

    @Schema(description = "Order item ID")
    private Long id;

    @Schema(description = "Menu item ID")
    private Long menuItemId;

    @Schema(description = "Item name")
    private String itemName;

    @Schema(description = "Quantity")
    private Integer quantity;

    @Schema(description = "Unit price")
    private BigDecimal unitPrice;

    @Schema(description = "Total price")
    private BigDecimal totalPrice;

    @Schema(description = "Variant name")
    private String variantName;

    @Schema(description = "Variant price delta")
    private BigDecimal variantPriceDelta;

    @Schema(description = "Selected modifiers")
    private List<ModifierDto> modifiers;

    @Schema(description = "Modifiers total")
    private BigDecimal modifiersTotal;

    @Schema(description = "Special instructions")
    private String specialInstructions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Modifier information")
    public static class ModifierDto {
        private Long id;
        private String name;
        private BigDecimal price;
    }
}
