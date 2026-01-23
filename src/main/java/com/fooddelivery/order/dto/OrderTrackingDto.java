package com.fooddelivery.order.dto;

import com.fooddelivery.order.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for order tracking information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order tracking information")
public class OrderTrackingDto {

    @Schema(description = "Order ID")
    private Long id;

    @Schema(description = "External order number")
    private String externalOrderNo;

    @Schema(description = "Current order status")
    private OrderStatus status;

    // Restaurant info
    @Schema(description = "Restaurant ID")
    private Long restaurantId;

    @Schema(description = "Restaurant name")
    private String restaurantName;

    @Schema(description = "Restaurant address")
    private String restaurantAddress;

    @Schema(description = "Restaurant latitude")
    private BigDecimal restaurantLat;

    @Schema(description = "Restaurant longitude")
    private BigDecimal restaurantLng;

    // Courier info
    @Schema(description = "Courier ID")
    private Long courierId;

    @Schema(description = "Courier name")
    private String courierName;

    @Schema(description = "Courier phone")
    private String courierPhone;

    @Schema(description = "Courier current latitude")
    private BigDecimal courierLat;

    @Schema(description = "Courier current longitude")
    private BigDecimal courierLng;

    // Delivery info
    @Schema(description = "Delivery address")
    private String deliveryAddress;

    @Schema(description = "Delivery latitude")
    private BigDecimal deliveryLat;

    @Schema(description = "Delivery longitude")
    private BigDecimal deliveryLng;

    // Timing
    @Schema(description = "Estimated delivery time")
    private LocalDateTime estimatedDeliveryTime;

    @Schema(description = "Created at")
    private LocalDateTime createdAt;

    @Schema(description = "Accepted at")
    private LocalDateTime acceptedAt;

    @Schema(description = "Ready at")
    private LocalDateTime readyAt;

    @Schema(description = "Picked up at")
    private LocalDateTime pickedUpAt;

    @Schema(description = "Delivered at")
    private LocalDateTime deliveredAt;
}
