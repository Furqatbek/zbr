package com.fooddelivery.analytics.financial.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity tracking commission earned from each order.
 * Commission is calculated based on restaurant's commission rate.
 */
@Entity
@Table(name = "restaurant_commissions", indexes = {
        @Index(name = "idx_rc_restaurant_id", columnList = "restaurant_id"),
        @Index(name = "idx_rc_order_id", columnList = "order_id"),
        @Index(name = "idx_rc_earned_at", columnList = "earned_at"),
        @Index(name = "idx_rc_restaurant_earned", columnList = "restaurant_id, earned_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantCommission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * Order subtotal (base for commission calculation).
     */
    @Column(name = "order_subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal orderSubtotal;

    /**
     * Commission rate applied (as percentage, e.g., 15.00 for 15%).
     */
    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;

    /**
     * Fixed commission amount (if applicable).
     */
    @Column(name = "fixed_commission", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal fixedCommission = BigDecimal.ZERO;

    /**
     * Total commission earned from this order.
     */
    @Column(name = "commission_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionAmount;

    /**
     * Commission type: PERCENTAGE, FIXED, HYBRID (fixed + percentage).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "commission_type", nullable = false, length = 20)
    @Builder.Default
    private CommissionType commissionType = CommissionType.PERCENTAGE;

    @Column(name = "earned_at", nullable = false)
    private LocalDateTime earnedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Calculate commission based on type.
     */
    public void calculateCommission() {
        if (commissionType == CommissionType.FIXED) {
            this.commissionAmount = fixedCommission;
        } else if (commissionType == CommissionType.PERCENTAGE) {
            this.commissionAmount = orderSubtotal.multiply(commissionRate)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        } else {
            // HYBRID: fixed + percentage
            BigDecimal percentageCommission = orderSubtotal.multiply(commissionRate)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            this.commissionAmount = fixedCommission.add(percentageCommission);
        }
    }
}
