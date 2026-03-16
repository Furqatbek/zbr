package com.fooddelivery.sms.dto;

import com.fooddelivery.sms.config.SmsProperties.SmsProviderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating SMS provider credentials.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SMS provider credentials update request")
public class ProviderCredentialsRequest {

    @NotNull(message = "Provider type is required")
    @Schema(description = "Provider type to configure", example = "DEVSMS")
    private SmsProviderType provider;

    @Schema(description = "Enable or disable this provider", example = "true")
    private Boolean enabled;

    @Schema(description = "API token (for DevSMS)", example = "your-api-token")
    private String token;

    @Schema(description = "Email (for Eskiz)", example = "your@email.com")
    private String email;

    @Schema(description = "Password (for Eskiz)", example = "your-password")
    private String password;

    @Schema(description = "Sender ID / From name", example = "4546")
    private String from;

    @Schema(description = "Base URL for the API", example = "https://devsms.uz/api")
    private String baseUrl;

    @Schema(description = "Callback URL for delivery reports")
    private String callbackUrl;
}
