package com.fooddelivery.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for DevSMS API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevSmsSendResponse {

    /**
     * Whether the request was successful.
     */
    private boolean success;

    /**
     * Response data (present on success).
     */
    private DevSmsData data;

    /**
     * Error message (present on failure).
     */
    private String error;

    /**
     * Data object returned on successful SMS send.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DevSmsData {

        @JsonProperty("sms_id")
        private Long smsId;

        @JsonProperty("request_id")
        private String requestId;

        private String status;

        private Double balance;
    }

    /**
     * Convert to common SmsSendResponse.
     */
    public SmsSendResponse toSmsSendResponse() {
        if (success && data != null) {
            return SmsSendResponse.builder()
                    .success(true)
                    .smsId(data.getSmsId() != null ? data.getSmsId().toString() : null)
                    .requestId(data.getRequestId())
                    .status(data.getStatus())
                    .balance(data.getBalance())
                    .provider("DEVSMS")
                    .build();
        } else {
            return SmsSendResponse.builder()
                    .success(false)
                    .message(error)
                    .provider("DEVSMS")
                    .build();
        }
    }
}
