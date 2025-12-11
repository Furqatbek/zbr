package com.fooddelivery.analytics.fraud.util;

import org.springframework.stereotype.Component;

/**
 * Utility class for calculating fraud risk scores.
 * All scores are normalized to 0-100 scale where:
 * - 0-30: Low risk
 * - 31-60: Medium risk
 * - 61-80: High risk
 * - 81-100: Critical risk
 */
@Component
public class FraudRiskCalculator {

    // Risk thresholds
    private static final int LOW_RISK_MAX = 30;
    private static final int MEDIUM_RISK_MAX = 60;
    private static final int HIGH_RISK_MAX = 80;
    private static final int MAX_RISK = 100;

    // ============================================
    // Payment Fraud Risk Calculations
    // ============================================

    /**
     * Calculate payment risk score based on failed payments.
     * Formula: base_score + (failure_rate_penalty) + (volume_penalty)
     *
     * @param failedCount Number of failed payment attempts
     * @param failureRate Percentage of failed payments (0-100)
     * @param totalCount Total payment attempts
     * @return Risk score 0-100
     */
    public int calculatePaymentRiskScore(int failedCount, double failureRate, int totalCount) {
        if (totalCount == 0) return 0;

        double score = 0;

        // Base score from failure rate (max 40 points)
        score += Math.min(failureRate * 0.8, 40);

        // Volume penalty for high failure counts (max 30 points)
        if (failedCount >= 10) {
            score += 30;
        } else if (failedCount >= 5) {
            score += 20;
        } else if (failedCount >= 3) {
            score += 10;
        }

        // Pattern penalty for consistent failures (max 30 points)
        if (failureRate >= 80 && totalCount >= 5) {
            score += 30; // Almost all attempts failing
        } else if (failureRate >= 50 && totalCount >= 3) {
            score += 20;
        } else if (failureRate >= 30 && totalCount >= 2) {
            score += 10;
        }

        return normalizeScore(score);
    }

    /**
     * Calculate card testing risk score.
     * Detects rapid small-amount transactions typical of card testing.
     *
     * @param smallAmountCount Number of small transactions
     * @param rapidSuccession Whether transactions were in rapid succession
     * @param declineRate Decline rate for small amounts
     * @return Risk score 0-100
     */
    public int calculateCardTestingRiskScore(int smallAmountCount, boolean rapidSuccession, double declineRate) {
        double score = 0;

        // Small transaction volume (max 40 points)
        score += Math.min(smallAmountCount * 5, 40);

        // Rapid succession bonus (20 points)
        if (rapidSuccession) {
            score += 20;
        }

        // High decline rate bonus (max 40 points)
        score += Math.min(declineRate * 0.4, 40);

        return normalizeScore(score);
    }

    // ============================================
    // Behavioral Fraud Risk Calculations
    // ============================================

    /**
     * Calculate velocity risk score based on order frequency.
     *
     * @param ordersPerHour Orders placed per hour
     * @param ordersPerDay Orders placed per day
     * @param threshold Expected normal threshold
     * @return Risk score 0-100
     */
    public int calculateVelocityRiskScore(double ordersPerHour, double ordersPerDay, int threshold) {
        double score = 0;

        // Hourly velocity (max 50 points)
        if (ordersPerHour > threshold * 2) {
            score += 50;
        } else if (ordersPerHour > threshold) {
            score += 30;
        } else if (ordersPerHour > threshold * 0.5) {
            score += 15;
        }

        // Daily velocity (max 50 points)
        if (ordersPerDay > threshold * 10) {
            score += 50;
        } else if (ordersPerDay > threshold * 5) {
            score += 30;
        } else if (ordersPerDay > threshold * 2) {
            score += 15;
        }

        return normalizeScore(score);
    }

    /**
     * Calculate order value anomaly risk score using z-score.
     *
     * @param zScore Statistical z-score of order value
     * @param orderValue Actual order value
     * @param avgOrderValue Platform average order value
     * @return Risk score 0-100
     */
    public int calculateOrderValueRiskScore(double zScore, double orderValue, double avgOrderValue) {
        double score = 0;

        // Z-score based scoring (max 60 points)
        double absZScore = Math.abs(zScore);
        if (absZScore > 4) {
            score += 60;
        } else if (absZScore > 3) {
            score += 45;
        } else if (absZScore > 2) {
            score += 30;
        } else if (absZScore > 1.5) {
            score += 15;
        }

        // Value deviation from average (max 40 points)
        if (avgOrderValue > 0) {
            double deviationRatio = orderValue / avgOrderValue;
            if (deviationRatio > 5) {
                score += 40;
            } else if (deviationRatio > 3) {
                score += 25;
            } else if (deviationRatio > 2) {
                score += 15;
            }
        }

        return normalizeScore(score);
    }

