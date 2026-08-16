package com.fooddelivery.notification.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fooddelivery.notification.entity.UserDeviceToken.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for registering/updating device tokens.
 *
 * The mobile apps send {@code {token, platform, deviceId}}. The older
 * {@code deviceToken}/{@code deviceType} names are accepted as aliases so
 * existing clients keep working.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenRequest {

    /** Raw FCM registration token (Android) or APNs device token (iOS, 64-char hex). */
    @JsonAlias("deviceToken")
    @NotBlank(message = "Device token is required")
    @Size(max = 500, message = "Device token must not exceed 500 characters")
    private String token;

    /** ANDROID | IOS (alias: deviceType). */
    @JsonAlias("deviceType")
    private DeviceType platform;

    /** Stable per-device id; registration upserts on (user, deviceId). */
    @Size(max = 200, message = "Device id must not exceed 200 characters")
    private String deviceId;

    @Size(max = 100, message = "Device name must not exceed 100 characters")
    private String deviceName;

    @Size(max = 20, message = "App version must not exceed 20 characters")
    private String appVersion;

    /** Convenience accessor for the legacy field name. */
    public String getDeviceToken() {
        return token;
    }

    /** Convenience accessor for the legacy field name. */
    public DeviceType getDeviceType() {
        return platform;
    }
}
