package com.fooddelivery.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Eskiz authentication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EskizAuthRequest {

    private String email;
    private String password;
}
