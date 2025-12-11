package com.fooddelivery.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Eskiz authentication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EskizAuthResponse {

    private String message;

    @JsonProperty("data")
    private TokenData data;

    @JsonProperty("token_type")
    private String tokenType;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenData {
        private String token;
    }
}
