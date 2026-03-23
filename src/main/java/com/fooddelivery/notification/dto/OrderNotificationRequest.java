package com.fooddelivery.notification.dto;

import com.fooddelivery.notification.model.NotificationType;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request DTO for creating order-related notifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderNotificationRequest {

    /**
     * Order ID.
     */
    private Long orderId;

    /**
     * Order number/reference.
     */
    private String orderNumber;

    /**
     * Customer user ID.
     */
    private Long customerId;

    /**
     * Customer name.
     */
    private String customerName;

    /**
     * Restaurant ID.
     */
    private Long restaurantId;

    /**
     * Restaurant name.
     */
    private String restaurantName;

    /**
     * Restaurant user ID (owner/manager).
     */
    private Long restaurantUserId;

    /**
     * Restaurant owner's phone number.
     */
    private String restaurantOwnerPhone;

    /**
     * Restaurant owner's email.
     */
    private String restaurantOwnerEmail;

    /**
     * Restaurant's phone number.
     */
    private String restaurantPhone;

    /**
     * Restaurant's email.
     */
    private String restaurantEmail;

    /**
     * Courier ID.
     */
    private Long courierId;

    /**
     * Courier name.
     */
    private String courierName;

    /**
     * Courier user ID.
     */
    private Long courierUserId;

    /**
     * Order total amount.
     */
    private BigDecimal orderTotal;

    /**
     * Total amount (alias for orderTotal).
     */
    private BigDecimal totalAmount;

    /**
     * Event type that triggered this notification.
     */
    private NotificationType eventType;

    /**
     * Estimated delivery time (in minutes).
     */
    private Integer estimatedDeliveryMinutes;

    /**
     * Cancellation reason (if applicable).
     */
    private String cancellationReason;

    /**
     * Who cancelled (if applicable).
     */
    private String cancelledBy;

    /**
     * Rejection reason (if applicable).
     */
    private String rejectionReason;

    /**
     * Additional context metadata.
     */
    private Map<String, Object> additionalData;

    /**
     * Metadata for notification processing.
     */
    private Map<String, Object> metadata;
}
