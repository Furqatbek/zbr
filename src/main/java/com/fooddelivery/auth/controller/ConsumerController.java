package com.fooddelivery.auth.controller;

import com.fooddelivery.auth.dto.ConsumerAddressDto;
import com.fooddelivery.auth.dto.CreateAddressRequest;
import com.fooddelivery.auth.dto.UpdateProfileRequest;
import com.fooddelivery.auth.dto.UserDto;
import com.fooddelivery.auth.service.ConsumerAddressService;
import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.auth.service.PhoneAuthService;
import com.fooddelivery.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
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
    private final ConsumerAddressService addressService;

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

    // ==================== Address Management ====================

    /**
     * Get all saved addresses for the current consumer.
     */
    @GetMapping("/addresses")
    @Operation(summary = "Get addresses", description = "Get all saved addresses for the current consumer")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<ApiResponse<List<ConsumerAddressDto>>> getAddresses(
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("Get addresses request for user: {}", principal.getId());
        List<ConsumerAddressDto> addresses = addressService.getAddresses(principal.getId());

        return ResponseEntity.ok(ApiResponse.success(addresses));
    }

    /**
     * Add a new address.
     */
    @PostMapping("/addresses")
    @Operation(summary = "Add address", description = "Add a new saved address")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<ApiResponse<ConsumerAddressDto>> addAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateAddressRequest request) {

        log.info("Add address request for user: {}", principal.getId());
        ConsumerAddressDto address = addressService.addAddress(principal.getId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address added successfully", address));
    }

    /**
     * Update an existing address.
     */
    @PutMapping("/addresses/{addressId}")
    @Operation(summary = "Update address", description = "Update an existing saved address")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<ApiResponse<ConsumerAddressDto>> updateAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long addressId,
            @Valid @RequestBody CreateAddressRequest request) {

        log.info("Update address request for user: {}, address: {}", principal.getId(), addressId);
        ConsumerAddressDto address = addressService.updateAddress(principal.getId(), addressId, request);

        return ResponseEntity.ok(ApiResponse.success("Address updated successfully", address));
    }

    /**
     * Delete an address.
     */
    @DeleteMapping("/addresses/{addressId}")
    @Operation(summary = "Delete address", description = "Delete a saved address")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long addressId) {

        log.info("Delete address request for user: {}, address: {}", principal.getId(), addressId);
        addressService.deleteAddress(principal.getId(), addressId);

        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully"));
    }

    /**
     * Set an address as the default.
     */
    @PutMapping("/addresses/{addressId}/default")
    @Operation(summary = "Set default address", description = "Set an address as the default delivery address")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<ApiResponse<ConsumerAddressDto>> setDefaultAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long addressId) {

        log.info("Set default address request for user: {}, address: {}", principal.getId(), addressId);
        ConsumerAddressDto address = addressService.setDefaultAddress(principal.getId(), addressId);

        return ResponseEntity.ok(ApiResponse.success("Default address updated", address));
    }
}
