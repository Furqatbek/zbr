package com.fooddelivery.order.dto;

import com.fooddelivery.order.entity.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating an order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create order request")
public class CreateOrderRequest {

    @Schema(description = "Restaurant ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Restaurant ID is required")
    private Long restaurantId;

    @Schema(description = "Order type", example = "DELIVERY", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Order type is required")
    private OrderType orderType;

    @Schema(description = "Order items", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderItemRequest> items;

    // Delivery details
    @Schema(description = "Delivery address (required for DELIVERY orders)")
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String deliveryAddress;

    @Schema(description = "Delivery latitude")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal deliveryLatitude;

    @Schema(description = "Delivery longitude")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal deliveryLongitude;

    @Schema(description = "Delivery instructions")
    @Size(max = 500, message = "Instructions must not exceed 500 characters")
    private String deliveryInstructions;

    // For dine-in orders
    @Schema(description = "Table ID (for DINE_IN orders)")
    private String tableId;

    // Contact info
    @Schema(description = "Customer name")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String customerName;

    @Schema(description = "Customer phone")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String customerPhone;

    // Optional
    @Schema(description = "Order notes/special requests")
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    @Schema(description = "Tip amount")
    @DecimalMin(value = "0.0", message = "Tip must be non-negative")
    private BigDecimal tipAmount;

    @Schema(description = "Discount code to apply")
    private String discountCode;
}
