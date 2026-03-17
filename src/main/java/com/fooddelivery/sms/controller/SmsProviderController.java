package com.fooddelivery.sms.controller;

import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.sms.config.DevSmsProperties;
import com.fooddelivery.sms.config.EskizSmsProperties;
import com.fooddelivery.sms.config.SmsProperties;
import com.fooddelivery.sms.config.SmsProperties.SmsProviderType;
import com.fooddelivery.sms.dto.ProviderCredentialsRequest;
import com.fooddelivery.sms.dto.SmsProviderConfigRequest;
import com.fooddelivery.sms.dto.SmsProviderStatusResponse;
import com.fooddelivery.sms.dto.SmsProviderStatusResponse.ProviderStatus;
import com.fooddelivery.sms.service.SmsProvider;
import com.fooddelivery.sms.service.SmsProviderFactory;
import com.fooddelivery.sms.service.SmsSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Data;
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
    private final DevSmsProperties devSmsProperties;
    private final EskizSmsProperties eskizSmsProperties;
    private final SmsSettingsService smsSettingsService;

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

        // Update configuration and persist to database
        smsSettingsService.updateMainSettings(
                request.getEnabled(),
                request.getProvider(),
                request.getFallbackEnabled(),
                request.getFallbackProvider()
        );

        log.info("SMS provider config updated and persisted successfully");

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

        // Update and persist
        smsSettingsService.updateMainSettings(null, provider, null, null);

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

        // Update and persist
        smsSettingsService.updateMainSettings(enabled, null, null, null);

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

    /**
     * Configure provider credentials.
     */
    @PutMapping("/provider/credentials")
    @Operation(summary = "Configure provider credentials",
               description = "Update credentials and settings for a specific SMS provider")
    public ResponseEntity<ApiResponse<SmsProviderStatusResponse>> configureProvider(
            @Valid @RequestBody ProviderCredentialsRequest request) {

        log.info("Configuring SMS provider: {}", request.getProvider());

        switch (request.getProvider()) {
            case DEVSMS -> configureDevSms(request);
            case ESKIZ -> configureEskiz(request);
            default -> {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Unknown provider: " + request.getProvider()));
            }
        }

        log.info("SMS provider {} configured successfully", request.getProvider());
        return getStatus();
    }

    /**
     * Enable a provider.
     */
    @PostMapping("/provider/{provider}/enable")
    @Operation(summary = "Enable provider", description = "Enable a specific SMS provider")
    public ResponseEntity<ApiResponse<SmsProviderStatusResponse>> enableProvider(
            @PathVariable SmsProviderType provider) {

        log.info("Enabling SMS provider: {}", provider);

        switch (provider) {
            case DEVSMS -> smsSettingsService.updateDevSmsSettings(true, null, null, null, null);
            case ESKIZ -> smsSettingsService.updateEskizSettings(true, null, null, null, null, null);
        }

        return getStatus();
    }

    /**
     * Disable a provider.
     */
    @PostMapping("/provider/{provider}/disable")
    @Operation(summary = "Disable provider", description = "Disable a specific SMS provider")
    public ResponseEntity<ApiResponse<SmsProviderStatusResponse>> disableProvider(
            @PathVariable SmsProviderType provider) {

        log.info("Disabling SMS provider: {}", provider);

        switch (provider) {
            case DEVSMS -> smsSettingsService.updateDevSmsSettings(false, null, null, null, null);
            case ESKIZ -> smsSettingsService.updateEskizSettings(false, null, null, null, null, null);
        }

        return getStatus();
    }

    /**
     * Get provider details.
     */
    @GetMapping("/provider/{provider}")
    @Operation(summary = "Get provider details", description = "Get configuration details for a specific provider")
    public ResponseEntity<ApiResponse<ProviderDetailsResponse>> getProviderDetails(
            @PathVariable SmsProviderType provider) {

        log.info("Getting details for SMS provider: {}", provider);

        ProviderDetailsResponse details = switch (provider) {
            case DEVSMS -> ProviderDetailsResponse.builder()
                    .provider(provider)
                    .enabled(devSmsProperties.isEnabled())
                    .baseUrl(devSmsProperties.getBaseUrl())
                    .from(devSmsProperties.getFrom())
                    .hasToken(devSmsProperties.getToken() != null && !devSmsProperties.getToken().isBlank())
                    .callbackUrl(devSmsProperties.getCallbackUrl())
                    .available(smsProviderFactory.getProvider(provider) != null
                            && smsProviderFactory.getProvider(provider).isAvailable())
                    .build();
            case ESKIZ -> ProviderDetailsResponse.builder()
                    .provider(provider)
                    .enabled(eskizSmsProperties.isEnabled())
                    .baseUrl(eskizSmsProperties.getBaseUrl())
                    .from(eskizSmsProperties.getFrom())
                    .hasCredentials(eskizSmsProperties.getEmail() != null
                            && eskizSmsProperties.getPassword() != null)
                    .callbackUrl(eskizSmsProperties.getCallbackUrl())
                    .available(smsProviderFactory.getProvider(provider) != null
                            && smsProviderFactory.getProvider(provider).isAvailable())
                    .build();
        };

        return ResponseEntity.ok(ApiResponse.success(details));
    }

    private void configureDevSms(ProviderCredentialsRequest request) {
        smsSettingsService.updateDevSmsSettings(
                request.getEnabled(),
                request.getToken(),
                request.getFrom(),
                request.getBaseUrl(),
                request.getCallbackUrl()
        );
    }

    private void configureEskiz(ProviderCredentialsRequest request) {
        smsSettingsService.updateEskizSettings(
                request.getEnabled(),
                request.getEmail(),
                request.getPassword(),
                request.getFrom(),
                request.getBaseUrl(),
                request.getCallbackUrl()
        );
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

    /**
     * Response DTO for provider details.
     */
    @Data
    @Builder
    public static class ProviderDetailsResponse {
        private SmsProviderType provider;
        private boolean enabled;
        private String baseUrl;
        private String from;
        private boolean hasToken;
        private boolean hasCredentials;
        private String callbackUrl;
        private boolean available;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) {
            return "***";
        }
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }
}
