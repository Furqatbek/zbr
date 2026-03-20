package com.fooddelivery.notification.controller;

import com.fooddelivery.auth.service.AuthService;
import com.fooddelivery.notification.dto.DeviceTokenRequest;
import com.fooddelivery.notification.entity.UserDeviceToken;
import com.fooddelivery.notification.service.DeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for managing device tokens for push notifications.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/device-tokens")
@RequiredArgsConstructor
@Tag(name = "Device Tokens", description = "Push notification device token management")
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;
    private final AuthService authService;

    /**
     * Register or update a device token for push notifications.
     * Call this after user login or when FCM token refreshes.
     */
    @PostMapping
    @Operation(summary = "Register device token",
            description = "Register or update FCM device token for push notifications")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Map<String, Object>> registerToken(@Valid @RequestBody DeviceTokenRequest request) {
        Long userId = authService.getCurrentUserId();
        log.info("Registering device token for user {}", userId);

        UserDeviceToken token = deviceTokenService.registerToken(userId, request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Device token registered successfully",
                "tokenId", token.getId()
        ));
    }

    /**
     * Remove a device token (e.g., on logout).
     */
    @DeleteMapping
    @Operation(summary = "Remove device token",
            description = "Deactivate device token on logout")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token removed successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Map<String, Object>> removeToken(@RequestBody Map<String, String> request) {
        String deviceToken = request.get("deviceToken");
        if (deviceToken == null || deviceToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Device token is required"
            ));
        }

        deviceTokenService.deactivateToken(deviceToken);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Device token removed successfully"
        ));
    }

    /**
     * Remove all device tokens for current user (logout from all devices).
     */
    @DeleteMapping("/all")
    @Operation(summary = "Remove all device tokens",
            description = "Deactivate all device tokens for current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All tokens removed successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Map<String, Object>> removeAllTokens() {
        Long userId = authService.getCurrentUserId();
        deviceTokenService.deactivateAllTokens(userId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "All device tokens removed successfully"
        ));
    }
}
