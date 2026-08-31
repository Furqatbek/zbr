package com.fooddelivery.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating consumer profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    /**
     * Convenience alternative to firstName/lastName, split on the first space.
     * completeRegistration already accepts fullName, and clients reasonably
     * expect the same field here; sending only firstName/lastName still works.
     *
     * <p>101, not 50: this holds both halves plus the separating space, so it
     * must admit anything the two fields would accept separately.
     */
    @Size(max = 101, message = "Full name must be less than 101 characters")
    private String fullName;

    @Size(max = 50, message = "First name must be less than 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must be less than 50 characters")
    private String lastName;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must be less than 100 characters")
    private String email;

    @Size(max = 255, message = "Address must be less than 255 characters")
    private String address;

    private Double latitude;

    private Double longitude;

    @Size(max = 255, message = "Profile image URL must be less than 255 characters")
    private String profileImageUrl;
}
