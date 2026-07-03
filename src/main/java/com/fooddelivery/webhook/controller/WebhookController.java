package com.fooddelivery.webhook.controller;

import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.order.service.PaymentService;
import com.fooddelivery.webhook.dto.PaymentWebhookPayload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Controller for handling external webhooks.
 *
 * SECURITY: this endpoint is publicly routable (payment providers are external),
 * so it fails closed. The webhook secret must be configured to a non-default
 * value, and every mutating call must present that secret in the Stripe-Signature
 * header (constant-time compared). Until the real payment gateway is wired in,
 * this shared-secret check is what prevents anonymous "mark any order paid"
 * abuse; it should be replaced with real provider HMAC-over-raw-body verification.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "External webhook endpoints")
public class WebhookController {

    private static final String PLACEHOLDER_SECRET = "whsec_test_secret";

    private final PaymentService paymentService;

    @Value("${app.payment.webhook-secret:}")
    private String webhookSecret;

    /**
     * Reject the request unless the webhook secret is properly configured and the
     * caller presents it. Fails closed on misconfiguration.
     */
    private void verifyWebhookAuth(String signature) {
        if (webhookSecret == null || webhookSecret.isBlank() || PLACEHOLDER_SECRET.equals(webhookSecret)) {
            log.error("Payment webhook rejected: PAYMENT_WEBHOOK_SECRET is not configured (or is the default placeholder).");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Webhook not configured");
        }
        if (signature == null || !MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                webhookSecret.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Payment webhook rejected: missing/invalid signature");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }
    }

    /**
     * Handle payment provider webhook (Stripe-like).
     */
    @PostMapping("/payments")
    @Operation(summary = "Payment webhook", description = "Handle payment provider webhooks")
    public ResponseEntity<ApiResponse<Void>> handlePaymentWebhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            @RequestBody PaymentWebhookPayload payload) {

        verifyWebhookAuth(signature);

        log.info("Received payment webhook: type={}, id={}", payload.getType(), payload.getId());

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
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            @RequestBody PaymentWebhookPayload payload) {

        verifyWebhookAuth(signature);

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
