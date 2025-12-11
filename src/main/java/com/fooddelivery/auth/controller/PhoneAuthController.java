package com.fooddelivery.auth.controller;

import com.fooddelivery.auth.dto.*;
import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.auth.service.PhoneAuthService;
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
     * Send OTP to phone number for login/signup.
     */
    @PostMapping("/send-otp")
    @Operation(summary = "Send OTP", description = "Send verification code to phone number")
    public ResponseEntity<ApiResponse<OtpSendResponse>> sendOtp(
            @Valid @RequestBody PhoneLoginRequest request) {

        log.info("OTP request received for phone: {}", maskPhone(request.getPhone()));
        OtpSendResponse response = phoneAuthService.initiatePhoneAuth(request);

        return ResponseEntity.ok(ApiResponse.success("Verification code sent", response));
    }

    /**
     * Verify OTP and complete login/signup.
     */
    @PostMapping("/verify")
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
    @Operation(summary = "Resend OTP", description = "Resend verification code to phone number")
    public ResponseEntity<ApiResponse<OtpSendResponse>> resendOtp(
            @Valid @RequestBody PhoneLoginRequest request) {

        log.info("OTP resend request for phone: {}", maskPhone(request.getPhone()));
        OtpSendResponse response = phoneAuthService.resendOtp(request);

        return ResponseEntity.ok(ApiResponse.success("Verification code resent", response));
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