    /**
     * Calculate refund abuse risk score.
     *
     * @param refundRate Percentage of orders refunded
     * @param refundCount Total number of refunds
     * @return Risk score 0-100
     */
    public int calculateRefundRiskScore(double refundRate, long refundCount) {
        double score = 0;

        // Refund rate scoring (max 50 points)
        if (refundRate >= 50) {
            score += 50;
        } else if (refundRate >= 30) {
            score += 35;
        } else if (refundRate >= 20) {
            score += 20;
        } else if (refundRate >= 10) {
            score += 10;
        }

        // Volume scoring (max 50 points)
        if (refundCount >= 10) {
            score += 50;
        } else if (refundCount >= 5) {
            score += 30;
        } else if (refundCount >= 3) {
            score += 15;
        }

        return normalizeScore(score);
    }

    /**
     * Calculate address suspicion score for multiple users at same address.
     *
     * @param userCount Number of users at same address
     * @param orderCount Total orders from that address
     * @return Risk score 0-100
     */
    public int calculateAddressSuspicionScore(long userCount, long orderCount) {
        double score = 0;

        // User concentration (max 60 points)
        if (userCount >= 10) {
            score += 60;
        } else if (userCount >= 5) {
            score += 40;
        } else if (userCount >= 3) {
            score += 20;
        }

        // Order concentration (max 40 points)
        double ordersPerUser = userCount > 0 ? orderCount / (double) userCount : 0;
        if (ordersPerUser > 10) {
            score += 40;
        } else if (ordersPerUser > 5) {
            score += 25;
        } else if (ordersPerUser > 2) {
            score += 10;
        }

        return normalizeScore(score);
    }

    /**
     * Calculate restaurant anomaly risk score.
     *
     * @param cancellationRate Restaurant's cancellation rate
     * @param platformAvgRate Platform average cancellation rate
     * @return Risk score 0-100
     */
    public int calculateRestaurantRiskScore(double cancellationRate, double platformAvgRate) {
        if (platformAvgRate <= 0) return 0;

        double ratio = cancellationRate / platformAvgRate;
        double score = 0;

        // Ratio-based scoring
        if (ratio > 5) {
            score = 90;
        } else if (ratio > 3) {
            score = 70;
        } else if (ratio > 2) {
            score = 50;
        } else if (ratio > 1.5) {
            score = 30;
        } else if (ratio > 1) {
            score = 15;
        }

        return normalizeScore(score);
    }

    // ============================================
    // Account Integrity Risk Calculations
    // ============================================

    /**
     * Calculate fake account risk score.
     *
     * @param accountAgeHours Account age in hours
     * @param usedPromoOnFirst Whether promo was used on first order
     * @return Risk score 0-100
     */
    public int calculateFakeAccountRiskScore(int accountAgeHours, boolean usedPromoOnFirst) {
        double score = 0;

        // Account age scoring (max 60 points)
        if (accountAgeHours < 1) {
            score += 60; // Very new account
        } else if (accountAgeHours < 6) {
            score += 40;
        } else if (accountAgeHours < 24) {
            score += 20;
        } else if (accountAgeHours < 48) {
            score += 10;
        }

        // Promo on first order (40 points)
        if (usedPromoOnFirst) {
            score += 40;
        }

        return normalizeScore(score);
    }

    /**
     * Calculate multi-account risk score.
     *
     * @param userCount Number of users sharing identifier
     * @param orderCount Total orders from shared identifier
     * @return Risk score 0-100
     */
    public int calculateMultiAccountRiskScore(int userCount, long orderCount) {
        double score = 0;

        // User sharing concentration (max 70 points)
        if (userCount >= 10) {
            score += 70;
        } else if (userCount >= 5) {
            score += 50;
        } else if (userCount >= 3) {
            score += 30;
        } else if (userCount >= 2) {
            score += 15;
        }

        // Order activity bonus (max 30 points)
        if (orderCount >= 50) {
            score += 30;
        } else if (orderCount >= 20) {
            score += 20;
        } else if (orderCount >= 10) {
            score += 10;
        }

        return normalizeScore(score);
    }

