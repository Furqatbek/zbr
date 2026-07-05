package com.fooddelivery.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for initiating phone-based login/signup.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneLoginRequest {

    @NotBlank(message = "Phone number is required")
    // Uzbek numbers only: +998XXXXXXXXX / 998XXXXXXXXX / bare 9-digit local.
    // Rejects wrong-length (e.g. 13-digit) and non-+998 country codes before any SMS is sent.
    @Pattern(regexp = "^(\\+?998[0-9]{9}|[0-9]{9})$",
            message = "Enter a valid Uzbek phone number, e.g. +998901234567")
    private String phone;
}
