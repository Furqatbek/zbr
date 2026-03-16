package com.fooddelivery.sms.controller;

import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.sms.config.SmsProperties;
import com.fooddelivery.sms.config.SmsProperties.SmsProviderType;
import com.fooddelivery.sms.dto.SmsProviderConfigRequest;
import com.fooddelivery.sms.dto.SmsProviderStatusResponse;
import com.fooddelivery.sms.dto.SmsProviderStatusResponse.ProviderStatus;
import com.fooddelivery.sms.service.SmsProvider;
import com.fooddelivery.sms.service.SmsProviderFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * REST controller for managing SMS providers.
 */
@RestController
@RequestMapping("/api/v1/admin/sms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SMS Provider Management", description = "Manage SMS provider configuration")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM')")
public class SmsProviderController {

    private final SmsProperties smsProperties;
    private final SmsProviderFactory smsProviderFactory;

    /**
     * Get current SMS provider status.
     */
    @GetMapping("/status")
    @Operation(summary = "Get SMS provider status", description = "Get current SMS provider configuration and status")
    public ResponseEntity<ApiResponse<SmsProviderStatusResponse>> getStatus() {
        log.info("Getting SMS provider status");

        List<ProviderStatus> providers = new ArrayList<>();

        // Check each provider type
        for (SmsProviderType type : SmsProviderType.values()) {
            SmsProvider provider = smsProviderFactory.getProvider(type);
            providers.add(ProviderStatus.builder()
                    .type(type)
                    .configured(provider != null)
                    .available(provider != null && provider.isAvailable())
                    .statusMessage(getProviderStatusMessage(provider))
                    .build());
        }

        SmsProviderStatusResponse response = SmsProviderStatusResponse.builder()
                .enabled(smsProperties.isEnabled())
                .currentProvider(smsProperties.getProvider())
                .fallbackEnabled(smsProperties.isFallbackEnabled())
                .fallbackProvider(smsProperties.getFallbackProvider())
                .activeProviderName(smsProviderFactory.getActiveProviderName())
                .providers(providers)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update SMS provider configuration.
     */
    @PutMapping("/config")
    @Operation(summary = "Update SMS provider config", description = "Change the active SMS provider and settings")
    public ResponseEntity<ApiResponse<SmsProviderStatusResponse>> updateConfig(
            @Valid @RequestBody SmsProviderConfigRequest request) {

        log.info("Updating SMS provider config: provider={}, fallbackEnabled={}, fallbackProvider={}",
                request.getProvider(), request.getFallbackEnabled(), request.getFallbackProvider());

        // Update configuration
        if (request.getProvider() != null) {
            smsProperties.setProvider(request.getProvider());
        }

        if (request.getFallbackEnabled() != null) {
            smsProperties.setFallbackEnabled(request.getFallbackEnabled());
        }

        if (request.getFallbackProvider() != null) {
            smsProperties.setFallbackProvider(request.getFallbackProvider());
        }

        if (request.getEnabled() != null) {
            smsProperties.setEnabled(request.getEnabled());
        }

        log.info("SMS provider config updated successfully");

        // Return updated status
        return getStatus();
    }

    /**
     * Switch to a specific provider.
     */
    @PostMapping("/switch/{provider}")
    @Operation(summary = "Switch SMS provider", description = "Quick switch to a specific SMS provider")
    public ResponseEntity<ApiResponse<SmsProviderStatusResponse>> switchProvider(
            @PathVariable SmsProviderType provider) {

        log.info("Switching SMS provider to: {}", provider);

        // Validate provider is available
        SmsProvider smsProvider = smsProviderFactory.getProvider(provider);
        if (smsProvider == null || !smsProvider.isAvailable()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Provider " + provider + " is not available"));
        }

        smsProperties.setProvider(provider);

        log.info("SMS provider switched to: {}", provider);

        return getStatus();
    }

    /**
     * Enable/disable SMS globally.
     */
    @PostMapping("/toggle")
    @Operation(summary = "Toggle SMS", description = "Enable or disable SMS sending globally")
    public ResponseEntity<ApiResponse<SmsProviderStatusResponse>> toggleSms(
            @RequestParam boolean enabled) {

        log.info("Toggling SMS: enabled={}", enabled);

        smsProperties.setEnabled(enabled);

        return getStatus();
    }

    /**
     * Test SMS sending.
     */
    @PostMapping("/test")
    @Operation(summary = "Send test SMS", description = "Send a test SMS to verify provider configuration")
    public ResponseEntity<ApiResponse<String>> sendTestSms(
            @RequestParam String phoneNumber,
            @RequestParam(defaultValue = "Test SMS from Food Delivery Platform") String message) {

        log.info("Sending test SMS to: {}", maskPhone(phoneNumber));

        if (!smsProviderFactory.isAnyProviderAvailable()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("No SMS provider available"));
        }

        try {
            var response = smsProviderFactory.sendSms(
                    com.fooddelivery.sms.dto.SmsMessage.builder()
                            .phoneNumber(phoneNumber)
                            .message(message)
                            .type(com.fooddelivery.sms.dto.SmsMessage.SmsType.GENERAL)
                            .build()
            );

            if (response.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(
                        "Test SMS sent successfully via " + response.getProvider(),
                        "SMS ID: " + response.getSmsId()
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to send SMS: " + response.getMessage()));
            }

        } catch (Exception e) {
            log.error("Test SMS failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to send test SMS: " + e.getMessage()));
        }
    }

    private String getProviderStatusMessage(SmsProvider provider) {
        if (provider == null) {
            return "Not configured";
        }
        if (!provider.isAvailable()) {
            return "Configured but not available (check credentials)";
        }
        return "Ready";
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) {
            return "***";
        }
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }
}
