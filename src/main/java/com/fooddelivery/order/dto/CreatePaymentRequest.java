package com.fooddelivery.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a payment intent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create payment request")
public class CreatePaymentRequest {

    @Schema(description = "Payment method", example = "card", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @Schema(description = "Return URL for redirect-based payments")
    private String returnUrl;

    @Schema(description = "Payment metadata (optional)")
    private String metadata;
}