    /**
     * Calculate payment token sharing risk score.
     *
     * @param userCount Number of users sharing token
     * @param transactionCount Transactions with shared token
     * @return Risk score 0-100
     */
    public int calculateTokenSharingRiskScore(int userCount, long transactionCount) {
        double score = 0;

        // Sharing is inherently suspicious (max 80 points)
        if (userCount >= 5) {
            score += 80;
        } else if (userCount >= 3) {
            score += 60;
        } else if (userCount >= 2) {
            score += 40;
        }

        // Transaction volume (max 20 points)
        if (transactionCount >= 20) {
            score += 20;
        } else if (transactionCount >= 10) {
            score += 15;
        } else if (transactionCount >= 5) {
            score += 10;
        }

        return normalizeScore(score);
    }

    /**
     * Calculate device trust risk score.
     *
     * @param trustScore Device trust score (0-100, higher is better)
     * @param isEmulator Whether device is emulator
     * @param isRooted Whether device is rooted
     * @return Risk score 0-100 (higher is worse)
     */
    public int calculateDeviceRiskScore(int trustScore, boolean isEmulator, boolean isRooted) {
        double score = 0;

        // Inverse of trust score (max 50 points)
        score += (100 - trustScore) * 0.5;

        // Emulator penalty (25 points)
        if (isEmulator) {
            score += 25;
        }

        // Rooted penalty (25 points)
        if (isRooted) {
            score += 25;
        }

        return normalizeScore(score);
    }

    // ============================================
    // Referral Fraud Risk Calculations
    // ============================================

    /**
     * Calculate device-based referral fraud risk score.
     *
     * @param signupCount Number of signups from same device
     * @return Risk score 0-100
     */
    public int calculateDeviceReferralRiskScore(int signupCount) {
        double score = 0;

        // Multiple signups from same device is suspicious
        if (signupCount >= 10) {
            score = 95;
        } else if (signupCount >= 5) {
            score = 75;
        } else if (signupCount >= 3) {
            score = 50;
        } else if (signupCount >= 2) {
            score = 30;
        }

        return normalizeScore(score);
    }

    /**
     * Calculate IP-based referral fraud risk score.
     *
     * @param signupCount Number of signups from same IP
     * @return Risk score 0-100
     */
    public int calculateIpReferralRiskScore(int signupCount) {
        // IP can be shared (offices, universities), so slightly lower scores
        double score = 0;

        if (signupCount >= 20) {
            score = 90;
        } else if (signupCount >= 10) {
            score = 65;
        } else if (signupCount >= 5) {
            score = 40;
        } else if (signupCount >= 3) {
            score = 20;
        }

        return normalizeScore(score);
    }

    /**
     * Calculate circular referral risk score.
     *
     * @param chainLength Length of referral chain forming circle
     * @param usersInvolved Number of users in circular referral
     * @return Risk score 0-100
     */
    public int calculateCircularReferralRiskScore(int chainLength, int usersInvolved) {
        // Circular referrals are highly suspicious
        double score = 70; // Base score for any circular referral

        // Chain length bonus (max 15 points)
        if (chainLength >= 5) {
            score += 15;
        } else if (chainLength >= 3) {
            score += 10;
        }

        // Users involved bonus (max 15 points)
        if (usersInvolved >= 10) {
            score += 15;
        } else if (usersInvolved >= 5) {
            score += 10;
        }

        return normalizeScore(score);
    }

    /**
     * Calculate promo abuse risk score.
     *
     * @param promoOrderRate Percentage of orders using promos
     * @param hasNoPaidOrders Whether user has never paid full price
     * @return Risk score 0-100
     */
    public int calculatePromoAbuseRiskScore(double promoOrderRate, boolean hasNoPaidOrders) {
        double score = 0;

        // Promo usage rate (max 60 points)
        if (promoOrderRate >= 90) {
            score += 60;
        } else if (promoOrderRate >= 70) {
            score += 40;
        } else if (promoOrderRate >= 50) {
            score += 25;
        } else if (promoOrderRate >= 30) {
            score += 10;
        }

        // Never paid full price is very suspicious (40 points)
        if (hasNoPaidOrders) {
            score += 40;
        }

        return normalizeScore(score);
    }

