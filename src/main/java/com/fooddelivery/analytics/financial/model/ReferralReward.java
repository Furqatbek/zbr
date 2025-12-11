package com.fooddelivery.analytics.financial.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity tracking referral rewards issued.
 */
@Entity
@Table(name = "referral_rewards", indexes = {
        @Index(name = "idx_rr_referrer_id", columnList = "referrer_id"),
        @Index(name = "idx_rr_referred_id", columnList = "referred_user_id"),
        @Index(name = "idx_rr_awarded_at", columnList = "awarded_at"),
        @Index(name = "idx_rr_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralReward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "referral_id", nullable = false)
    private Long referralId;

    @Column(name = "referrer_id", nullable = false)
    private Long referrerId;

    @Column(name = "referred_user_id", nullable = false)
    private Long referredUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 20)
    private ReferralRewardType rewardType;

    /**
     * Reward amount.
     */
    @Column(name = "reward_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal rewardAmount;

    /**
     * Related order ID (triggering order).
     */
    @Column(name = "triggering_order_id")
    private Long triggeringOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "awarded_at", nullable = false)
    private LocalDateTime awardedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
