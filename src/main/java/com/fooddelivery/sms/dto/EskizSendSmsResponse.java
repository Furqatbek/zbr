package com.fooddelivery.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for sending SMS via Eskiz.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EskizSendSmsResponse {

    /**
     * SMS ID assigned by Eskiz.
     */
    private String id;

    /**
     * Response message from API.
     */
    private String message;

    /**
     * Status of the SMS (e.g., "waiting", "success", "failed").
     */
    private String status;

    /**
     * Status code from the API.
     */
    @JsonProperty("status_code")
    private Integer statusCode;
}