    /**
     * Calculate referrer risk score.
     *
     * @param suspiciousReferralRate Rate of suspicious referrals
     * @param suspiciousCount Count of suspicious referrals
     * @return Risk score 0-100
     */
    public int calculateReferrerRiskScore(double suspiciousReferralRate, long suspiciousCount) {
        double score = 0;

        // Rate scoring (max 50 points)
        if (suspiciousReferralRate >= 80) {
            score += 50;
        } else if (suspiciousReferralRate >= 50) {
            score += 35;
        } else if (suspiciousReferralRate >= 30) {
            score += 20;
        }

        // Volume scoring (max 50 points)
        if (suspiciousCount >= 20) {
            score += 50;
        } else if (suspiciousCount >= 10) {
            score += 35;
        } else if (suspiciousCount >= 5) {
            score += 20;
        }

        return normalizeScore(score);
    }

    // ============================================
    // Security Log Risk Calculations
    // ============================================

    /**
     * Calculate login security risk score.
     *
     * @param failedCount Number of failed login attempts
     * @param failureRate Failed login rate
     * @return Risk score 0-100
     */
    public int calculateLoginRiskScore(long failedCount, double failureRate) {
        double score = 0;

        // Failed count scoring (max 50 points)
        if (failedCount >= 20) {
            score += 50;
        } else if (failedCount >= 10) {
            score += 35;
        } else if (failedCount >= 5) {
            score += 20;
        } else if (failedCount >= 3) {
            score += 10;
        }

        // Failure rate scoring (max 50 points)
        if (failureRate >= 80) {
            score += 50;
        } else if (failureRate >= 50) {
            score += 35;
        } else if (failureRate >= 30) {
            score += 20;
        }

        return normalizeScore(score);
    }

    /**
     * Calculate IP-based login risk score.
     *
     * @param failedCount Failed login attempts from IP
     * @param usersTargeted Number of different users targeted
     * @param isSuspiciousIp Whether IP is flagged as suspicious
     * @return Risk score 0-100
     */
    public int calculateIpLoginRiskScore(long failedCount, long usersTargeted, boolean isSuspiciousIp) {
        double score = 0;

        // Failed attempts (max 30 points)
        if (failedCount >= 50) {
            score += 30;
        } else if (failedCount >= 20) {
            score += 20;
        } else if (failedCount >= 10) {
            score += 10;
        }

        // Multiple users targeted - credential stuffing indicator (max 40 points)
        if (usersTargeted >= 10) {
            score += 40;
        } else if (usersTargeted >= 5) {
            score += 25;
        } else if (usersTargeted >= 3) {
            score += 15;
        }

        // Suspicious IP bonus (30 points)
        if (isSuspiciousIp) {
            score += 30;
        }

        return normalizeScore(score);
    }

    /**
     * Calculate IP threat score.
     *
     * @param isVpn Whether IP is VPN
     * @param isTor Whether IP is TOR exit node
     * @param isProxy Whether IP is proxy
     * @param failedAttempts Failed attempts from this IP
     * @param usersFromIp Number of users from this IP
     * @return Risk score 0-100
     */
    public int calculateIpThreatScore(boolean isVpn, boolean isTor, boolean isProxy,
                                       long failedAttempts, long usersFromIp) {
        double score = 0;

        // Anonymization services (max 50 points)
        if (isTor) {
            score += 50; // TOR is highest risk
        } else if (isProxy) {
            score += 35;
        } else if (isVpn) {
            score += 20; // VPN is common and might be legitimate
        }

        // Failed attempts bonus (max 25 points)
        if (failedAttempts >= 20) {
            score += 25;
        } else if (failedAttempts >= 10) {
            score += 15;
        } else if (failedAttempts >= 5) {
            score += 10;
        }

        // Multiple users bonus (max 25 points)
        if (usersFromIp >= 10) {
            score += 25;
        } else if (usersFromIp >= 5) {
            score += 15;
        }

        return normalizeScore(score);
    }

