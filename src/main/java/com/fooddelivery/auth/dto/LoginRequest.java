package com.fooddelivery.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user login.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User login request")
public class LoginRequest {

    @Schema(description = "User email or phone", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email or phone is required")
    private String emailOrPhone;

    @Schema(description = "User password", example = "SecureP@ss123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password is required")
    private String password;

    @Schema(description = "Device information for session tracking", example = "iPhone 14 Pro")
    private String deviceInfo;
}
