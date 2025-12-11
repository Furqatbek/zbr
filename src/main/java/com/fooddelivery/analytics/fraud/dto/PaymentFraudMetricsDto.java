package com.fooddelivery.analytics.fraud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for payment fraud metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFraudMetricsDto {

    // Volume metrics
    private Long totalPaymentAttempts;
    private Long successfulPayments;
    private Long failedPayments;
    private Long declinedPayments;

    // Failure rates
    private Double failedPaymentRate;
    private Double declineRate;
    private Double timeoutRate;

    // User-level metrics
    private Long usersWithFailedPayments;
    private Long highRiskUsers;
    private Double avgFailedAttemptsPerUser;

    // Amount metrics
    private BigDecimal totalFailedAmount;
    private BigDecimal avgFailedAmount;
    private BigDecimal maxFailedAmount;

    // Failure breakdown
    private Map<String, Long> failuresByReason;
    private Map<String, Long> failuresByMethod;
    private Map<String, Double> failureRateByMethod;

    // High-risk indicators
    private List<UserPaymentRiskDto> highRiskUsersList;
    private List<PaymentSpikeDto> failedPaymentSpikes;

    // Card-level metrics
    private Long uniqueCardsUsed;
    private Long cardsWithMultipleFailures;
    private Map<String, Long> failuresByCardBin;

    // Time-based analysis
    private List<HourlyPaymentMetricsDto> hourlyDistribution;

    // Thresholds used
    private Integer failedAttemptThreshold;
    private Integer timeWindowMinutes;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserPaymentRiskDto {
        private Long userId;
        private Long failedAttempts;
        private Long totalAttempts;
        private Double failureRate;
        private BigDecimal totalFailedAmount;
        private String lastFailureReason;
        private LocalDateTime lastFailedAt;
        private Integer riskScore;
        private List<String> riskFlags;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSpikeDto {
        private LocalDateTime spikeStart;
        private LocalDateTime spikeEnd;
        private Long failedCount;
        private Long normalBaseline;
        private Double deviationFactor;
        private List<Long> affectedUserIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyPaymentMetricsDto {
        private Integer hour;
        private Long totalAttempts;
        private Long failedAttempts;
        private Double failureRate;
    }
}
