package com.fooddelivery.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO after sending OTP.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSendResponse {

    private String phone;
    private String message;
    private Integer expiresInSeconds;
    private Boolean isNewUser;
    private Integer remainingAttempts;
}
