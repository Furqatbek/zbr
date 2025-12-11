package com.fooddelivery.analytics.fraud.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing authentication logs for security monitoring.
 */
@Entity
@Table(name = "auth_logs", indexes = {
        @Index(name = "idx_auth_logs_user_id", columnList = "user_id"),
        @Index(name = "idx_auth_logs_ip_address", columnList = "ip_address"),
        @Index(name = "idx_auth_logs_status", columnList = "status"),
        @Index(name = "idx_auth_logs_created_at", columnList = "created_at"),
        @Index(name = "idx_auth_logs_user_status_created", columnList = "user_id, status, created_at"),
        @Index(name = "idx_auth_logs_ip_status_created", columnList = "ip_address, status, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuthStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", length = 30)
    private AuthType authType;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "device_id", length = 255)
    private String deviceId;

    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    @Column(name = "geo_country", length = 2)
    private String geoCountry;

    @Column(name = "geo_city", length = 100)
    private String geoCity;

    @Column(name = "is_vpn")
    private Boolean isVpn;

    @Column(name = "is_tor")
    private Boolean isTor;

    @Column(name = "is_proxy")
    private Boolean isProxy;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum AuthStatus {
        SUCCESS, FAILED, LOCKED, RATE_LIMITED, TOKEN_EXPIRED, TOKEN_INVALID,
        MFA_REQUIRED, MFA_FAILED, SESSION_EXPIRED, SUSPICIOUS
    }

    public enum AuthType {
        LOGIN, LOGOUT, TOKEN_REFRESH, PASSWORD_RESET, MFA_VERIFY, REGISTRATION
    }

    public boolean isFailed() {
        return status == AuthStatus.FAILED || status == AuthStatus.LOCKED ||
                status == AuthStatus.TOKEN_INVALID || status == AuthStatus.MFA_FAILED;
    }

    public boolean isSuspiciousNetwork() {
        return Boolean.TRUE.equals(isVpn) || Boolean.TRUE.equals(isTor) || Boolean.TRUE.equals(isProxy);
    }
}
