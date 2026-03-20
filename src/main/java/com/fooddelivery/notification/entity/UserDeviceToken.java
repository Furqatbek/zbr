package com.fooddelivery.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for storing user device tokens for push notifications.
 * Each user can have multiple devices (phone, tablet, etc.)
 */
@Entity
@Table(name = "user_device_tokens", indexes = {
        @Index(name = "idx_device_tokens_user", columnList = "user_id"),
        @Index(name = "idx_device_tokens_token", columnList = "device_token")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_token", nullable = false, length = 500)
    private String deviceToken;

    @Column(name = "device_type", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DeviceType deviceType = DeviceType.UNKNOWN;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "app_version", length = 20)
    private String appVersion;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum DeviceType {
        ANDROID,
        IOS,
        WEB,
        UNKNOWN
    }
}
