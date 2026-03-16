package com.fooddelivery.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Callback request DTO for DevSMS delivery status webhook.
 * DevSMS sends this payload when SMS delivery status changes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevSmsCallbackRequest {

    /**
     * SMS ID from the original send request.
     */
    @JsonProperty("sms_id")
    private Long smsId;

    /**
     * Request ID from the original send request.
     */
    @JsonProperty("request_id")
    private String requestId;

    /**
     * Phone number the SMS was sent to.
     */
    private String phone;

    /**
     * Delivery status: sent, delivered, failed, pending, expired.
     */
    private String status;

    /**
     * Error code if delivery failed.
     */
    @JsonProperty("error_code")
    private String errorCode;

    /**
     * Error message if delivery failed.
     */
    @JsonProperty("error_message")
    private String errorMessage;

    /**
     * Timestamp when status was updated.
     */
    private String timestamp;
}
