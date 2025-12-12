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
 * DTO for fraud summary with overall risk metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudSummaryDto {

    // Overall risk score (0-100)
    private Integer overallRiskScore;
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL

    // Key metrics summary
    private Long highRiskUsers;
    private Long failedPaymentSpikes;
    private Long promoAbuseAccounts;
    private Long multiAccountDevices;
    private Long vpnSuspiciousIPs;
    private Long abnormalOrderPatterns;
    private Long bruteForceAttempts;
    private Long circularReferrals;

    // Totals used by mapper
    private Long totalFlaggedUsers;
    private Long totalSecurityIncidents;

    // Financial impact
    private FinancialImpactDto financialImpact;

    // Category-specific summaries
    private CategorySummaryDto paymentFraudSummary;
    private CategorySummaryDto behavioralFraudSummary;
    private CategorySummaryDto accountFraudSummary;
    private CategorySummaryDto referralFraudSummary;
    private CategorySummaryDto securitySummary;

    // Used by FraudSummaryMapper
    private List<FraudCategorySummaryDto> categorySummaries;
    private FraudTrendDto trendAnalysis;
    private List<FraudAlertDto> activeAlerts;

    // Active security flags
    private Long activeSecurityFlags;
    private Map<String, Long> flagsBySeverity;
    private Map<String, Long> flagsByType;

    // Trends
    private TrendDataDto weekOverWeekTrend;
    private List<DailyFraudTrendDto> dailyTrend;

    // Top threats
    private List<TopThreatDto> topThreats;

    // Recommendations
    private List<String> recommendations;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialImpactDto {
        private BigDecimal estimatedFraudLoss;
        private BigDecimal preventedFraudAmount;
        private BigDecimal suspiciousTransactionVolume;
        private BigDecimal promoAbuseAmount;
        private BigDecimal chargebackAmount;
        private BigDecimal refundAbuseAmount;
        // Additional fields used by FraudSummaryMapper
        private BigDecimal totalEstimatedLoss;
        private BigDecimal failedPaymentLoss;
        private BigDecimal refundAbuseLoss;
        private BigDecimal promoAbuseLoss;
        private BigDecimal referralFraudLoss;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummaryDto {
        private String category;
        private Integer riskScore;
        private String status; // HEALTHY, WARNING, CRITICAL
        private Long totalIncidents;
        private Long newIncidents;
        private Long resolvedIncidents;
        private Double changeFromPreviousPeriod;
        private List<String> topIssues;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDataDto {
        private Double percentageChange;
        private String direction; // UP, DOWN, STABLE
        private Long previousPeriodIncidents;
        private Long currentPeriodIncidents;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyFraudTrendDto {
        private LocalDateTime date;
        private Long paymentFraudCount;
        private Long behavioralFraudCount;
        private Long accountFraudCount;
        private Long referralFraudCount;
        private Long securityIncidents;
        private Integer dailyRiskScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopThreatDto {
        private String threatType;
        private String description;
        private String severity;
        private Long affectedEntities;
        private BigDecimal potentialImpact;
        private String recommendedAction;
        private Integer priority;
        // Additional fields used by FraudSummaryMapper
        private Integer riskScore;
        private String affectedEntity;
        private String category;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FraudCategorySummaryDto {
        private String category;
        private Integer riskScore;
        private String riskLevel;
        private Long incidentCount;
        private List<String> topIndicators;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FraudTrendDto {
        private String trendDirection;
        private Double percentageChange;
        private String comparisonPeriod;
        private String paymentTrend;
        private String behavioralTrend;
        private String securityTrend;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FraudAlertDto {
        private String alertId;
        private String severity;
        private String title;
        private String description;
        private String category;
        private LocalDateTime createdAt;
        private Boolean acknowledged;
    }
}
