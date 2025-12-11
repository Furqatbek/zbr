package com.fooddelivery.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for confirming password reset.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Password reset confirmation request")
public class PasswordResetConfirmRequest {

    @Schema(description = "Password reset token", example = "abc123...", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Token is required")
    private String token;

    @Schema(description = "New password", example = "NewSecureP@ss123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
            message = "Password must contain at least one digit, one lowercase, one uppercase letter, and one special character")
    private String newPassword;
}
