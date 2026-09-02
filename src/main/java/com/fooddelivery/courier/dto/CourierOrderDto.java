package com.fooddelivery.courier.dto;

import com.fooddelivery.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for order information displayed to couriers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierOrderDto {
    private Long orderId;
    private String externalOrderNo;

    // Restaurant info
    private Long restaurantId;
    private String restaurantName;
    private String restaurantAddress;
    /**
     * The restaurant's own number, not the customer's. A courier who arrives
     * before the food is ready needs to reach the kitchen, and the pickup call
     * refuses with "Order is not ready for pickup yet" until then — without
     * this field the app could only offer the customer's number, which is no
     * help at all.
     */
    private String restaurantPhone;
    private BigDecimal restaurantLat;
    private BigDecimal restaurantLng;

    // Delivery info
    private String deliveryAddress;
    private BigDecimal deliveryLat;
    private BigDecimal deliveryLng;
    private String customerName;
    private String customerPhone;
    private String deliveryInstructions;

    // Order details
    private OrderStatus status;
    private BigDecimal deliveryFee;
    private BigDecimal tipAmount;
    private BigDecimal total;
    private Integer itemCount;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime readyAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
}
