package com.fooddelivery.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Callback request DTO for Eskiz delivery status webhook.
 * Eskiz sends this payload when SMS delivery status changes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EskizCallbackRequest {

    /**
     * Message ID from the original send request.
     */
    @JsonProperty("message_id")
    private String messageId;

    /**
     * User SMS ID (custom ID provided during send).
     */
    @JsonProperty("user_sms_id")
    private String userSmsId;

    /**
     * Phone number the SMS was sent to.
     */
    @JsonProperty("mobile_phone")
    private String mobilePhone;

    /**
     * Delivery status.
     */
    private String status;

    /**
     * Status code.
     */
    @JsonProperty("status_code")
    private Integer statusCode;

    /**
     * Delivery timestamp.
     */
    @JsonProperty("delivery_time")
    private String deliveryTime;

    /**
     * Error description if failed.
     */
    private String error;
}
