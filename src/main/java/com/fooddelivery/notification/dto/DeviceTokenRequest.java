package com.fooddelivery.notification.dto;

import com.fooddelivery.notification.entity.UserDeviceToken.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for registering/updating device tokens.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenRequest {

    @NotBlank(message = "Device token is required")
    @Size(max = 500, message = "Device token must not exceed 500 characters")
    private String deviceToken;

    private DeviceType deviceType;

    @Size(max = 100, message = "Device name must not exceed 100 characters")
    private String deviceName;

    @Size(max = 20, message = "App version must not exceed 20 characters")
    private String appVersion;
}
