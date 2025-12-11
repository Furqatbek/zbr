package com.fooddelivery.order.dto;

import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.entity.OrderType;
import com.fooddelivery.order.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for order information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order information")
public class OrderDto {

    @Schema(description = "Order ID")
    private Long id;

    @Schema(description = "External order number")
    private String externalOrderNo;

    @Schema(description = "Consumer ID")
    private Long consumerId;

    @Schema(description = "Consumer name")
    private String consumerName;

    @Schema(description = "Restaurant ID")
    private Long restaurantId;

    @Schema(description = "Restaurant name")
    private String restaurantName;

    @Schema(description = "Courier ID")
    private Long courierId;

    @Schema(description = "Courier name")
    private String courierName;

    @Schema(description = "Order type")
    private OrderType orderType;

    @Schema(description = "Order status")
    private OrderStatus status;

    @Schema(description = "Payment status")
    private PaymentStatus paymentStatus;

    @Schema(description = "Order items")
    private List<OrderItemDto> items;

    // Pricing
    @Schema(description = "Subtotal (items total)")
    private BigDecimal subtotal;

    @Schema(description = "Tax amount")
    private BigDecimal tax;

    @Schema(description = "Delivery fee")
    private BigDecimal deliveryFee;

    @Schema(description = "Discount amount")
    private BigDecimal discount;

    @Schema(description = "Tip amount")
    private BigDecimal tipAmount;

    @Schema(description = "Total amount")
    private BigDecimal total;

    // Delivery details
    @Schema(description = "Delivery address")
    private String deliveryAddress;

    @Schema(description = "Delivery instructions")
    private String deliveryInstructions;

    @Schema(description = "Table ID (for dine-in)")
    private String tableId;

    // Contact
    @Schema(description = "Customer name")
    private String customerName;

    @Schema(description = "Customer phone")
    private String customerPhone;

    @Schema(description = "Order notes")
    private String notes;

    // Timing
    @Schema(description = "Estimated preparation time (minutes)")
    private Integer estimatedPrepTimeMinutes;

    @Schema(description = "Estimated delivery time")
    private LocalDateTime estimatedDeliveryTime;

    @Schema(description = "Created at")
    private LocalDateTime createdAt;

    @Schema(description = "Accepted at")
    private LocalDateTime acceptedAt;

    @Schema(description = "Ready at")
    private LocalDateTime readyAt;

    @Schema(description = "Delivered at")
    private LocalDateTime deliveredAt;

    @Schema(description = "Cancellation reason")
    private String cancellationReason;

    // Rating
    @Schema(description = "Consumer rating (1-5)")
    private Integer consumerRating;

    @Schema(description = "Consumer review")
    private String consumerReview;
}
