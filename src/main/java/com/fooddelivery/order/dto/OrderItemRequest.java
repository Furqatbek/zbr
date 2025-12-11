package com.fooddelivery.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for an order item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order item request")
public class OrderItemRequest {

    @Schema(description = "Menu item ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Menu item ID is required")
    private Long menuItemId;

    @Schema(description = "Quantity", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Builder.Default
    private Integer quantity = 1;

    @Schema(description = "Selected variant ID (e.g., size)")
    private Long variantId;

    @Schema(description = "Selected option IDs (add-ons)")
    private List<Long> optionIds;

    @Schema(description = "Special instructions for this item")
    @Size(max = 500, message = "Instructions must not exceed 500 characters")
    private String specialInstructions;
}
