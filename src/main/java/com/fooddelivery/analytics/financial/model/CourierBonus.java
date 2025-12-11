package com.fooddelivery.analytics.financial.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity tracking bonuses and incentives paid to couriers.
 */
@Entity
@Table(name = "courier_bonuses", indexes = {
        @Index(name = "idx_cb_courier_id", columnList = "courier_id"),
        @Index(name = "idx_cb_bonus_date", columnList = "bonus_date"),
        @Index(name = "idx_cb_bonus_type", columnList = "bonus_type"),
        @Index(name = "idx_cb_courier_date", columnList = "courier_id, bonus_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierBonus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "courier_id", nullable = false)
    private Long courierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "bonus_type", nullable = false, length = 30)
    private BonusType bonusType;

    @Column(name = "bonus_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal bonusAmount;

    @Column(name = "description", length = 500)
    private String description;

    /**
     * Related campaign or promotion ID (if applicable).
     */
    @Column(name = "campaign_id")
    private Long campaignId;

    /**
     * Target achieved (e.g., "50 deliveries").
     */
    @Column(name = "target_achieved", length = 200)
    private String targetAchieved;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "bonus_date", nullable = false)
    private LocalDateTime bonusDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
