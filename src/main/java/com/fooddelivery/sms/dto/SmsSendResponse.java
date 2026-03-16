package com.fooddelivery.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Common response DTO for SMS send operations.
 * Abstracts responses from different SMS providers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsSendResponse {

    /**
     * Whether the SMS was sent successfully.
     */
    private boolean success;

    /**
     * SMS ID from the provider.
     */
    private String smsId;

    /**
     * Request/message ID.
     */
    private String requestId;

    /**
     * Status of the SMS (sent, pending, delivered, failed).
     */
    private String status;

    /**
     * Human-readable message.
     */
    private String message;

    /**
     * Provider name that handled the request.
     */
    private String provider;

    /**
     * Remaining balance (if provided by the provider).
     */
    private Double balance;

    /**
     * Error code (if failed).
     */
    private String errorCode;

    /**
     * Create a success response.
     */
    public static SmsSendResponse success(String smsId, String status, String provider) {
        return SmsSendResponse.builder()
                .success(true)
                .smsId(smsId)
                .status(status)
                .provider(provider)
                .build();
    }

    /**
     * Create a failure response.
     */
    public static SmsSendResponse failure(String errorMessage, String errorCode, String provider) {
        return SmsSendResponse.builder()
                .success(false)
                .message(errorMessage)
                .errorCode(errorCode)
                .provider(provider)
                .build();
    }
}
