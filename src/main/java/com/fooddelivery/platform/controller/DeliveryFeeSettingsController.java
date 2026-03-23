package com.fooddelivery.platform.controller;

import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.order.dto.DeliveryFeeSettingDto;
import com.fooddelivery.order.dto.DeliveryFeeSettingsDto;
import com.fooddelivery.order.dto.UpdateDeliveryFeeSettingsRequest;
import com.fooddelivery.order.service.DeliveryFeeSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for managing delivery fee settings.
 */
@RestController
@RequestMapping("/api/v1/admin/delivery-fee-settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Delivery Fee Settings", description = "Manage delivery fee configuration")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM')")
public class DeliveryFeeSettingsController {

    private final DeliveryFeeSettingsService deliveryFeeSettingsService;

    @GetMapping
    @Operation(summary = "Get delivery fee settings", description = "Get all delivery fee settings as a consolidated object")
    public ResponseEntity<ApiResponse<DeliveryFeeSettingsDto>> getSettings() {
        log.info("Fetching delivery fee settings");
        DeliveryFeeSettingsDto settings = deliveryFeeSettingsService.getSettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all individual settings", description = "Get all delivery fee settings as individual key-value pairs")
    public ResponseEntity<ApiResponse<List<DeliveryFeeSettingDto>>> getAllSettings() {
        log.info("Fetching all delivery fee settings individually");
        List<DeliveryFeeSettingDto> settings = deliveryFeeSettingsService.getAllSettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping
    @Operation(summary = "Update delivery fee settings", description = "Update one or more delivery fee settings")
    public ResponseEntity<ApiResponse<DeliveryFeeSettingsDto>> updateSettings(
            @Valid @RequestBody UpdateDeliveryFeeSettingsRequest request) {
        log.info("Updating delivery fee settings: {}", request);
        DeliveryFeeSettingsDto updated = deliveryFeeSettingsService.updateSettings(request);
        return ResponseEntity.ok(ApiResponse.success("Delivery fee settings updated successfully", updated));
    }

    @PatchMapping("/{key}")
    @Operation(summary = "Update single setting", description = "Update a specific delivery fee setting by key")
    public ResponseEntity<ApiResponse<DeliveryFeeSettingDto>> updateSingleSetting(
            @PathVariable String key,
            @RequestParam BigDecimal value) {
        log.info("Updating delivery fee setting: {} = {}", key, value);
        DeliveryFeeSettingDto updated = deliveryFeeSettingsService.updateSingleSetting(key.toUpperCase(), value);
        return ResponseEntity.ok(ApiResponse.success("Setting updated successfully", updated));
    }
}
