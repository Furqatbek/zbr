package com.fooddelivery.analytics.fraud.mapper;

import com.fooddelivery.analytics.fraud.dto.*;
import com.fooddelivery.analytics.fraud.util.FraudConstants;
import com.fooddelivery.analytics.fraud.util.FraudRiskCalculator;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * MapStruct mapper for creating fraud summary from individual metrics.
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class FraudSummaryMapper {

    @Autowired
    protected FraudRiskCalculator riskCalculator;

    /**
     * Build complete fraud summary from individual metric components.
     */
    public FraudSummaryDto buildFraudSummary(
            PaymentFraudMetricsDto paymentMetrics,
            BehavioralFraudMetricsDto behavioralMetrics,
            AccountIntegrityMetricsDto accountMetrics,
            ReferralFraudMetricsDto referralMetrics,
            SecurityLogMetricsDto securityMetrics,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        // Calculate category risk scores
        int paymentRisk = calculatePaymentRiskScore(paymentMetrics);
        int behavioralRisk = calculateBehavioralRiskScore(behavioralMetrics);
        int accountRisk = calculateAccountRiskScore(accountMetrics);
        int referralRisk = calculateReferralRiskScore(referralMetrics);
        int securityRisk = calculateSecurityRiskScore(securityMetrics);

        // Calculate overall risk
        int overallRisk = riskCalculator.calculateOverallRiskScore(
                paymentRisk, behavioralRisk, accountRisk, referralRisk, securityRisk);

        // Build category summaries
        List<FraudSummaryDto.FraudCategorySummaryDto> categorySummaries = buildCategorySummaries(
                paymentRisk, behavioralRisk, accountRisk, referralRisk, securityRisk,
                paymentMetrics, behavioralMetrics, accountMetrics, referralMetrics, securityMetrics);

        // Calculate financial impact
        FraudSummaryDto.FinancialImpactDto financialImpact = calculateFinancialImpact(
                paymentMetrics, behavioralMetrics, referralMetrics);

        // Build trend analysis
        FraudSummaryDto.FraudTrendDto trendAnalysis = buildTrendAnalysis(
                paymentMetrics, behavioralMetrics, securityMetrics);

        // Extract top threats
        List<FraudSummaryDto.TopThreatDto> topThreats = extractTopThreats(
                paymentMetrics, behavioralMetrics, accountMetrics, referralMetrics, securityMetrics);

        // Build alerts
        List<FraudSummaryDto.FraudAlertDto> alerts = generateAlerts(
                paymentRisk, behavioralRisk, accountRisk, referralRisk, securityRisk,
                paymentMetrics, securityMetrics);

        return FraudSummaryDto.builder()
                .overallRiskScore(overallRisk)
                .riskLevel(riskCalculator.getRiskLevel(overallRisk))
                .totalFlaggedUsers(calculateTotalFlaggedUsers(
                        paymentMetrics, behavioralMetrics, accountMetrics))
                .totalSecurityIncidents(countSecurityIncidents(securityMetrics))
                .financialImpact(financialImpact)
                .categorySummaries(categorySummaries)
                .trendAnalysis(trendAnalysis)
                .topThreats(topThreats)
                .activeAlerts(alerts)
                .periodStart(startDate)
                .periodEnd(endDate)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private int calculatePaymentRiskScore(PaymentFraudMetricsDto metrics) {
        if (metrics == null) return 0;

        double score = 0;

        // Failed payment rate (max 40 points)
        if (metrics.getFailedPaymentRate() != null) {
            score += Math.min(metrics.getFailedPaymentRate() * 2, 40);
        }

        // High risk users (max 30 points)
        if (metrics.getHighRiskUsers() != null && metrics.getHighRiskUsers() > 0) {
            score += Math.min(metrics.getHighRiskUsers() * 2, 30);
        }

        // Decline rate (max 30 points)
        if (metrics.getDeclineRate() != null) {
            score += Math.min(metrics.getDeclineRate(), 30);
        }

        return Math.min((int) score, 100);
    }

    private int calculateBehavioralRiskScore(BehavioralFraudMetricsDto metrics) {
        if (metrics == null) return 0;

        double score = 0;

        // Velocity violations (max 30 points)
        if (metrics.getVelocityMetrics() != null &&
                metrics.getVelocityMetrics().getVelocityViolationCount() != null) {
            score += Math.min(metrics.getVelocityMetrics().getVelocityViolationCount() * 3, 30);
        }

        // Refund abuse (max 35 points)
        if (metrics.getRefundMetrics() != null &&
                metrics.getRefundMetrics().getHighRefundUsers() != null) {
            score += Math.min(metrics.getRefundMetrics().getHighRefundUsers() * 5, 35);
        }

        // Order value anomalies (max 35 points)
        if (metrics.getOrderValueMetrics() != null &&
                metrics.getOrderValueMetrics().getAnomalousOrderCount() != null) {
            score += Math.min(metrics.getOrderValueMetrics().getAnomalousOrderCount() * 2, 35);
        }

        return Math.min((int) score, 100);
    }

    private int calculateAccountRiskScore(AccountIntegrityMetricsDto metrics) {
        if (metrics == null) return 0;

        // Use the integrity score directly (inverted)
        if (metrics.getAccountIntegrityScore() != null) {
            return 100 - metrics.getAccountIntegrityScore().intValue();
        }
        return 0;
    }

    private int calculateReferralRiskScore(ReferralFraudMetricsDto metrics) {
        if (metrics == null) return 0;

        double score = 0;

        // Overall fraud rate (max 50 points)
        if (metrics.getOverallReferralFraudRate() != null) {
            score += Math.min(metrics.getOverallReferralFraudRate() * 0.5, 50);
        }

        // Circular referrals (max 25 points)
        if (metrics.getCircularReferralMetrics() != null &&
                metrics.getCircularReferralMetrics().getCircularChainCount() != null) {
            score += Math.min(metrics.getCircularReferralMetrics().getCircularChainCount() * 10, 25);
        }

        // Promo abuse (max 25 points)
        if (metrics.getPromoAbuseMetrics() != null &&
                metrics.getPromoAbuseMetrics().getPromoOnlyUsersCount() != null) {
            score += Math.min(metrics.getPromoAbuseMetrics().getPromoOnlyUsersCount() * 2, 25);
        }

        return Math.min((int) score, 100);
    }

    private int calculateSecurityRiskScore(SecurityLogMetricsDto metrics) {
        if (metrics == null) return 0;

        double score = 0;

        // Failed login rate (max 30 points)
        if (metrics.getLoginMetrics() != null &&
                metrics.getLoginMetrics().getFailedLoginRate() != null) {
            score += Math.min(metrics.getLoginMetrics().getFailedLoginRate(), 30);
        }

        // Suspicious IPs (max 35 points)
        if (metrics.getIpSecurityMetrics() != null &&
                metrics.getIpSecurityMetrics().getSuspiciousIpCount() != null) {
            score += Math.min(metrics.getIpSecurityMetrics().getSuspiciousIpCount() * 5, 35);
        }

        // Brute force attempts (max 35 points)
        if (metrics.getBruteForceMetrics() != null &&
                metrics.getBruteForceMetrics().getDetectedAttacks() != null) {
            score += Math.min(metrics.getBruteForceMetrics().getDetectedAttacks() * 15, 35);
        }

        return Math.min((int) score, 100);
    }

    private List<FraudSummaryDto.FraudCategorySummaryDto> buildCategorySummaries(
            int paymentRisk, int behavioralRisk, int accountRisk, int referralRisk, int securityRisk,
            PaymentFraudMetricsDto payment, BehavioralFraudMetricsDto behavioral,
            AccountIntegrityMetricsDto account, ReferralFraudMetricsDto referral,
            SecurityLogMetricsDto security) {

        List<FraudSummaryDto.FraudCategorySummaryDto> summaries = new ArrayList<>();

        // Payment category
        summaries.add(FraudSummaryDto.FraudCategorySummaryDto.builder()
                .category(FraudConstants.CATEGORY_PAYMENT)
                .riskScore(paymentRisk)
                .riskLevel(riskCalculator.getRiskLevel(paymentRisk))
                .incidentCount(payment != null && payment.getHighRiskUsers() != null ?
                        payment.getHighRiskUsers() : 0L)
                .topIndicators(extractPaymentIndicators(payment))
                .build());

        // Behavioral category
        summaries.add(FraudSummaryDto.FraudCategorySummaryDto.builder()
                .category(FraudConstants.CATEGORY_BEHAVIORAL)
                .riskScore(behavioralRisk)
                .riskLevel(riskCalculator.getRiskLevel(behavioralRisk))
                .incidentCount(countBehavioralIncidents(behavioral))
                .topIndicators(extractBehavioralIndicators(behavioral))
                .build());

        // Account category
        summaries.add(FraudSummaryDto.FraudCategorySummaryDto.builder()
                .category(FraudConstants.CATEGORY_ACCOUNT)
                .riskScore(accountRisk)
                .riskLevel(riskCalculator.getRiskLevel(accountRisk))
                .incidentCount(account != null && account.getTotalFlaggedAccounts() != null ?
                        account.getTotalFlaggedAccounts() : 0L)
                .topIndicators(extractAccountIndicators(account))
                .build());

        // Referral category
        summaries.add(FraudSummaryDto.FraudCategorySummaryDto.builder()
                .category(FraudConstants.CATEGORY_REFERRAL)
                .riskScore(referralRisk)
                .riskLevel(riskCalculator.getRiskLevel(referralRisk))
                .incidentCount(referral != null && referral.getTotalFraudulentReferrals() != null ?
                        referral.getTotalFraudulentReferrals() : 0L)
                .topIndicators(extractReferralIndicators(referral))
                .build());

        // Security category
        summaries.add(FraudSummaryDto.FraudCategorySummaryDto.builder()
                .category(FraudConstants.CATEGORY_SECURITY)
                .riskScore(securityRisk)
                .riskLevel(riskCalculator.getRiskLevel(securityRisk))
                .incidentCount(countSecurityIncidents(security))
                .topIndicators(extractSecurityIndicators(security))
                .build());

        return summaries;
    }

    private FraudSummaryDto.FinancialImpactDto calculateFinancialImpact(
            PaymentFraudMetricsDto payment,
            BehavioralFraudMetricsDto behavioral,
            ReferralFraudMetricsDto referral) {

        BigDecimal failedAmount = payment != null && payment.getTotalFailedAmount() != null ?
                payment.getTotalFailedAmount() : BigDecimal.ZERO;

        BigDecimal refundAmount = BigDecimal.ZERO;
        if (behavioral != null && behavioral.getRefundMetrics() != null &&
                behavioral.getRefundMetrics().getTotalRefundAmount() != null) {
            refundAmount = behavioral.getRefundMetrics().getTotalRefundAmount();
        }

        BigDecimal promoLoss = BigDecimal.ZERO;
        if (referral != null && referral.getPromoAbuseMetrics() != null &&
                referral.getPromoAbuseMetrics().getEstimatedPromoLoss() != null) {
            promoLoss = referral.getPromoAbuseMetrics().getEstimatedPromoLoss();
        }

        BigDecimal total = failedAmount.add(refundAmount).add(promoLoss);

        return FraudSummaryDto.FinancialImpactDto.builder()
                .totalEstimatedLoss(total)
                .failedPaymentLoss(failedAmount)
                .refundAbuseLoss(refundAmount)
                .promoAbuseLoss(promoLoss)
                .referralFraudLoss(BigDecimal.ZERO)
                .build();
    }

    private FraudSummaryDto.FraudTrendDto buildTrendAnalysis(
            PaymentFraudMetricsDto payment,
            BehavioralFraudMetricsDto behavioral,
            SecurityLogMetricsDto security) {

        // Simplified trend - in production, compare with previous period
        return FraudSummaryDto.FraudTrendDto.builder()
                .trendDirection("STABLE")
                .percentageChange(0.0)
                .comparisonPeriod("Previous period")
                .paymentTrend("STABLE")
                .behavioralTrend("STABLE")
                .securityTrend("STABLE")
                .build();
    }

    private List<FraudSummaryDto.TopThreatDto> extractTopThreats(
            PaymentFraudMetricsDto payment,
            BehavioralFraudMetricsDto behavioral,
            AccountIntegrityMetricsDto account,
            ReferralFraudMetricsDto referral,
            SecurityLogMetricsDto security) {

        List<FraudSummaryDto.TopThreatDto> threats = new ArrayList<>();

        // Add payment threats
        if (payment != null && payment.getHighRiskUsersList() != null) {
            payment.getHighRiskUsersList().stream()
                    .limit(2)
                    .forEach(user -> threats.add(FraudSummaryDto.TopThreatDto.builder()
                            .threatType("PAYMENT_FRAUD")
                            .description("High payment failure rate for user " + user.getUserId())
                            .riskScore(user.getRiskScore() != null ? user.getRiskScore() : 0)
                            .affectedEntity("User:" + user.getUserId())
                            .category(FraudConstants.CATEGORY_PAYMENT)
                            .build()));
        }

        // Add behavioral threats
        if (behavioral != null && behavioral.getRefundMetrics() != null &&
                behavioral.getRefundMetrics().getRefundAbusersList() != null) {
            behavioral.getRefundMetrics().getRefundAbusersList().stream()
                    .limit(2)
                    .forEach(user -> threats.add(FraudSummaryDto.TopThreatDto.builder()
                            .threatType("REFUND_ABUSE")
                            .description("High refund rate detected for user " + user.getUserId())
                            .riskScore(user.getRiskScore() != null ? user.getRiskScore() : 0)
                            .affectedEntity("User:" + user.getUserId())
                            .category(FraudConstants.CATEGORY_BEHAVIORAL)
                            .build()));
        }

        // Add security threats
        if (security != null && security.getIpSecurityMetrics() != null &&
                security.getIpSecurityMetrics().getSuspiciousIpsList() != null) {
            security.getIpSecurityMetrics().getSuspiciousIpsList().stream()
                    .limit(2)
                    .forEach(ip -> threats.add(FraudSummaryDto.TopThreatDto.builder()
                            .threatType("SUSPICIOUS_IP")
                            .description("Suspicious activity from IP " + ip.getIpAddress())
                            .riskScore(ip.getThreatScore() != null ? ip.getThreatScore() : 0)
                            .affectedEntity("IP:" + ip.getIpAddress())
                            .category(FraudConstants.CATEGORY_SECURITY)
                            .build()));
        }

        // Sort by risk score and limit
        threats.sort((a, b) -> Integer.compare(b.getRiskScore(), a.getRiskScore()));
        return threats.stream().limit(FraudConstants.DEFAULT_TOP_THREATS_SIZE).toList();
    }

    private List<FraudSummaryDto.FraudAlertDto> generateAlerts(
            int paymentRisk, int behavioralRisk, int accountRisk, int referralRisk, int securityRisk,
            PaymentFraudMetricsDto payment, SecurityLogMetricsDto security) {

        List<FraudSummaryDto.FraudAlertDto> alerts = new ArrayList<>();

        // High risk category alerts
        if (paymentRisk > FraudConstants.HIGH_RISK_THRESHOLD) {
            alerts.add(createAlert("CRITICAL", "Payment fraud risk is critical",
                    FraudConstants.CATEGORY_PAYMENT, "Payment fraud indicators exceed safe thresholds"));
        }

        if (securityRisk > FraudConstants.HIGH_RISK_THRESHOLD) {
            alerts.add(createAlert("CRITICAL", "Security risk is critical",
                    FraudConstants.CATEGORY_SECURITY, "Multiple security incidents detected"));
        }

        if (behavioralRisk > FraudConstants.MEDIUM_RISK_THRESHOLD) {
            alerts.add(createAlert("HIGH", "Elevated behavioral fraud",
                    FraudConstants.CATEGORY_BEHAVIORAL, "Abnormal user behavior patterns detected"));
        }

        // Specific condition alerts
        if (security != null && security.getBruteForceMetrics() != null &&
                security.getBruteForceMetrics().getDetectedAttacks() != null &&
                security.getBruteForceMetrics().getDetectedAttacks() > 0) {
            alerts.add(createAlert("CRITICAL", "Brute force attacks detected",
                    FraudConstants.CATEGORY_SECURITY, "Active credential stuffing or brute force attempts"));
        }

        return alerts;
    }

    private FraudSummaryDto.FraudAlertDto createAlert(String severity, String title, String category, String description) {
        return FraudSummaryDto.FraudAlertDto.builder()
                .alertId(UUID.randomUUID().toString())
                .severity(severity)
                .title(title)
                .description(description)
                .category(category)
                .createdAt(LocalDateTime.now())
                .acknowledged(false)
                .build();
    }

    private Long calculateTotalFlaggedUsers(PaymentFraudMetricsDto payment,
                                            BehavioralFraudMetricsDto behavioral,
                                            AccountIntegrityMetricsDto account) {
        long total = 0;
        if (payment != null && payment.getHighRiskUsers() != null) {
            total += payment.getHighRiskUsers();
        }
        if (behavioral != null && behavioral.getRefundMetrics() != null &&
                behavioral.getRefundMetrics().getHighRefundUsers() != null) {
            total += behavioral.getRefundMetrics().getHighRefundUsers();
        }
        if (account != null && account.getTotalFlaggedAccounts() != null) {
            total += account.getTotalFlaggedAccounts();
        }
        return total;
    }

    private Long countSecurityIncidents(SecurityLogMetricsDto security) {
        if (security == null) return 0L;

        long total = 0;
        if (security.getLoginMetrics() != null && security.getLoginMetrics().getFailedLogins() != null) {
            total += security.getLoginMetrics().getFailedLogins();
        }
        if (security.getRateLimitMetrics() != null &&
                security.getRateLimitMetrics().getTotalRateLimitViolations() != null) {
            total += security.getRateLimitMetrics().getTotalRateLimitViolations();
        }
        return total;
    }

    private Long countBehavioralIncidents(BehavioralFraudMetricsDto behavioral) {
        if (behavioral == null) return 0L;

        long total = 0;
        if (behavioral.getVelocityMetrics() != null &&
                behavioral.getVelocityMetrics().getVelocityViolationCount() != null) {
            total += behavioral.getVelocityMetrics().getVelocityViolationCount();
        }
        if (behavioral.getOrderValueMetrics() != null &&
                behavioral.getOrderValueMetrics().getAnomalousOrderCount() != null) {
            total += behavioral.getOrderValueMetrics().getAnomalousOrderCount();
        }
        return total;
    }

    private List<String> extractPaymentIndicators(PaymentFraudMetricsDto metrics) {
        List<String> indicators = new ArrayList<>();
        if (metrics == null) return indicators;

        if (metrics.getFailedPaymentRate() != null && metrics.getFailedPaymentRate() > 10) {
            indicators.add(FraudConstants.INDICATOR_HIGH_FAILURE_RATE);
        }
        if (metrics.getHighRiskUsers() != null && metrics.getHighRiskUsers() > 0) {
            indicators.add(FraudConstants.INDICATOR_HIGH_FAILURE_COUNT);
        }
        return indicators;
    }

    private List<String> extractBehavioralIndicators(BehavioralFraudMetricsDto metrics) {
        List<String> indicators = new ArrayList<>();
        if (metrics == null) return indicators;

        if (metrics.getVelocityMetrics() != null &&
                metrics.getVelocityMetrics().getVelocityViolationCount() != null &&
                metrics.getVelocityMetrics().getVelocityViolationCount() > 0) {
            indicators.add(FraudConstants.INDICATOR_VELOCITY_VIOLATION);
        }
        if (metrics.getRefundMetrics() != null &&
                metrics.getRefundMetrics().getHighRefundUsers() != null &&
                metrics.getRefundMetrics().getHighRefundUsers() > 0) {
            indicators.add(FraudConstants.INDICATOR_REFUND_ABUSE);
        }
        return indicators;
    }

    private List<String> extractAccountIndicators(AccountIntegrityMetricsDto metrics) {
        List<String> indicators = new ArrayList<>();
        if (metrics == null) return indicators;

        if (metrics.getFakeAccounts() != null &&
                metrics.getFakeAccounts().getSuspectedFakeAccounts() != null &&
                metrics.getFakeAccounts().getSuspectedFakeAccounts() > 0) {
            indicators.add(FraudConstants.INDICATOR_FAKE_ACCOUNT);
        }
        if (metrics.getMultiAccounts() != null &&
                metrics.getMultiAccounts().getSuspectedMultiAccountUsers() != null &&
                metrics.getMultiAccounts().getSuspectedMultiAccountUsers() > 0) {
            indicators.add(FraudConstants.INDICATOR_MULTI_ACCOUNT);
        }
        return indicators;
    }

    private List<String> extractReferralIndicators(ReferralFraudMetricsDto metrics) {
        List<String> indicators = new ArrayList<>();
        if (metrics == null) return indicators;

        if (metrics.getCircularReferralMetrics() != null &&
                metrics.getCircularReferralMetrics().getCircularChainCount() != null &&
                metrics.getCircularReferralMetrics().getCircularChainCount() > 0) {
            indicators.add(FraudConstants.INDICATOR_CIRCULAR_REFERRAL);
        }
        if (metrics.getPromoAbuseMetrics() != null &&
                metrics.getPromoAbuseMetrics().getPromoOnlyUsersCount() != null &&
                metrics.getPromoAbuseMetrics().getPromoOnlyUsersCount() > 0) {
            indicators.add(FraudConstants.INDICATOR_PROMO_ABUSE);
        }
        return indicators;
    }

    private List<String> extractSecurityIndicators(SecurityLogMetricsDto metrics) {
        List<String> indicators = new ArrayList<>();
        if (metrics == null) return indicators;

        if (metrics.getBruteForceMetrics() != null &&
                metrics.getBruteForceMetrics().getDetectedAttacks() != null &&
                metrics.getBruteForceMetrics().getDetectedAttacks() > 0) {
            indicators.add(FraudConstants.INDICATOR_BRUTE_FORCE);
        }
        if (metrics.getIpSecurityMetrics() != null &&
                metrics.getIpSecurityMetrics().getSuspiciousIpCount() != null &&
                metrics.getIpSecurityMetrics().getSuspiciousIpCount() > 0) {
            indicators.add(FraudConstants.INDICATOR_SUSPICIOUS_IP);
        }
        return indicators;
    }
}
