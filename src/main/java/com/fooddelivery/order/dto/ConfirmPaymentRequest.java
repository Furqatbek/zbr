package com.fooddelivery.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for confirming a payment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Confirm payment request")
public class ConfirmPaymentRequest {

    @NotBlank(message = "Payment intent ID is required")
    @Schema(description = "The payment intent ID from the payment provider")
    private String paymentIntentId;

    @Schema(description = "The provider's payment ID (from webhook or confirmation)")
    private String providerPaymentId;
}
