package com.fooddelivery.auth.controller;

import com.fooddelivery.auth.dto.UpdateProfileRequest;
import com.fooddelivery.auth.dto.UserDto;
import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.auth.service.PhoneAuthService;
import com.fooddelivery.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for consumer profile management.
 */
@RestController
@RequestMapping("/api/v1/consumers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Consumer Profile", description = "Consumer profile management")
@SecurityRequirement(name = "bearerAuth")
public class ConsumerController {

    private final PhoneAuthService phoneAuthService;

    /**
     * Get current consumer profile.
     */
    @GetMapping("/profile")
    @Operation(summary = "Get profile", description = "Get current consumer's profile")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<ApiResponse<UserDto>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("Get profile request for user: {}", principal.getId());
        UserDto profile = phoneAuthService.getProfile(principal.getId());

        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", profile));
    }

    /**
     * Update current consumer profile.
     */
    @PutMapping("/profile")
    @Operation(summary = "Update profile", description = "Update current consumer's profile")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {

        log.info("Update profile request for user: {}", principal.getId());
        UserDto profile = phoneAuthService.updateProfile(principal.getId(), request);

        return ResponseEntity.ok(ApiResponse.success("Profile updated", profile));
    }

    /**
     * Get consumer profile by ID (for other services).
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get consumer by ID", description = "Get consumer profile by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<UserDto>> getConsumerById(@PathVariable Long id) {

        log.info("Get consumer request for id: {}", id);
        UserDto profile = phoneAuthService.getProfile(id);

        return ResponseEntity.ok(ApiResponse.success("Consumer retrieved", profile));
    }
}
