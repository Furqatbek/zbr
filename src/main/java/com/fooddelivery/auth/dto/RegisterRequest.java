package com.fooddelivery.auth.dto;

import com.fooddelivery.auth.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user registration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User registration request")
public class RegisterRequest {

    @Schema(description = "User email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Schema(description = "User password", example = "SecureP@ss123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
            message = "Password must contain at least one digit, one lowercase, one uppercase letter, and one special character")
    private String password;

    @Schema(description = "User phone number", example = "+1234567890")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phone;

    @Schema(description = "User first name", example = "John")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Schema(description = "User last name", example = "Doe")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Schema(description = "User role", example = "CONSUMER", requiredMode = Schema.RequiredMode.REQUIRED)
    @Builder.Default
    private Role role = Role.CONSUMER;

    @Schema(description = "Referral code (optional)", example = "REF123ABC")
    private String referralCode;
}
