package com.fooddelivery.auth.dto;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User information")
public class UserDto {

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "User email", example = "john.doe@example.com")
    private String email;

    @Schema(description = "User phone number", example = "+1234567890")
    private String phone;

    @Schema(description = "User first name", example = "John")
    private String firstName;

    @Schema(description = "User last name", example = "Doe")
    private String lastName;

    @Schema(description = "User full name", example = "John Doe")
    private String fullName;

    @Schema(description = "Profile image URL")
    private String profileImageUrl;

    @Schema(description = "User roles", example = "[\"CONSUMER\"]")
    private Set<Role> roles;

    @Schema(description = "Single role (string format)", example = "CONSUMER")
    private String role;

    @Schema(description = "User status (enum)", example = "ACTIVE")
    private UserStatus status;

    @Schema(description = "Email verified flag", example = "true")
    private Boolean emailVerified;

    @Schema(description = "Phone verified flag", example = "false")
    private Boolean phoneVerified;

    @Schema(description = "Last login timestamp — moves only when credentials are exchanged for a token")
    private LocalDateTime lastLoginAt;

    @Schema(description = "Last authenticated activity of any kind (request, socket connect or token refresh), UTC. "
            + "Null until the account has been active at least once.")
    private LocalDateTime lastSeenAt;

    @Schema(description = "Account creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "User address")
    private String address;

    @Schema(description = "User latitude")
    private Double latitude;

    @Schema(description = "User longitude")
    private Double longitude;

    // ---- Consumer profile extras -------------------------------------------
    // Populated only by the consumer profile endpoints (GET/PUT
    // /consumers/profile). They are absent from auth responses, which must not
    // pay for the extra queries on every login. Being absent rather than null
    // is a consequence of default-property-inclusion: non_null.

    @Schema(description = "Alias of profileImageUrl, for clients that expect avatarUrl")
    private String avatarUrl;

    @Schema(description = "Alias of createdAt, for clients that expect memberSince")
    private LocalDateTime memberSince;

    @Schema(description = "The consumer's default saved address, if one is set")
    private ConsumerAddressDto defaultAddress;

    @Schema(description = "Lifetime order count for this consumer", example = "12")
    private Long totalOrders;
}
