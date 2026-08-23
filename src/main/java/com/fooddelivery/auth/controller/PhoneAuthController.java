package com.fooddelivery.auth.controller;

import com.fooddelivery.auth.dto.*;
import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.auth.service.PhoneAuthService;
import com.fooddelivery.common.annotation.RateLimited;
import com.fooddelivery.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for phone-based authentication (OTP login/signup).
 */
@RestController
@RequestMapping("/api/v1/auth/phone")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Phone Authentication", description = "OTP-based authentication for consumers")
public class PhoneAuthController {

    private final PhoneAuthService phoneAuthService;

    /**
     * Request OTP to phone number for login/signup.
     */
    @PostMapping("/request-otp")
    // Every OTP is a paid SMS. OtpService caps 5/hour PER PHONE, which bounds
    // the cost of targeting one number but not the cost of walking through
    // many — without a per-IP cap a script can bill us across the whole
    // +998 range. These two limits are complementary, not redundant.
    @RateLimited(requestsPerMinute = 5, keyType = RateLimited.KeyType.IP)
    @Operation(summary = "Request OTP", description = "Send verification code to phone number")
    public ResponseEntity<ApiResponse<OtpSendResponse>> sendOtp(
            @Valid @RequestBody PhoneLoginRequest request) {

        log.info("OTP request received for phone: {}", maskPhone(request.getPhone()));
        OtpSendResponse response = phoneAuthService.initiatePhoneAuth(request);

        return ResponseEntity.ok(ApiResponse.success("Verification code sent", response));
    }

    /**
     * Verify OTP and complete login/signup.
     */
    @PostMapping("/verify-otp")
    // Guessing a 6-digit code: OtpService allows 3 attempts per issued code,
    // but nothing stopped re-requesting codes and guessing indefinitely.
    @RateLimited(requestsPerMinute = 10, keyType = RateLimited.KeyType.IP)
    @Operation(summary = "Verify OTP", description = "Verify OTP code and authenticate user")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request) {

        log.info("OTP verification request for phone: {}", maskPhone(request.getPhone()));
        AuthResponse response = phoneAuthService.verifyOtpAndAuthenticate(request);

        return ResponseEntity.ok(ApiResponse.success("Authentication successful", response));
    }

    /**
     * Resend OTP to phone number.
     */
    @PostMapping("/resend-otp")
    @RateLimited(requestsPerMinute = 3, keyType = RateLimited.KeyType.IP)
    @Operation(summary = "Resend OTP", description = "Resend verification code to phone number")
    public ResponseEntity<ApiResponse<OtpSendResponse>> resendOtp(
            @Valid @RequestBody PhoneLoginRequest request) {

        log.info("OTP resend request for phone: {}", maskPhone(request.getPhone()));
        OtpSendResponse response = phoneAuthService.resendOtp(request);

        return ResponseEntity.ok(ApiResponse.success("Verification code resent", response));
    }

    /**
     * Complete phone registration for new users with additional details.
     */
    // Also verifies an OTP, so it is a second guessing door onto the same code.
    @RateLimited(requestsPerMinute = 10, keyType = RateLimited.KeyType.IP)
    @PostMapping("/complete-registration")
    @Operation(summary = "Complete registration", description = "Complete phone registration with user details for new users")
    public ResponseEntity<ApiResponse<AuthResponse>> completeRegistration(
            @Valid @RequestBody CompleteRegistrationRequest request) {

        log.info("Complete registration request for phone: {}", maskPhone(request.getPhone()));
        AuthResponse response = phoneAuthService.completeRegistration(request);

        return ResponseEntity.ok(ApiResponse.success("Registration completed successfully", response));
    }

    /**
     * Mask phone for logging.
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) {
            return "***";
        }
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }
}
