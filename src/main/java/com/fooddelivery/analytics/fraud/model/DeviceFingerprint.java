package com.fooddelivery.analytics.fraud.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing device fingerprints for fraud detection.
 */
@Entity
@Table(name = "device_fingerprints", indexes = {
        @Index(name = "idx_device_fingerprint_user_id", columnList = "user_id"),
        @Index(name = "idx_device_fingerprint_device_id", columnList = "device_id"),
        @Index(name = "idx_device_fingerprint_fingerprint", columnList = "fingerprint_hash"),
        @Index(name = "idx_device_fingerprint_first_seen", columnList = "first_seen_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceFingerprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_id", nullable = false, length = 255)
    private String deviceId;

    @Column(name = "fingerprint_hash", length = 64)
    private String fingerprintHash;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "device_brand", length = 100)
    private String deviceBrand;

    @Column(name = "device_model", length = 100)
    private String deviceModel;

    @Column(name = "os_name", length = 50)
    private String osName;

    @Column(name = "os_version", length = 50)
    private String osVersion;

    @Column(name = "app_version", length = 50)
    private String appVersion;

    @Column(name = "screen_resolution", length = 20)
    private String screenResolution;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "is_emulator")
    private Boolean isEmulator;

    @Column(name = "is_rooted")
    private Boolean isRooted;

    @Column(name = "is_trusted")
    private Boolean isTrusted;

    @Column(name = "trust_score")
    private Integer trustScore;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "times_seen")
    private Integer timesSeen;

    @PrePersist
    protected void onCreate() {
        if (firstSeenAt == null) {
            firstSeenAt = LocalDateTime.now();
        }
        lastSeenAt = LocalDateTime.now();
        if (timesSeen == null) {
            timesSeen = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastSeenAt = LocalDateTime.now();
        if (timesSeen != null) {
            timesSeen++;
        }
    }

    public boolean isSuspiciousDevice() {
        return Boolean.TRUE.equals(isEmulator) || Boolean.TRUE.equals(isRooted) ||
                (trustScore != null && trustScore < 50);
    }
}