    /**
     * Calculate brute force attack risk score.
     *
     * @param attemptCount Number of attempts in time window
     * @param timeWindowMinutes Time window in minutes
     * @param uniquePasswords Number of unique passwords tried
     * @return Risk score 0-100
     */
    public int calculateBruteForceRiskScore(long attemptCount, int timeWindowMinutes, int uniquePasswords) {
        double score = 0;

        // Velocity of attempts (max 40 points)
        double attemptsPerMinute = timeWindowMinutes > 0 ? attemptCount / (double) timeWindowMinutes : 0;
        if (attemptsPerMinute >= 10) {
            score += 40;
        } else if (attemptsPerMinute >= 5) {
            score += 30;
        } else if (attemptsPerMinute >= 2) {
            score += 20;
        } else if (attemptsPerMinute >= 1) {
            score += 10;
        }

        // Total attempts (max 30 points)
        if (attemptCount >= 100) {
            score += 30;
        } else if (attemptCount >= 50) {
            score += 20;
        } else if (attemptCount >= 20) {
            score += 10;
        }

        // Unique passwords - dictionary attack indicator (max 30 points)
        if (uniquePasswords >= 50) {
            score += 30;
        } else if (uniquePasswords >= 20) {
            score += 20;
        } else if (uniquePasswords >= 10) {
            score += 10;
        }

        return normalizeScore(score);
    }

    /**
     * Calculate rate limit violation risk score.
     *
     * @param violationCount Number of rate limit violations
     * @param violationRate Percentage of requests that hit rate limit
     * @return Risk score 0-100
     */
    public int calculateRateLimitRiskScore(long violationCount, double violationRate) {
        double score = 0;

        // Violation count (max 50 points)
        if (violationCount >= 100) {
            score += 50;
        } else if (violationCount >= 50) {
            score += 35;
        } else if (violationCount >= 20) {
            score += 20;
        } else if (violationCount >= 10) {
            score += 10;
        }

        // Violation rate (max 50 points)
        if (violationRate >= 50) {
            score += 50;
        } else if (violationRate >= 30) {
            score += 35;
        } else if (violationRate >= 20) {
            score += 20;
        } else if (violationRate >= 10) {
            score += 10;
        }

        return normalizeScore(score);
    }

    // ============================================
    // Overall Risk Calculations
    // ============================================

    /**
     * Calculate overall fraud risk score from multiple category scores.
     * Uses weighted average with fraud severity weights.
     *
     * @param paymentRisk Payment fraud risk score
     * @param behavioralRisk Behavioral fraud risk score
     * @param accountRisk Account integrity risk score
     * @param referralRisk Referral fraud risk score
     * @param securityRisk Security risk score
     * @return Overall risk score 0-100
     */
    public int calculateOverallRiskScore(int paymentRisk, int behavioralRisk,
                                          int accountRisk, int referralRisk, int securityRisk) {
        // Weights based on financial impact potential
        double paymentWeight = 0.30; // Payment fraud has direct financial impact
        double behavioralWeight = 0.25; // Behavioral fraud can lead to significant losses
        double accountWeight = 0.20; // Account integrity affects overall platform trust
        double referralWeight = 0.10; // Referral fraud is typically lower impact
        double securityWeight = 0.15; // Security issues can escalate quickly

        double weightedScore = (paymentRisk * paymentWeight) +
                (behavioralRisk * behavioralWeight) +
                (accountRisk * accountWeight) +
                (referralRisk * referralWeight) +
                (securityRisk * securityWeight);

        return normalizeScore(weightedScore);
    }

    /**
     * Calculate financial impact estimate from fraud metrics.
     *
     * @param failedPaymentAmount Total failed payment amount
     * @param refundAmount Total refund amount
     * @param promoAbuseEstimate Estimated promo abuse losses
     * @return Estimated total financial impact
     */
    public double calculateFinancialImpact(double failedPaymentAmount, double refundAmount,
                                            double promoAbuseEstimate) {
        // Sum up all potential fraud-related financial losses
        return failedPaymentAmount + refundAmount + promoAbuseEstimate;
    }

    // ============================================
    // Helper Methods
    // ============================================

    /**
     * Normalize score to 0-100 range.
     */
    private int normalizeScore(double score) {
        return (int) Math.max(0, Math.min(MAX_RISK, Math.round(score)));
    }

    /**
     * Get risk level label from score.
     *
     * @param score Risk score 0-100
     * @return Risk level label
     */
    public String getRiskLevel(int score) {
        if (score <= LOW_RISK_MAX) {
            return "LOW";
        } else if (score <= MEDIUM_RISK_MAX) {
            return "MEDIUM";
        } else if (score <= HIGH_RISK_MAX) {
            return "HIGH";
        } else {
            return "CRITICAL";
        }
    }

    /**
     * Check if score indicates high or critical risk.
     */
    public boolean isHighRisk(int score) {
        return score > MEDIUM_RISK_MAX;
    }

    /**
     * Check if score indicates critical risk.
     */
    public boolean isCriticalRisk(int score) {
        return score > HIGH_RISK_MAX;
    }
}
