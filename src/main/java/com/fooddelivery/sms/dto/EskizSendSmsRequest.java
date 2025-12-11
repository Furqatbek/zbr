package com.fooddelivery.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sending SMS via Eskiz.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EskizSendSmsRequest {

    /**
     * Recipient phone number (e.g., 998901234567).
     */
    @JsonProperty("mobile_phone")
    private String mobilePhone;

    /**
     * SMS message content.
     */
    private String message;

    /**
     * Sender name (alpha-name).
     */
    private String from;

    /**
     * Callback URL for delivery status.
     */
    @JsonProperty("callback_url")
    private String callbackUrl;

    /**
     * Unique user SMS ID for tracking.
     */
    @JsonProperty("user_sms_id")
    private String userSmsId;
}
