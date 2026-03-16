package com.fooddelivery.sms.dto;

import com.fooddelivery.sms.config.SmsProperties.SmsProviderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for changing SMS provider configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SMS provider configuration request")
public class SmsProviderConfigRequest {

    @NotNull(message = "Provider is required")
    @Schema(description = "Primary SMS provider", example = "ESKIZ")
    private SmsProviderType provider;

    @Schema(description = "Enable fallback provider", example = "true")
    private Boolean fallbackEnabled;

    @Schema(description = "Fallback SMS provider", example = "DEVSMS")
    private SmsProviderType fallbackProvider;

    @Schema(description = "Enable/disable SMS globally", example = "true")
    private Boolean enabled;
}
