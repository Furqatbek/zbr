package com.fooddelivery.analytics.fraud.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing order history for fraud analysis.
 * This is a fraud-specific view of order data with additional fraud-relevant fields.
 */
@Entity
@Table(name = "fraud_order_history", indexes = {
        @Index(name = "idx_fraud_order_user_id", columnList = "user_id"),
        @Index(name = "idx_fraud_order_device_id", columnList = "device_id"),
        @Index(name = "idx_fraud_order_created_at", columnList = "created_at"),
        @Index(name = "idx_fraud_order_address_hash", columnList = "delivery_address_hash"),
        @Index(name = "idx_fraud_order_status", columnList = "status"),
        @Index(name = "idx_fraud_order_user_created", columnList = "user_id, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudOrderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "restaurant_id")
    private Long restaurantId;

    @Column(name = "courier_id")
    private Long courierId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "user_paid_amount")
    private BigDecimal userPaidAmount;

    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;

    @Column(name = "delivery_address_hash", length = 64)
    private String deliveryAddressHash;

    @Column(name = "delivery_lat")
    private Double deliveryLat;

    @Column(name = "delivery_lng")
    private Double deliveryLng;

    @Column(name = "device_id", length = 255)
    private String deviceId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "is_refunded")
    private Boolean isRefunded;

    @Column(name = "refund_amount")
    private BigDecimal refundAmount;

    @Column(name = "refund_reason", length = 255)
    private String refundReason;

    @Column(name = "is_promo_order")
    private Boolean isPromoOrder;

    @Column(name = "promo_code", length = 50)
    private String promoCode;

    @Column(name = "user_account_age_hours")
    private Integer userAccountAgeHours;

    @Column(name = "is_first_order")
    private Boolean isFirstOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum OrderStatus {
        PENDING, CONFIRMED, PREPARING, READY, PICKED_UP, DELIVERED, CANCELLED, REFUNDED
    }

    public boolean isNewAccountOrder() {
        return userAccountAgeHours != null && userAccountAgeHours < 24;
    }

    public boolean isFullyDiscounted() {
        return userPaidAmount != null && userPaidAmount.compareTo(BigDecimal.ZERO) == 0;
    }
}
