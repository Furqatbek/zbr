package com.fooddelivery.analytics.fraud.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing referral events for fraud detection.
 */
@Entity
@Table(name = "referral_events", indexes = {
        @Index(name = "idx_referral_events_referrer_id", columnList = "referrer_id"),
        @Index(name = "idx_referral_events_referred_user_id", columnList = "referred_user_id"),
        @Index(name = "idx_referral_events_device_id", columnList = "device_id"),
        @Index(name = "idx_referral_events_ip_address", columnList = "ip_address"),
        @Index(name = "idx_referral_events_created_at", columnList = "created_at"),
        @Index(name = "idx_referral_events_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "referrer_id", nullable = false)
    private Long referrerId;

    @Column(name = "referred_user_id", nullable = false)
    private Long referredUserId;

    @Column(name = "referral_code", length = 50)
    private String referralCode;

    @Column(name = "device_id", length = 255)
    private String deviceId;

    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReferralStatus status;

    @Column(name = "referrer_bonus")
    private BigDecimal referrerBonus;

    @Column(name = "referred_bonus")
    private BigDecimal referredBonus;

    @Column(name = "phone_number_hash", length = 64)
    private String phoneNumberHash;

    @Column(name = "email_domain", length = 255)
    private String emailDomain;

    @Column(name = "first_order_completed")
    private Boolean firstOrderCompleted;

    @Column(name = "first_order_id")
    private Long firstOrderId;

    @Column(name = "is_suspicious")
    private Boolean isSuspicious;

    @Column(name = "fraud_flags", length = 500)
    private String fraudFlags;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ReferralStatus.PENDING;
        }
    }

    public enum ReferralStatus {
        PENDING, COMPLETED, EXPIRED, CANCELLED, FRAUD_DETECTED
    }
}
