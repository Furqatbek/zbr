package com.fooddelivery.analytics.fraud.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing security flags raised against users.
 */
@Entity
@Table(name = "security_flags", indexes = {
        @Index(name = "idx_security_flag_user_id", columnList = "user_id"),
        @Index(name = "idx_security_flag_type", columnList = "flag_type"),
        @Index(name = "idx_security_flag_severity", columnList = "severity"),
        @Index(name = "idx_security_flag_status", columnList = "status"),
        @Index(name = "idx_security_flag_created_at", columnList = "created_at"),
        @Index(name = "idx_security_flag_user_type", columnList = "user_id, flag_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "flag_type", nullable = false, length = 50)
    private FlagType flagType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FlagStatus status;

    @Column(length = 500)
    private String description;

    @Column(length = 1000)
    private String evidence;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "device_id", length = 255)
    private String deviceId;

    @Column(name = "auto_detected")
    private Boolean autoDetected;

    @Column(name = "detection_rule", length = 100)
    private String detectionRule;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", length = 500)
    private String reviewNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = FlagStatus.ACTIVE;
        }
        if (autoDetected == null) {
            autoDetected = true;
        }
    }

    public enum FlagType {
        // Payment fraud
        FAILED_PAYMENT_SPIKE,
        PAYMENT_METHOD_ABUSE,
        CHARGEBACK_HISTORY,

        // Account fraud
        FAKE_ACCOUNT,
        MULTI_ACCOUNT,
        ACCOUNT_TAKEOVER,
        IDENTITY_FRAUD,

        // Behavioral fraud
        ABNORMAL_ORDER_PATTERN,
        VELOCITY_VIOLATION,
        ADDRESS_FRAUD,
        REFUND_ABUSE,

        // Referral fraud
        REFERRAL_ABUSE,
        PROMO_ABUSE,
        CIRCULAR_REFERRAL,

        // Security
        BRUTE_FORCE_ATTACK,
        SUSPICIOUS_IP,
        DEVICE_MISMATCH,
        VPN_TOR_USAGE,

        // Other
        MANUAL_FLAG,
        WATCHLIST_MATCH
    }

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum FlagStatus {
        ACTIVE, UNDER_REVIEW, RESOLVED, FALSE_POSITIVE, CONFIRMED
    }

    public boolean isActive() {
        return status == FlagStatus.ACTIVE || status == FlagStatus.UNDER_REVIEW;
    }

    public boolean isHighRisk() {
        return severity == Severity.HIGH || severity == Severity.CRITICAL;
    }
}
