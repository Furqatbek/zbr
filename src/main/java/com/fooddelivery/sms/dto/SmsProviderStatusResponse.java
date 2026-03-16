package com.fooddelivery.sms.dto;

import com.fooddelivery.sms.config.SmsProperties.SmsProviderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for SMS provider status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SMS provider status response")
public class SmsProviderStatusResponse {

    @Schema(description = "Whether SMS is globally enabled")
    private boolean enabled;

    @Schema(description = "Current primary provider")
    private SmsProviderType currentProvider;

    @Schema(description = "Whether fallback is enabled")
    private boolean fallbackEnabled;

    @Schema(description = "Fallback provider")
    private SmsProviderType fallbackProvider;

    @Schema(description = "Active provider name (including fallback indicator)")
    private String activeProviderName;

    @Schema(description = "Available providers with their status")
    private List<ProviderStatus> providers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderStatus {
        @Schema(description = "Provider type")
        private SmsProviderType type;

        @Schema(description = "Whether provider is configured")
        private boolean configured;

        @Schema(description = "Whether provider is available/ready")
        private boolean available;

        @Schema(description = "Provider-specific status message")
        private String statusMessage;
    }
}
