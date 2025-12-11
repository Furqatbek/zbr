package com.fooddelivery.analytics.fraud.service;

import com.fooddelivery.analytics.fraud.dto.*;

import java.time.LocalDateTime;

/**
 * Service interface for fraud analytics and detection.
 * Provides comprehensive fraud metrics across multiple categories:
 * - Payment Fraud
 * - Behavioral Fraud
 * - Account Integrity
 * - Referral Fraud
 * - Security Logs
 */
public interface FraudAnalyticsService {

    /**
     * Get comprehensive fraud summary with overall risk assessment.
     *
     * @param request Request with date range and thresholds
     * @return Complete fraud summary with all categories
     */
    FraudSummaryDto getFraudSummary(FraudMetricsRequest request);

    /**
     * Get payment fraud metrics including failed payments, card testing, and high-risk users.
     *
     * @param startDate Start of analysis period
     * @param endDate End of analysis period
     * @param failedThreshold Threshold for failed payments per user
     * @param timeWindowMinutes Time window for velocity checks
     * @param includeDetails Whether to include detailed user lists
     * @param maxListSize Maximum size for detailed lists
     * @return Payment fraud metrics
     */
    PaymentFraudMetricsDto getPaymentFraudMetrics(
            LocalDateTime startDate,
            LocalDateTime endDate,
            int failedThreshold,
            int timeWindowMinutes,
            boolean includeDetails,
            int maxListSize);

    /**
     * Get behavioral fraud metrics including velocity, refund abuse, and order anomalies.
     *
     * @param startDate Start of analysis period
     * @param endDate End of analysis period
     * @param velocityThreshold Maximum orders per hour threshold
     * @param refundRateThreshold High refund rate threshold
     * @param orderValueZScore Z-score threshold for order value anomalies
     * @param addressUserThreshold Threshold for users per address
     * @param includeDetails Whether to include detailed lists
     * @param maxListSize Maximum size for detailed lists
     * @return Behavioral fraud metrics
     */
    BehavioralFraudMetricsDto getBehavioralFraudMetrics(
            LocalDateTime startDate,
            LocalDateTime endDate,
            int velocityThreshold,
            double refundRateThreshold,
            double orderValueZScore,
            int addressUserThreshold,
            boolean includeDetails,
            int maxListSize);

    /**
     * Get account integrity metrics including fake accounts and multi-account detection.
     *
     * @param startDate Start of analysis period
     * @param endDate End of analysis period
     * @param fakeAccountAgeThreshold Account age threshold in hours
     * @param deviceThreshold Threshold for users per device
     * @param ipThreshold Threshold for users per IP
     * @param includeDetails Whether to include detailed lists
     * @param maxListSize Maximum size for detailed lists
     * @return Account integrity metrics
     */
    AccountIntegrityMetricsDto getAccountIntegrityMetrics(
            LocalDateTime startDate,
            LocalDateTime endDate,
            int fakeAccountAgeThreshold,
            int deviceThreshold,
            int ipThreshold,
            boolean includeDetails,
            int maxListSize);

    /**
     * Get referral fraud metrics including device/IP reuse and circular referrals.
     *
     * @param startDate Start of analysis period
     * @param endDate End of analysis period
     * @param deviceSignupThreshold Threshold for signups per device
     * @param ipSignupThreshold Threshold for signups per IP
     * @param promoAbuseRate Threshold for promo abuse rate
     * @param includeDetails Whether to include detailed lists
     * @param maxListSize Maximum size for detailed lists
     * @return Referral fraud metrics
     */
    ReferralFraudMetricsDto getReferralFraudMetrics(
            LocalDateTime startDate,
            LocalDateTime endDate,
            int deviceSignupThreshold,
            int ipSignupThreshold,
            double promoAbuseRate,
            boolean includeDetails,
            int maxListSize);

    /**
     * Get security log metrics including login failures, rate limits, and IP threats.
     *
     * @param startDate Start of analysis period
     * @param endDate End of analysis period
     * @param failedLoginThreshold Threshold for failed logins
     * @param loginTimeWindowMinutes Time window for login velocity
     * @param rateLimitThreshold Rate limit violation threshold
     * @param includeDetails Whether to include detailed lists
     * @param maxListSize Maximum size for detailed lists
     * @return Security log metrics
     */
    SecurityLogMetricsDto getSecurityLogMetrics(
            LocalDateTime startDate,
            LocalDateTime endDate,
            int failedLoginThreshold,
            int loginTimeWindowMinutes,
            int rateLimitThreshold,
            boolean includeDetails,
            int maxListSize);

    /**
     * Get real-time fraud alerts based on configurable thresholds.
     *
     * @param request Request with thresholds
     * @return Fraud summary with active alerts
     */
    FraudSummaryDto getRealTimeAlerts(FraudMetricsRequest request);

    /**
     * Calculate overall risk score for a specific user.
     *
     * @param userId User ID to analyze
     * @param startDate Start of analysis period
     * @param endDate End of analysis period
     * @return User-specific risk assessment
     */
    UserFraudRiskDto getUserRiskScore(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Invalidate cached fraud metrics.
     * Call this after significant data changes or threshold updates.
     */
    void invalidateCache();

    /**
     * Invalidate specific category cache.
     *
     * @param category Category name (PAYMENT, BEHAVIORAL, ACCOUNT, REFERRAL, SECURITY)
     */
    void invalidateCategoryCache(String category);
}
