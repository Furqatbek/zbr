package com.fooddelivery.analytics.financial.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity tracking payouts to restaurants.
 */
@Entity
@Table(name = "restaurant_payouts", indexes = {
        @Index(name = "idx_rp_restaurant_id", columnList = "restaurant_id"),
        @Index(name = "idx_rp_payout_date", columnList = "payout_date"),
        @Index(name = "idx_rp_status", columnList = "status"),
        @Index(name = "idx_rp_period", columnList = "period_start, period_end"),
        @Index(name = "idx_rp_restaurant_date", columnList = "restaurant_id, payout_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    /**
     * Payout period start date.
     */
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    /**
     * Payout period end date.
     */
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    /**
     * Total GMV for the period.
     */
    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount;

    /**
     * Total commission deducted.
     */
    @Column(name = "commission_deducted", nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionDeducted;

    /**
     * Delivery fee subsidies deducted (if restaurant sponsors).
     */
    @Column(name = "delivery_subsidies", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal deliverySubsidies = BigDecimal.ZERO;

    /**
     * Promotion costs charged to restaurant.
     */
    @Column(name = "promotion_costs", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal promotionCosts = BigDecimal.ZERO;

    /**
     * Adjustments (refunds, disputes, etc.).
     */
    @Column(name = "adjustments", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal adjustments = BigDecimal.ZERO;

    /**
     * Fees (payment processing, etc.).
     */
    @Column(name = "fees", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal fees = BigDecimal.ZERO;

    /**
     * Net payout amount.
     */
    @Column(name = "net_payout", nullable = false, precision = 12, scale = 2)
    private BigDecimal netPayout;

    /**
     * Number of orders in this payout.
     */
    @Column(name = "order_count", nullable = false)
    private Integer orderCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "payout_date")
    private LocalDateTime payoutDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Calculate net payout.
     */
    public void calculateNetPayout() {
        this.netPayout = grossAmount
                .subtract(commissionDeducted)
                .subtract(deliverySubsidies != null ? deliverySubsidies : BigDecimal.ZERO)
                .subtract(promotionCosts != null ? promotionCosts : BigDecimal.ZERO)
                .add(adjustments != null ? adjustments : BigDecimal.ZERO)
                .subtract(fees != null ? fees : BigDecimal.ZERO);
    }
}
