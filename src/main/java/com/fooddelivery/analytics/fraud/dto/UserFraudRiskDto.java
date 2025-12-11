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
 * DTO for user-specific fraud risk assessment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFraudRiskDto {

    private Long userId;
    private Integer overallRiskScore;
    private String riskLevel;

    // Category-specific scores
    private Integer paymentRiskScore;
    private Integer behavioralRiskScore;
    private Integer accountRiskScore;
    private Integer referralRiskScore;
    private Integer securityRiskScore;

    // Risk indicators
    private List<String> riskIndicators;
    private List<RiskFactorDto> riskFactors;

    // Payment metrics
    private PaymentRiskSummaryDto paymentRisk;

    // Behavioral metrics
    private BehavioralRiskSummaryDto behavioralRisk;

    // Account metrics
    private AccountRiskSummaryDto accountRisk;

    // Security metrics
    private SecurityRiskSummaryDto securityRisk;

    // Historical data
    private List<RiskHistoryDto> riskHistory;

    // Recommendations
    private List<String> recommendedActions;

    // Metadata
    private LocalDateTime analyzedAt;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskFactorDto {
        private String factor;
        private String category;
        private Integer contribution;
        private String description;
        private String severity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentRiskSummaryDto {
        private Long totalPaymentAttempts;
        private Long failedPayments;
        private Double failureRate;
        private BigDecimal totalFailedAmount;
        private Long uniqueCardsUsed;
        private Boolean hasCardTestingPattern;
        private String lastFailureReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BehavioralRiskSummaryDto {
        private Long totalOrders;
        private Double ordersPerDay;
        private Long refundedOrders;
        private Double refundRate;
        private BigDecimal avgOrderValue;
        private Boolean hasVelocityViolation;
        private Boolean hasOrderValueAnomaly;
        private Integer uniqueDeliveryAddresses;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountRiskSummaryDto {
        private Integer accountAgeHours;
        private Boolean isNewAccount;
        private Integer devicesUsed;
        private Integer ipsUsed;
        private Boolean hasDeviceSharing;
        private Boolean usedPromoOnFirstOrder;
        private Integer referralsMade;
        private Integer suspiciousReferrals;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityRiskSummaryDto {
        private Long failedLoginAttempts;
        private Double loginFailureRate;
        private Boolean hasVpnUsage;
        private Boolean hasTorUsage;
        private Boolean hasRateLimitViolations;
        private Integer suspiciousIpsUsed;
        private LocalDateTime lastFailedLogin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskHistoryDto {
        private LocalDateTime date;
        private Integer riskScore;
        private String riskLevel;
        private Map<String, Integer> categoryScores;
    }
}
