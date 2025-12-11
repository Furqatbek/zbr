package com.fooddelivery.webhook.controller;

import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.order.service.PaymentService;
import com.fooddelivery.webhook.dto.PaymentWebhookPayload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling external webhooks.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "External webhook endpoints")
public class WebhookController {

    private final PaymentService paymentService;

    @Value("${app.payment.webhook-secret}")
    private String webhookSecret;

    /**
     * Handle payment provider webhook (Stripe-like).
     */
    @PostMapping("/payments")
    @Operation(summary = "Payment webhook", description = "Handle payment provider webhooks")
    public ResponseEntity<ApiResponse<Void>> handlePaymentWebhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            @RequestBody PaymentWebhookPayload payload) {

        log.info("Received payment webhook: type={}, id={}", payload.getType(), payload.getId());

        // In production, verify the signature:
        // Webhook.constructEvent(payload, signature, webhookSecret);

        try {
            switch (payload.getType()) {
                case "payment_intent.succeeded" -> {
                    log.info("Payment succeeded: {}", payload.getData().getPaymentIntentId());
                    paymentService.confirmPayment(
                            payload.getData().getPaymentIntentId(),
                            payload.getData().getPaymentId(),
                            payload.getRawData()
                    );
                }
                case "payment_intent.payment_failed" -> {
                    log.warn("Payment failed: {}", payload.getData().getPaymentIntentId());
                    paymentService.failPayment(
                            payload.getData().getPaymentIntentId(),
                            payload.getData().getErrorCode(),
                            payload.getData().getErrorMessage(),
                            payload.getRawData()
                    );
                }
                case "charge.refunded" -> {
                    log.info("Charge refunded: {}", payload.getData().getPaymentId());
                    // Handle refund confirmation
                }
                default -> log.debug("Unhandled webhook event type: {}", payload.getType());
            }

            return ResponseEntity.ok(ApiResponse.success("Webhook processed"));

        } catch (Exception e) {
            log.error("Webhook processing error: {}", e.getMessage(), e);
            // Return 200 to prevent webhook retries for known errors
            return ResponseEntity.ok(ApiResponse.error("Webhook processing failed: " + e.getMessage()));
        }
    }

    /**
     * Handle refund webhook.
     */
    @PostMapping("/refund")
    @Operation(summary = "Refund webhook", description = "Handle refund webhooks")
    public ResponseEntity<ApiResponse<Void>> handleRefundWebhook(
            @RequestBody PaymentWebhookPayload payload) {

        log.info("Received refund webhook: {}", payload.getId());

        // Process refund confirmation
        try {
            if ("refund.succeeded".equals(payload.getType())) {
                log.info("Refund succeeded for payment: {}", payload.getData().getPaymentId());
            }
            return ResponseEntity.ok(ApiResponse.success("Refund webhook processed"));
        } catch (Exception e) {
            log.error("Refund webhook error: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error("Processing failed"));
        }
    }

    /**
     * Health check endpoint for webhook configuration verification.
     */
    @GetMapping("/health")
    @Operation(summary = "Webhook health check", description = "Verify webhook endpoint is reachable")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Webhook endpoint healthy", "OK"));
    }
}
