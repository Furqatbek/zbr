package com.fooddelivery.analytics.fraud.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fooddelivery.common.util.FlexibleLocalDateTimeDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for fraud metrics with configurable thresholds.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudMetricsRequest {

    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime startDate;

    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime endDate;

    // Payment fraud thresholds
    @Builder.Default
    private Integer failedPaymentThreshold = 3;
    @Builder.Default
    private Integer paymentTimeWindowMinutes = 60;

    // Velocity thresholds
    @Builder.Default
    private Integer orderVelocityThresholdPerHour = 5;
    @Builder.Default
    private Integer velocityTimeWindowMinutes = 60;

    // Order value thresholds
    @Builder.Default
    private Double orderValueZScoreThreshold = 3.0;

    // Refund thresholds
    @Builder.Default
    private Double highRefundRateThreshold = 0.3; // 30%
    @Builder.Default
    private Integer minOrdersForRefundAnalysis = 5;

    // Account thresholds
    @Builder.Default
    private Integer fakeAccountAgeThresholdHours = 24;
    @Builder.Default
    private Integer multiAccountDeviceThreshold = 2;
    @Builder.Default
    private Integer multiAccountIpThreshold = 3;

    // Referral thresholds
    @Builder.Default
    private Integer signupsPerDeviceThreshold = 2;
    @Builder.Default
    private Integer signupsPerIpThreshold = 3;
    @Builder.Default
    private Integer minOrdersForPromoAbuse = 3;

    // Security thresholds
    @Builder.Default
    private Integer bruteForceThreshold = 10;
    @Builder.Default
    private Integer bruteForceTimeWindowMinutes = 5;
    @Builder.Default
    private Integer failedLoginThreshold = 5;

    // Address thresholds
    @Builder.Default
    private Integer addressMultiUserThreshold = 3;

    // Include flags
    @Builder.Default
    private Boolean includeDetailedLists = true;
    @Builder.Default
    private Boolean includeHourlyDistribution = false;
    @Builder.Default
    private Boolean includeTrends = false;
    @Builder.Default
    private Integer maxListSize = 100;

    /**
     * Create a default FraudMetricsRequest with date range.
     */
    public static FraudMetricsRequest createDefault(LocalDateTime startDate, LocalDateTime endDate) {
        return FraudMetricsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    // Alias getters for service compatibility
    public Integer getVelocityThreshold() {
        return orderVelocityThresholdPerHour;
    }

    public Double getRefundRateThreshold() {
        return highRefundRateThreshold;
    }

    public Integer getAddressUserThreshold() {
        return addressMultiUserThreshold;
    }

    public Integer getFakeAccountAgeThreshold() {
        return fakeAccountAgeThresholdHours;
    }

    public Integer getDeviceShareThreshold() {
        return multiAccountDeviceThreshold;
    }

    public Integer getIpShareThreshold() {
        return multiAccountIpThreshold;
    }

    public Integer getDeviceSignupThreshold() {
        return signupsPerDeviceThreshold;
    }

    public Integer getIpSignupThreshold() {
        return signupsPerIpThreshold;
    }

    public Double getPromoAbuseRateThreshold() {
        return 0.5; // Default 50%
    }

    public Integer getLoginTimeWindowMinutes() {
        return bruteForceTimeWindowMinutes;
    }

    public Integer getRateLimitThreshold() {
        return 100; // Default rate limit
    }
}
