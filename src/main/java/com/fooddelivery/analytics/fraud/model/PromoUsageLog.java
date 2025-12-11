package com.fooddelivery.analytics.fraud.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing promo code usage for abuse detection.
 */
@Entity
@Table(name = "promo_usage_logs", indexes = {
        @Index(name = "idx_promo_usage_user_id", columnList = "user_id"),
        @Index(name = "idx_promo_usage_promo_code", columnList = "promo_code"),
        @Index(name = "idx_promo_usage_order_id", columnList = "order_id"),
        @Index(name = "idx_promo_usage_created_at", columnList = "created_at"),
        @Index(name = "idx_promo_usage_device_id", columnList = "device_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromoUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "promo_code", nullable = false, length = 50)
    private String promoCode;

    @Column(name = "order_id")
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "promo_type", length = 30)
    private PromoType promoType;

    @Column(name = "discount_value", nullable = false)
    private BigDecimal discountValue;

    @Column(name = "order_total")
    private BigDecimal orderTotal;

    @Column(name = "user_paid_amount")
    private BigDecimal userPaidAmount;

    @Column(name = "device_id", length = 255)
    private String deviceId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "is_first_order")
    private Boolean isFirstOrder;

    @Column(name = "is_referral_promo")
    private Boolean isReferralPromo;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UsageStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = UsageStatus.APPLIED;
        }
    }

    public enum PromoType {
        PERCENTAGE, FIXED_AMOUNT, FREE_DELIVERY, REFERRAL, FIRST_ORDER, CASHBACK
    }

    public enum UsageStatus {
        APPLIED, COMPLETED, CANCELLED, REFUNDED
    }

    /**
     * Check if order was paid entirely with promo credit.
     */
    public boolean isFullyPromoPaid() {
        return userPaidAmount != null && userPaidAmount.compareTo(BigDecimal.ZERO) == 0;
    }
}
