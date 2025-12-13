package com.fooddelivery.analytics.fraud.service;

import com.fooddelivery.analytics.fraud.collector.*;
import com.fooddelivery.analytics.fraud.dto.*;
import com.fooddelivery.analytics.fraud.mapper.FraudSummaryMapper;
import com.fooddelivery.analytics.fraud.repository.*;
import com.fooddelivery.analytics.fraud.util.FraudConstants;
import com.fooddelivery.analytics.fraud.util.FraudDateUtils;
import com.fooddelivery.analytics.fraud.util.FraudRiskCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementation of FraudAnalyticsService providing comprehensive fraud detection
 * and metrics across payment, behavioral, account, referral, and security categories.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FraudAnalyticsServiceImpl implements FraudAnalyticsService {

    private final PaymentFraudCollector paymentFraudCollector;
    private final BehavioralFraudCollector behavioralFraudCollector;
    private final AccountIntegrityCollector accountIntegrityCollector;
    private final ReferralFraudCollector referralFraudCollector;
    private final SecurityLogCollector securityLogCollector;

    private final FraudSummaryMapper fraudSummaryMapper;
    private final FraudRiskCalculator riskCalculator;

    // Repositories for user-specific queries
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final FraudOrderHistoryRepository orderHistoryRepository;
    private final AuthLogRepository authLogRepository;
    private final DeviceFingerprintRepository deviceFingerprintRepository;
    private final ReferralEventRepository referralEventRepository;

    @Override
    @Cacheable(value = FraudConstants.CACHE_NAME_FRAUD_SUMMARY,
            key = "'summary:' + #request.startDate + ':' + #request.endDate",
            unless = "#result == null")
    public FraudSummaryDto getFraudSummary(FraudMetricsRequest request) {
        log.info("Generating fraud summary for period {} to {}", request.getStartDate(), request.getEndDate());

        LocalDateTime startDate = request.getStartDate();
        LocalDateTime endDate = request.getEndDate();

        // Collect all category metrics
        PaymentFraudMetricsDto paymentMetrics = getPaymentFraudMetrics(
                startDate, endDate,
                request.getFailedPaymentThreshold(),
                request.getPaymentTimeWindowMinutes(),
                request.getIncludeDetailedLists(),
                request.getMaxListSize());

        BehavioralFraudMetricsDto behavioralMetrics = getBehavioralFraudMetrics(
                startDate, endDate,
                request.getVelocityThreshold(),
                request.getRefundRateThreshold(),
                request.getOrderValueZScoreThreshold(),
                request.getAddressUserThreshold(),
                request.getIncludeDetailedLists(),
                request.getMaxListSize());

        AccountIntegrityMetricsDto accountMetrics = getAccountIntegrityMetrics(
                startDate, endDate,
                request.getFakeAccountAgeThreshold(),
                request.getDeviceShareThreshold(),
                request.getIpShareThreshold(),
                request.getIncludeDetailedLists(),
                request.getMaxListSize());

        ReferralFraudMetricsDto referralMetrics = getReferralFraudMetrics(
                startDate, endDate,
                request.getDeviceSignupThreshold(),
                request.getIpSignupThreshold(),
                request.getMinOrdersForPromoAbuse(),
                request.getIncludeDetailedLists(),
                request.getMaxListSize());

        SecurityLogMetricsDto securityMetrics = getSecurityLogMetrics(
                startDate, endDate,
                request.getFailedLoginThreshold(),
                request.getLoginTimeWindowMinutes(),
                request.getRateLimitThreshold(),
                request.getIncludeDetailedLists(),
                request.getMaxListSize());

        // Build comprehensive summary
        return fraudSummaryMapper.buildFraudSummary(
                paymentMetrics, behavioralMetrics, accountMetrics,
                referralMetrics, securityMetrics, startDate, endDate);
    }

    @Override
    @Cacheable(value = FraudConstants.CACHE_NAME_FRAUD_METRICS,
            key = "'payment:' + #startDate + ':' + #endDate + ':' + #failedThreshold",
            unless = "#result == null")
    public PaymentFraudMetricsDto getPaymentFraudMetrics(
            LocalDateTime startDate, LocalDateTime endDate,
            int failedThreshold, int timeWindowMinutes,
            boolean includeDetails, int maxListSize) {
        log.debug("Collecting payment fraud metrics from {} to {}", startDate, endDate);
        return paymentFraudCollector.collect(startDate, endDate, failedThreshold,
                timeWindowMinutes, includeDetails, maxListSize);
    }

    @Override
    @Cacheable(value = FraudConstants.CACHE_NAME_FRAUD_METRICS,
            key = "'behavioral:' + #startDate + ':' + #endDate + ':' + #velocityThreshold",
            unless = "#result == null")
    public BehavioralFraudMetricsDto getBehavioralFraudMetrics(
            LocalDateTime startDate, LocalDateTime endDate,
            int velocityThreshold, double refundRateThreshold,
            double orderValueZScore, int addressUserThreshold,
            boolean includeDetails, int maxListSize) {
        log.debug("Collecting behavioral fraud metrics from {} to {}", startDate, endDate);
        // Use default values for missing parameters
        int velocityWindowMinutes = 60;
        int minOrdersForRefund = 5;
        return behavioralFraudCollector.collect(startDate, endDate, velocityThreshold,
                velocityWindowMinutes, orderValueZScore, refundRateThreshold,
                minOrdersForRefund, addressUserThreshold,
                includeDetails, maxListSize);
    }

    @Override
    @Cacheable(value = FraudConstants.CACHE_NAME_FRAUD_METRICS,
            key = "'account:' + #startDate + ':' + #endDate + ':' + #deviceThreshold",
            unless = "#result == null")
    public AccountIntegrityMetricsDto getAccountIntegrityMetrics(
            LocalDateTime startDate, LocalDateTime endDate,
            int fakeAccountAgeThreshold, int deviceThreshold, int ipThreshold,
            boolean includeDetails, int maxListSize) {
        log.debug("Collecting account integrity metrics from {} to {}", startDate, endDate);
        return accountIntegrityCollector.collect(startDate, endDate, fakeAccountAgeThreshold,
                deviceThreshold, ipThreshold, includeDetails, maxListSize);
    }

    @Override
    @Cacheable(value = FraudConstants.CACHE_NAME_REFERRAL_METRICS,
            key = "'referral:' + #startDate + ':' + #endDate + ':' + #deviceSignupThreshold",
            unless = "#result == null")
    public ReferralFraudMetricsDto getReferralFraudMetrics(
            LocalDateTime startDate, LocalDateTime endDate,
            int deviceSignupThreshold, int ipSignupThreshold,
            int minOrdersForPromoAbuse, boolean includeDetails, int maxListSize) {
        log.debug("Collecting referral fraud metrics from {} to {}", startDate, endDate);
        return referralFraudCollector.collect(startDate, endDate, deviceSignupThreshold,
                ipSignupThreshold, minOrdersForPromoAbuse, includeDetails, maxListSize);
    }

    @Override
    @Cacheable(value = FraudConstants.CACHE_NAME_SECURITY_METRICS,
            key = "'security:' + #startDate + ':' + #endDate + ':' + #failedLoginThreshold",
            unless = "#result == null")
    public SecurityLogMetricsDto getSecurityLogMetrics(
            LocalDateTime startDate, LocalDateTime endDate,
            int failedLoginThreshold, int loginTimeWindowMinutes,
            int rateLimitThreshold, boolean includeDetails, int maxListSize) {
        log.debug("Collecting security log metrics from {} to {}", startDate, endDate);
        return securityLogCollector.collect(startDate, endDate, failedLoginThreshold,
                loginTimeWindowMinutes, rateLimitThreshold, includeDetails, maxListSize);
    }

    @Override
    public FraudSummaryDto getRealTimeAlerts(FraudMetricsRequest request) {
        log.info("Generating real-time fraud alerts");

        // Use shorter time window for real-time alerts
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusHours(1);

        FraudMetricsRequest realTimeRequest = FraudMetricsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .failedPaymentThreshold(request.getFailedPaymentThreshold())
                .paymentTimeWindowMinutes(15) // Shorter window for real-time
                .velocityThreshold(request.getVelocityThreshold())
                .refundRateThreshold(request.getRefundRateThreshold())
                .orderValueZScoreThreshold(request.getOrderValueZScoreThreshold())
                .addressUserThreshold(request.getAddressUserThreshold())
                .fakeAccountAgeThreshold(request.getFakeAccountAgeThreshold())
                .deviceShareThreshold(request.getDeviceShareThreshold())
                .ipShareThreshold(request.getIpShareThreshold())
                .deviceSignupThreshold(request.getDeviceSignupThreshold())
                .ipSignupThreshold(request.getIpSignupThreshold())
                .promoAbuseRateThreshold(request.getPromoAbuseRateThreshold())
                .failedLoginThreshold(request.getFailedLoginThreshold())
                .loginTimeWindowMinutes(5) // Shorter window for real-time
                .rateLimitThreshold(request.getRateLimitThreshold())
                .includeDetailedLists(true)
                .maxListSize(FraudConstants.DEFAULT_TOP_THREATS_SIZE)
                .build();

        return getFraudSummary(realTimeRequest);
    }

    @Override
    public UserFraudRiskDto getUserRiskScore(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Calculating risk score for user {} from {} to {}", userId, startDate, endDate);

        // Collect user-specific metrics
        UserFraudRiskDto.PaymentRiskSummaryDto paymentRisk = collectUserPaymentRisk(userId, startDate, endDate);
        UserFraudRiskDto.BehavioralRiskSummaryDto behavioralRisk = collectUserBehavioralRisk(userId, startDate, endDate);
        UserFraudRiskDto.AccountRiskSummaryDto accountRisk = collectUserAccountRisk(userId, startDate, endDate);
        UserFraudRiskDto.SecurityRiskSummaryDto securityRisk = collectUserSecurityRisk(userId, startDate, endDate);

        // Calculate category scores
        int paymentScore = calculateUserPaymentScore(paymentRisk);
        int behavioralScore = calculateUserBehavioralScore(behavioralRisk);
        int accountScore = calculateUserAccountScore(accountRisk);
        int securityScore = calculateUserSecurityScore(securityRisk);

        // Calculate overall score
        int overallScore = riskCalculator.calculateOverallRiskScore(
                paymentScore, behavioralScore, accountScore, 0, securityScore);

        // Generate risk indicators
        List<String> riskIndicators = generateUserRiskIndicators(
                paymentRisk, behavioralRisk, accountRisk, securityRisk);

        // Generate risk factors with contributions
        List<UserFraudRiskDto.RiskFactorDto> riskFactors = generateRiskFactors(
                paymentScore, behavioralScore, accountScore, securityScore,
                paymentRisk, behavioralRisk, accountRisk, securityRisk);

        // Generate recommended actions
        List<String> recommendedActions = generateRecommendedActions(
                overallScore, riskIndicators);

        return UserFraudRiskDto.builder()
                .userId(userId)
                .overallRiskScore(overallScore)
                .riskLevel(riskCalculator.getRiskLevel(overallScore))
                .paymentRiskScore(paymentScore)
                .behavioralRiskScore(behavioralScore)
                .accountRiskScore(accountScore)
                .referralRiskScore(0)
                .securityRiskScore(securityScore)
                .riskIndicators(riskIndicators)
                .riskFactors(riskFactors)
                .paymentRisk(paymentRisk)
                .behavioralRisk(behavioralRisk)
                .accountRisk(accountRisk)
                .securityRisk(securityRisk)
                .recommendedActions(recommendedActions)
                .analyzedAt(LocalDateTime.now())
                .periodStart(startDate)
                .periodEnd(endDate)
                .build();
    }

    @Override
    @CacheEvict(value = {
            FraudConstants.CACHE_NAME_FRAUD_METRICS,
            FraudConstants.CACHE_NAME_SECURITY_METRICS,
            FraudConstants.CACHE_NAME_REFERRAL_METRICS,
            FraudConstants.CACHE_NAME_FRAUD_SUMMARY
    }, allEntries = true)
    public void invalidateCache() {
        log.info("Invalidating all fraud analytics caches");
    }

    @Override
    @CacheEvict(value = FraudConstants.CACHE_NAME_FRAUD_METRICS,
            key = "#category.toLowerCase() + ':*'")
    public void invalidateCategoryCache(String category) {
        log.info("Invalidating cache for category: {}", category);
    }

    // ============================================
    // Private Helper Methods
    // ============================================

    private UserFraudRiskDto.PaymentRiskSummaryDto collectUserPaymentRisk(
            Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        Long totalAttempts = paymentAttemptRepository.countByUserIdAndCreatedAtBetween(
                userId, startDate, endDate);
        Long failedPayments = paymentAttemptRepository.countUserFailedPayments(userId, startDate, endDate);
        BigDecimal failedAmount = paymentAttemptRepository.getUserTotalFailedAmount(userId, startDate, endDate);

        if (totalAttempts == null) totalAttempts = 0L;
        if (failedPayments == null) failedPayments = 0L;
        if (failedAmount == null) failedAmount = BigDecimal.ZERO;

        double failureRate = totalAttempts > 0 ? failedPayments * 100.0 / totalAttempts : 0.0;

        return UserFraudRiskDto.PaymentRiskSummaryDto.builder()
                .totalPaymentAttempts(totalAttempts)
                .failedPayments(failedPayments)
                .failureRate(Math.round(failureRate * 100.0) / 100.0)
                .totalFailedAmount(failedAmount)
                .uniqueCardsUsed(0L) // Would need additional query
                .hasCardTestingPattern(failedPayments >= 5 && failureRate > 50)
                .lastFailureReason(null) // Would need additional query
                .build();
    }

    private UserFraudRiskDto.BehavioralRiskSummaryDto collectUserBehavioralRisk(
            Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        Object[] stats = orderHistoryRepository.getUserOrderStats(userId, startDate, endDate);

        long totalOrders = 0;
        long refundedOrders = 0;
        BigDecimal avgOrderValue = BigDecimal.ZERO;

        if (stats != null) {
            totalOrders = stats[0] != null ? ((Number) stats[0]).longValue() : 0;
            refundedOrders = stats[1] != null ? ((Number) stats[1]).longValue() : 0;
            avgOrderValue = stats[2] != null ? new BigDecimal(stats[2].toString()) : BigDecimal.ZERO;
        }

        double refundRate = totalOrders > 0 ? refundedOrders * 100.0 / totalOrders : 0.0;
        long daysBetween = FraudDateUtils.daysBetween(startDate, endDate);
        double ordersPerDay = daysBetween > 0 ? totalOrders / (double) daysBetween : totalOrders;

        return UserFraudRiskDto.BehavioralRiskSummaryDto.builder()
                .totalOrders(totalOrders)
                .ordersPerDay(Math.round(ordersPerDay * 100.0) / 100.0)
                .refundedOrders(refundedOrders)
                .refundRate(Math.round(refundRate * 100.0) / 100.0)
                .avgOrderValue(avgOrderValue)
                .hasVelocityViolation(ordersPerDay > 10)
                .hasOrderValueAnomaly(false) // Would need z-score calculation
                .uniqueDeliveryAddresses(0) // Would need additional query
                .build();
    }

    private UserFraudRiskDto.AccountRiskSummaryDto collectUserAccountRisk(
            Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        Long devicesUsed = deviceFingerprintRepository.countUserDevices(userId, startDate, endDate);
        Boolean hasDeviceSharing = deviceFingerprintRepository.hasSharedDevice(userId, startDate, endDate);

        Long referralsMade = referralEventRepository.countByReferrerId(userId);
        Long suspiciousReferrals = referralEventRepository.countSuspiciousReferralsByReferrer(
                userId, startDate, endDate);

        if (devicesUsed == null) devicesUsed = 0L;
        if (referralsMade == null) referralsMade = 0L;
        if (suspiciousReferrals == null) suspiciousReferrals = 0L;

        return UserFraudRiskDto.AccountRiskSummaryDto.builder()
                .accountAgeHours(0) // Would need user creation date
                .isNewAccount(false)
                .devicesUsed(devicesUsed.intValue())
                .ipsUsed(0) // Would need additional query
                .hasDeviceSharing(hasDeviceSharing != null && hasDeviceSharing)
                .usedPromoOnFirstOrder(false) // Would need additional query
                .referralsMade(referralsMade.intValue())
                .suspiciousReferrals(suspiciousReferrals.intValue())
                .build();
    }

    private UserFraudRiskDto.SecurityRiskSummaryDto collectUserSecurityRisk(
            Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        Long failedLogins = authLogRepository.countFailedLoginsByUser(userId, startDate, endDate);
        Long totalLogins = authLogRepository.countTotalLoginsByUser(userId, startDate, endDate);
        Boolean hasVpn = authLogRepository.hasVpnUsage(userId, startDate, endDate);
        Boolean hasTor = authLogRepository.hasTorUsage(userId, startDate, endDate);
        Boolean hasRateLimit = authLogRepository.hasRateLimitViolations(userId, startDate, endDate);

        if (failedLogins == null) failedLogins = 0L;
        if (totalLogins == null) totalLogins = 0L;

        double failureRate = totalLogins > 0 ? failedLogins * 100.0 / totalLogins : 0.0;

        return UserFraudRiskDto.SecurityRiskSummaryDto.builder()
                .failedLoginAttempts(failedLogins)
                .loginFailureRate(Math.round(failureRate * 100.0) / 100.0)
                .hasVpnUsage(hasVpn != null && hasVpn)
                .hasTorUsage(hasTor != null && hasTor)
                .hasRateLimitViolations(hasRateLimit != null && hasRateLimit)
                .suspiciousIpsUsed(0)
                .lastFailedLogin(null)
                .build();
    }

    private int calculateUserPaymentScore(UserFraudRiskDto.PaymentRiskSummaryDto risk) {
        if (risk == null) return 0;

        return riskCalculator.calculatePaymentRiskScore(
                risk.getFailedPayments() != null ? risk.getFailedPayments().intValue() : 0,
                risk.getFailureRate() != null ? risk.getFailureRate() : 0.0,
                risk.getTotalPaymentAttempts() != null ? risk.getTotalPaymentAttempts().intValue() : 0
        );
    }

    private int calculateUserBehavioralScore(UserFraudRiskDto.BehavioralRiskSummaryDto risk) {
        if (risk == null) return 0;

        int score = 0;

        // Refund abuse contribution
        if (risk.getRefundRate() != null && risk.getRefundedOrders() != null) {
            score += riskCalculator.calculateRefundRiskScore(
                    risk.getRefundRate(),
                    risk.getRefundedOrders()
            ) / 2;
        }

        // Velocity contribution
        if (risk.getHasVelocityViolation() != null && risk.getHasVelocityViolation()) {
            score += 30;
        }

        return Math.min(score, 100);
    }

    private int calculateUserAccountScore(UserFraudRiskDto.AccountRiskSummaryDto risk) {
        if (risk == null) return 0;

        int score = 0;

        // Device sharing
        if (risk.getHasDeviceSharing() != null && risk.getHasDeviceSharing()) {
            score += 30;
        }

        // Multiple devices
        if (risk.getDevicesUsed() != null && risk.getDevicesUsed() > 5) {
            score += 20;
        }

        // Suspicious referrals
        if (risk.getSuspiciousReferrals() != null && risk.getSuspiciousReferrals() > 0) {
            score += Math.min(risk.getSuspiciousReferrals() * 10, 30);
        }

        // New account with promo
        if (risk.getIsNewAccount() != null && risk.getIsNewAccount() &&
                risk.getUsedPromoOnFirstOrder() != null && risk.getUsedPromoOnFirstOrder()) {
            score += 20;
        }

        return Math.min(score, 100);
    }

    private int calculateUserSecurityScore(UserFraudRiskDto.SecurityRiskSummaryDto risk) {
        if (risk == null) return 0;

        int score = 0;

        // Failed logins
        if (risk.getFailedLoginAttempts() != null && risk.getLoginFailureRate() != null) {
            score += riskCalculator.calculateLoginRiskScore(
                    risk.getFailedLoginAttempts(),
                    risk.getLoginFailureRate()
            ) / 2;
        }

        // VPN/TOR usage
        if (risk.getHasTorUsage() != null && risk.getHasTorUsage()) {
            score += 40;
        } else if (risk.getHasVpnUsage() != null && risk.getHasVpnUsage()) {
            score += 15;
        }

        // Rate limit violations
        if (risk.getHasRateLimitViolations() != null && risk.getHasRateLimitViolations()) {
            score += 20;
        }

        return Math.min(score, 100);
    }

    private List<String> generateUserRiskIndicators(
            UserFraudRiskDto.PaymentRiskSummaryDto payment,
            UserFraudRiskDto.BehavioralRiskSummaryDto behavioral,
            UserFraudRiskDto.AccountRiskSummaryDto account,
            UserFraudRiskDto.SecurityRiskSummaryDto security) {

        List<String> indicators = new ArrayList<>();

        // Payment indicators
        if (payment != null) {
            if (payment.getFailureRate() != null && payment.getFailureRate() > 50) {
                indicators.add(FraudConstants.INDICATOR_HIGH_FAILURE_RATE);
            }
            if (payment.getHasCardTestingPattern() != null && payment.getHasCardTestingPattern()) {
                indicators.add(FraudConstants.INDICATOR_CARD_TESTING);
            }
        }

        // Behavioral indicators
        if (behavioral != null) {
            if (behavioral.getHasVelocityViolation() != null && behavioral.getHasVelocityViolation()) {
                indicators.add(FraudConstants.INDICATOR_VELOCITY_VIOLATION);
            }
            if (behavioral.getRefundRate() != null && behavioral.getRefundRate() > 30) {
                indicators.add(FraudConstants.INDICATOR_REFUND_ABUSE);
            }
        }

        // Account indicators
        if (account != null) {
            if (account.getHasDeviceSharing() != null && account.getHasDeviceSharing()) {
                indicators.add(FraudConstants.INDICATOR_MULTI_ACCOUNT);
            }
        }

        // Security indicators
        if (security != null) {
            if (security.getHasTorUsage() != null && security.getHasTorUsage()) {
                indicators.add(FraudConstants.INDICATOR_TOR_DETECTED);
            }
            if (security.getHasVpnUsage() != null && security.getHasVpnUsage()) {
                indicators.add(FraudConstants.INDICATOR_VPN_DETECTED);
            }
        }

        return indicators;
    }

    private List<UserFraudRiskDto.RiskFactorDto> generateRiskFactors(
            int paymentScore, int behavioralScore, int accountScore, int securityScore,
            UserFraudRiskDto.PaymentRiskSummaryDto payment,
            UserFraudRiskDto.BehavioralRiskSummaryDto behavioral,
            UserFraudRiskDto.AccountRiskSummaryDto account,
            UserFraudRiskDto.SecurityRiskSummaryDto security) {

        List<UserFraudRiskDto.RiskFactorDto> factors = new ArrayList<>();

        if (paymentScore > 0) {
            factors.add(UserFraudRiskDto.RiskFactorDto.builder()
                    .factor("Payment Failures")
                    .category(FraudConstants.CATEGORY_PAYMENT)
                    .contribution(paymentScore)
                    .description("Failed payment patterns detected")
                    .severity(riskCalculator.getRiskLevel(paymentScore))
                    .build());
        }

        if (behavioralScore > 0) {
            factors.add(UserFraudRiskDto.RiskFactorDto.builder()
                    .factor("Behavioral Anomalies")
                    .category(FraudConstants.CATEGORY_BEHAVIORAL)
                    .contribution(behavioralScore)
                    .description("Unusual ordering patterns detected")
                    .severity(riskCalculator.getRiskLevel(behavioralScore))
                    .build());
        }

        if (accountScore > 0) {
            factors.add(UserFraudRiskDto.RiskFactorDto.builder()
                    .factor("Account Integrity")
                    .category(FraudConstants.CATEGORY_ACCOUNT)
                    .contribution(accountScore)
                    .description("Account integrity concerns detected")
                    .severity(riskCalculator.getRiskLevel(accountScore))
                    .build());
        }

        if (securityScore > 0) {
            factors.add(UserFraudRiskDto.RiskFactorDto.builder()
                    .factor("Security Concerns")
                    .category(FraudConstants.CATEGORY_SECURITY)
                    .contribution(securityScore)
                    .description("Security-related risks detected")
                    .severity(riskCalculator.getRiskLevel(securityScore))
                    .build());
        }

        // Sort by contribution
        factors.sort((a, b) -> Integer.compare(b.getContribution(), a.getContribution()));
        return factors;
    }

    private List<String> generateRecommendedActions(int overallScore, List<String> indicators) {
        List<String> actions = new ArrayList<>();

        if (overallScore > FraudConstants.HIGH_RISK_THRESHOLD) {
            actions.add("Immediately review account for potential fraud");
            actions.add("Consider temporary account restriction");
        } else if (overallScore > FraudConstants.MEDIUM_RISK_THRESHOLD) {
            actions.add("Monitor account activity closely");
            actions.add("Require additional verification for high-value transactions");
        }

        if (indicators.contains(FraudConstants.INDICATOR_HIGH_FAILURE_RATE)) {
            actions.add("Verify payment methods on file");
        }

        if (indicators.contains(FraudConstants.INDICATOR_REFUND_ABUSE)) {
            actions.add("Review refund history and apply stricter refund policies");
        }

        if (indicators.contains(FraudConstants.INDICATOR_MULTI_ACCOUNT)) {
            actions.add("Investigate potential multi-account fraud");
        }

        if (indicators.contains(FraudConstants.INDICATOR_TOR_DETECTED) ||
                indicators.contains(FraudConstants.INDICATOR_VPN_DETECTED)) {
            actions.add("Verify user identity through additional authentication");
        }

        return actions;
    }
}
