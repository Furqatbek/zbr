package com.fooddelivery.order.dto;

import com.fooddelivery.order.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for payment information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment information")
public class PaymentDto {

    @Schema(description = "Payment ID")
    private Long id;

    @Schema(description = "Order ID")
    private Long orderId;

    @Schema(description = "Payment provider")
    private String provider;

    @Schema(description = "Provider payment ID")
    private String providerPaymentId;

    @Schema(description = "Payment intent ID (for client-side completion)")
    private String paymentIntentId;

    @Schema(description = "Client secret (for Stripe-like providers)")
    private String clientSecret;

    @Schema(description = "Payment method")
    private String paymentMethod;

    @Schema(description = "Amount")
    private BigDecimal amount;

    @Schema(description = "Currency")
    private String currency;

    @Schema(description = "Payment status")
    private PaymentStatus status;

    @Schema(description = "Error message (if failed)")
    private String errorMessage;

    @Schema(description = "Refund amount")
    private BigDecimal refundAmount;

    @Schema(description = "Created at")
    private LocalDateTime createdAt;

    @Schema(description = "Confirmed at")
    private LocalDateTime confirmedAt;

    @Schema(description = "Refunded at")
    private LocalDateTime refundedAt;
}
