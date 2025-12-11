package com.fooddelivery.analytics.financial.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity tracking payments to couriers for deliveries.
 */
@Entity
@Table(name = "courier_payments", indexes = {
        @Index(name = "idx_cp_courier_id", columnList = "courier_id"),
        @Index(name = "idx_cp_order_id", columnList = "order_id"),
        @Index(name = "idx_cp_payment_date", columnList = "payment_date"),
        @Index(name = "idx_cp_status", columnList = "payment_status"),
        @Index(name = "idx_cp_courier_date", columnList = "courier_id, payment_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "courier_id", nullable = false)
    private Long courierId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * Base delivery payment.
     */
    @Column(name = "base_payment", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePayment;

    /**
     * Distance bonus (if applicable).
     */
    @Column(name = "distance_bonus", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal distanceBonus = BigDecimal.ZERO;

    /**
     * Tip amount from customer.
     */
    @Column(name = "tip_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal tipAmount = BigDecimal.ZERO;

    /**
     * Peak hour bonus.
     */
    @Column(name = "peak_bonus", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal peakBonus = BigDecimal.ZERO;

    /**
     * Total payment amount.
     */
    @Column(name = "total_payment", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPayment;

    /**
     * Payment type.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    @Builder.Default
    private CourierPaymentType paymentType = CourierPaymentType.DELIVERY;

    /**
     * Payment status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Calculate total payment.
     */
    public void calculateTotal() {
        this.totalPayment = basePayment
                .add(distanceBonus != null ? distanceBonus : BigDecimal.ZERO)
                .add(tipAmount != null ? tipAmount : BigDecimal.ZERO)
                .add(peakBonus != null ? peakBonus : BigDecimal.ZERO);
    }
}
