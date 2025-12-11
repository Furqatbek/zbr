package com.fooddelivery.webhook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for payment webhook payload (Stripe-like structure).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment webhook payload")
public class PaymentWebhookPayload {

    @Schema(description = "Webhook event ID")
    private String id;

    @Schema(description = "Event type (e.g., payment_intent.succeeded)")
    private String type;

    @Schema(description = "API version")
    @JsonProperty("api_version")
    private String apiVersion;

    @Schema(description = "Event data")
    private PaymentData data;

    @Schema(description = "Raw webhook payload (for signature verification)")
    private String rawData;

    @Schema(description = "Webhook created timestamp")
    private Long created;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Payment data")
    public static class PaymentData {

        @Schema(description = "Payment intent ID")
        @JsonProperty("payment_intent_id")
        private String paymentIntentId;

        @Schema(description = "Payment/charge ID")
        @JsonProperty("payment_id")
        private String paymentId;

        @Schema(description = "Amount in smallest currency unit (cents)")
        private Long amount;

        @Schema(description = "Amount decimal")
        @JsonProperty("amount_decimal")
        private BigDecimal amountDecimal;

        @Schema(description = "Currency")
        private String currency;

        @Schema(description = "Payment status")
        private String status;

        @Schema(description = "Payment method type")
        @JsonProperty("payment_method_type")
        private String paymentMethodType;

        @Schema(description = "Customer ID")
        @JsonProperty("customer_id")
        private String customerId;

        @Schema(description = "Error code (for failed payments)")
        @JsonProperty("error_code")
        private String errorCode;

        @Schema(description = "Error message (for failed payments)")
        @JsonProperty("error_message")
        private String errorMessage;

        @Schema(description = "Metadata")
        private String metadata;
    }
}
