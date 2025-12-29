package com.fooddelivery.auth.dto;

import com.fooddelivery.auth.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Response DTO for authentication operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response with tokens")
public class AuthResponse {

    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "JWT refresh token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Access token expiration time in seconds", example = "900")
    private Long expiresIn;

    @Schema(description = "User ID", example = "1")
    private Long userId;

    @Schema(description = "User email", example = "john.doe@example.com")
    private String email;

    @Schema(description = "User full name", example = "John Doe")
    private String fullName;

    @Schema(description = "User roles", example = "[\"CONSUMER\"]")
    private Set<Role> roles;

    @Schema(description = "User details")
    private UserDto user;
}
