package com.fooddelivery.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for applying a referral code.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Apply referral code request")
public class ApplyReferralRequest {

    @NotBlank(message = "Referral code is required")
    @Schema(description = "The referral code to apply")
    private String code;
}
