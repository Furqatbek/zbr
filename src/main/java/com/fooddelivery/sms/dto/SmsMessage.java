package com.fooddelivery.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for SMS messages queued in RabbitMQ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique message ID for tracking.
     */
    private String messageId;

    /**
     * Recipient phone number.
     */
    private String phoneNumber;

    /**
     * SMS message content.
     */
    private String message;

    /**
     * SMS type for categorization.
     */
    private SmsType type;

    /**
     * Reference ID (e.g., order ID, user ID).
     */
    private String referenceId;

    /**
     * Reference type (e.g., "ORDER", "USER").
     */
    private String referenceType;

    /**
     * Number of retry attempts.
     */
    @Builder.Default
    private int retryCount = 0;

    /**
     * Timestamp when message was created.
     */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Priority level (1-10, higher is more urgent).
     */
    @Builder.Default
    private int priority = 5;

    /**
     * Types of SMS messages.
     */
    public enum SmsType {
        OTP,
        ORDER_CONFIRMATION,
        ORDER_STATUS_UPDATE,
        DELIVERY_UPDATE,
        PROMOTIONAL,
        WELCOME,
        PASSWORD_RESET,
        PAYMENT_CONFIRMATION,
        GENERAL
    }
}
