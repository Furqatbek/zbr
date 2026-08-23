package com.fooddelivery.platform.entity;

import com.fooddelivery.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Referral entity for tracking referral program.
 */
@Entity
@Table(name = "referrals", indexes = {
        @Index(name = "idx_referrals_code", columnList = "code"),
        @Index(name = "idx_referrals_referrer", columnList = "referrer_user_id"),
        @Index(name = "idx_referrals_referred", columnList = "referred_user_id"),
        @Index(name = "idx_referrals_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Referral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_user_id", nullable = false)
    private User referrer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_user_id")
    private User referred;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReferralStatus status = ReferralStatus.PENDING;

    @Column(name = "reward_amount", precision = 10, scale = 2)
    private BigDecimal rewardAmount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "UZS";

    @Column(name = "referrer_rewarded")
    @Builder.Default
    private Boolean referrerRewarded = false;

    @Column(name = "referred_rewarded")
    @Builder.Default
    private Boolean referredRewarded = false;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return status == ReferralStatus.PENDING && !isExpired();
    }

    public void complete() {
        this.status = ReferralStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}
