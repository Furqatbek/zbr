package com.fooddelivery.platform.dto;

import com.fooddelivery.platform.entity.ReferralStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for referral information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Referral information")
public class ReferralDto {

    @Schema(description = "Referral ID")
    private Long id;

    @Schema(description = "Referral code")
    private String code;

    @Schema(description = "Referrer user ID")
    private Long referrerId;

    @Schema(description = "Referrer name")
    private String referrerName;

    @Schema(description = "Referred user ID")
    private Long referredId;

    @Schema(description = "Referred user name")
    private String referredName;

    @Schema(description = "Referral status")
    private ReferralStatus status;

    @Schema(description = "Reward amount")
    private BigDecimal rewardAmount;

    @Schema(description = "Currency")
    private String currency;

    @Schema(description = "Referrer rewarded")
    private Boolean referrerRewarded;

    @Schema(description = "Referred user rewarded")
    private Boolean referredRewarded;

    @Schema(description = "Expiration date")
    private LocalDateTime expiresAt;

    @Schema(description = "Completion date")
    private LocalDateTime completedAt;

    @Schema(description = "Created at")
    private LocalDateTime createdAt;
}
