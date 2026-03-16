package com.fooddelivery.sms.controller;

import com.fooddelivery.sms.dto.DevSmsCallbackRequest;
import com.fooddelivery.sms.dto.EskizCallbackRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for receiving SMS delivery status callbacks from providers.
 * These endpoints are public (no authentication) as they are called by SMS providers.
 */
@RestController
@RequestMapping("/api/v1/sms/callback")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SMS Callbacks", description = "SMS delivery status webhook endpoints")
public class SmsCallbackController {

    /**
     * Receive delivery status callback from DevSMS.
     * Configure this URL in DevSMS: https://your-domain.com/api/v1/sms/callback/devsms
     */
    @PostMapping("/devsms")
    @Operation(summary = "DevSMS callback", description = "Receive SMS delivery status from DevSMS")
    public ResponseEntity<Map<String, Object>> handleDevSmsCallback(
            @RequestBody DevSmsCallbackRequest request) {

        log.info("DevSMS callback received: smsId={}, status={}, phone={}",
                request.getSmsId(),
                request.getStatus(),
                maskPhone(request.getPhone()));

        try {
            processDevSmsStatus(request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Callback processed"
            ));
        } catch (Exception e) {
            log.error("Failed to process DevSMS callback: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Processing failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Receive delivery status callback from Eskiz.
     * Configure this URL in Eskiz: https://your-domain.com/api/v1/sms/callback/eskiz
     */
    @PostMapping("/eskiz")
    @Operation(summary = "Eskiz callback", description = "Receive SMS delivery status from Eskiz")
    public ResponseEntity<Map<String, Object>> handleEskizCallback(
            @RequestBody EskizCallbackRequest request) {

        log.info("Eskiz callback received: messageId={}, status={}, phone={}",
                request.getMessageId(),
                request.getStatus(),
                maskPhone(request.getMobilePhone()));

        try {
            processEskizStatus(request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Callback processed"
            ));
        } catch (Exception e) {
            log.error("Failed to process Eskiz callback: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Processing failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Process DevSMS delivery status.
     */
    private void processDevSmsStatus(DevSmsCallbackRequest request) {
        String normalizedStatus = normalizeDevSmsStatus(request.getStatus());

        switch (normalizedStatus) {
            case "DELIVERED" -> log.info("SMS delivered: smsId={}", request.getSmsId());
            case "FAILED" -> log.warn("SMS delivery failed: smsId={}, error={}, errorCode={}",
                    request.getSmsId(), request.getErrorMessage(), request.getErrorCode());
            case "EXPIRED" -> log.warn("SMS expired: smsId={}", request.getSmsId());
            case "PENDING", "SENT" -> log.debug("SMS status update: smsId={}, status={}",
                    request.getSmsId(), normalizedStatus);
            default -> log.debug("Unknown DevSMS status: smsId={}, status={}",
                    request.getSmsId(), request.getStatus());
        }

        // TODO: Update SMS status in database if tracking is implemented
        // smsTrackingService.updateStatus(request.getSmsId(), normalizedStatus, "DEVSMS");
    }

    /**
     * Process Eskiz delivery status.
     */
    private void processEskizStatus(EskizCallbackRequest request) {
        String normalizedStatus = normalizeEskizStatus(request.getStatus());

        switch (normalizedStatus) {
            case "DELIVERED" -> log.info("SMS delivered: messageId={}", request.getMessageId());
            case "FAILED" -> log.warn("SMS delivery failed: messageId={}, error={}",
                    request.getMessageId(), request.getError());
            case "EXPIRED" -> log.warn("SMS expired: messageId={}", request.getMessageId());
            case "PENDING", "SENT" -> log.debug("SMS status update: messageId={}, status={}",
                    request.getMessageId(), normalizedStatus);
            default -> log.debug("Unknown Eskiz status: messageId={}, status={}",
                    request.getMessageId(), request.getStatus());
        }

        // TODO: Update SMS status in database if tracking is implemented
        // smsTrackingService.updateStatus(request.getMessageId(), normalizedStatus, "ESKIZ");
    }

    /**
     * Normalize DevSMS status to standard values.
     */
    private String normalizeDevSmsStatus(String status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status.toLowerCase()) {
            case "delivered", "доставлено" -> "DELIVERED";
            case "failed", "ошибка", "error" -> "FAILED";
            case "expired", "истёк" -> "EXPIRED";
            case "sent", "отправлено" -> "SENT";
            case "pending", "в ожидании" -> "PENDING";
            default -> status.toUpperCase();
        };
    }

    /**
     * Normalize Eskiz status to standard values.
     */
    private String normalizeEskizStatus(String status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status.toLowerCase()) {
            case "delivered", "доставлено", "success" -> "DELIVERED";
            case "failed", "ошибка", "error", "undelivered" -> "FAILED";
            case "expired" -> "EXPIRED";
            case "sent", "accepted" -> "SENT";
            case "pending", "waiting" -> "PENDING";
            default -> status.toUpperCase();
        };
    }

    /**
     * Mask phone number for logging.
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) {
            return "***";
        }
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }
}
