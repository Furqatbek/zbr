package com.fooddelivery.analytics.fraud.collector;

import com.fooddelivery.analytics.fraud.dto.SecurityLogMetricsDto;
import com.fooddelivery.analytics.fraud.repository.AuthLogRepository;
import com.fooddelivery.analytics.fraud.util.FraudRiskCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Collector for security log metrics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityLogCollector {

    private final AuthLogRepository authLogRepository;
    private final FraudRiskCalculator riskCalculator;

    public SecurityLogMetricsDto collect(LocalDateTime startDate, LocalDateTime endDate,
                                         int failedLoginThreshold, int bruteForceThreshold,
                                         int bruteForceWindowMinutes,
                                         boolean includeDetailedLists, int maxListSize) {
        log.debug("Collecting security log metrics from {} to {}", startDate, endDate);

        // Collect sub-metrics
        SecurityLogMetricsDto.LoginSecurityMetricsDto loginMetrics = collectLoginMetrics(
                startDate, endDate, failedLoginThreshold, includeDetailedLists, maxListSize);

        SecurityLogMetricsDto.TokenSecurityMetricsDto tokenMetrics = collectTokenMetrics(startDate, endDate);

        SecurityLogMetricsDto.RateLimitMetricsDto rateLimitMetrics = collectRateLimitMetrics(
                startDate, endDate, includeDetailedLists, maxListSize);

        SecurityLogMetricsDto.IpSecurityMetricsDto ipSecurityMetrics = collectIpSecurityMetrics(
                startDate, endDate, includeDetailedLists, maxListSize);

        SecurityLogMetricsDto.BruteForceMetricsDto bruteForceMetrics = collectBruteForceMetrics(
                startDate, endDate, bruteForceThreshold, bruteForceWindowMinutes, includeDetailedLists, maxListSize);

        // Calculate overall metrics
        long totalEvents = (loginMetrics.getFailedLogins() != null ? loginMetrics.getFailedLogins() : 0) +
                (rateLimitMetrics.getTotalRateLimitedRequests() != null ? rateLimitMetrics.getTotalRateLimitedRequests() : 0);

        long criticalEvents = (bruteForceMetrics.getBruteForceAttempts() != null ? bruteForceMetrics.getBruteForceAttempts() : 0);
        long highEvents = (loginMetrics.getAccountsLocked() != null ? loginMetrics.getAccountsLocked() : 0);

        double healthScore = calculateSecurityHealthScore(loginMetrics, rateLimitMetrics, bruteForceMetrics, ipSecurityMetrics);

        return SecurityLogMetricsDto.builder()
                .loginMetrics(loginMetrics)
                .tokenMetrics(tokenMetrics)
                .rateLimitMetrics(rateLimitMetrics)
                .ipSecurityMetrics(ipSecurityMetrics)
                .bruteForceMetrics(bruteForceMetrics)
                .totalSecurityEvents(totalEvents)
                .criticalEvents(criticalEvents)
                .highSeverityEvents(highEvents)
                .securityHealthScore(round(healthScore))
                .periodStart(startDate)
                .periodEnd(endDate)
                .build();
    }

    private SecurityLogMetricsDto.LoginSecurityMetricsDto collectLoginMetrics(
            LocalDateTime startDate, LocalDateTime endDate, int threshold,
            boolean includeDetails, int maxSize) {

        Long totalLogins = authLogRepository.countTotalLoginAttempts(startDate, endDate);
        Long successful = authLogRepository.countSuccessfulLogins(startDate, endDate);
        Long failed = authLogRepository.countFailedLogins(startDate, endDate);
        Long locked = authLogRepository.countLockedAccounts(startDate, endDate);
        Long uniqueUsers = authLogRepository.countUniqueUsersWithFailedLogins(startDate, endDate);
        Long uniqueIps = authLogRepository.countUniqueIpsWithFailedLogins(startDate, endDate);

        if (totalLogins == null) totalLogins = 0L;
        if (successful == null) successful = 0L;
        if (failed == null) failed = 0L;
        if (locked == null) locked = 0L;
        if (uniqueUsers == null) uniqueUsers = 0L;
        if (uniqueIps == null) uniqueIps = 0L;

        double failureRate = totalLogins > 0 ? failed * 100.0 / totalLogins : 0.0;

        // Failure breakdown
        Map<String, Long> failuresByReason = new LinkedHashMap<>();
        authLogRepository.getFailuresByReason(startDate, endDate).forEach(row -> {
            String reason = row[0] != null ? (String) row[0] : "UNKNOWN";
            failuresByReason.put(reason, ((Number) row[1]).longValue());
        });

        // High risk users
        List<SecurityLogMetricsDto.UserLoginRiskDto> highRiskUsers = new ArrayList<>();
        if (includeDetails) {
            highRiskUsers = authLogRepository.getUsersWithHighFailedLogins(startDate, endDate, threshold)
                    .stream()
                    .limit(maxSize)
                    .map(row -> {
                        Long userId = row[0] != null ? ((Number) row[0]).longValue() : null;
                        String username = (String) row[1];
                        long failedCount = ((Number) row[2]).longValue();
                        long successCount = ((Number) row[3]).longValue();
                        LocalDateTime lastFailed = (LocalDateTime) row[4];
                        long total = failedCount + successCount;
                        double rate = total > 0 ? failedCount * 100.0 / total : 0.0;

                        return SecurityLogMetricsDto.UserLoginRiskDto.builder()
                                .userId(userId)
                                .username(username)
                                .failedAttempts(failedCount)
                                .successfulAttempts(successCount)
                                .failureRate(round(rate))
                                .lastFailedAt(lastFailed)
                                .riskScore(riskCalculator.calculateLoginRiskScore(failedCount, rate))
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        // High risk IPs
        List<SecurityLogMetricsDto.IpLoginRiskDto> highRiskIps = new ArrayList<>();
        if (includeDetails) {
            highRiskIps = authLogRepository.getIpsWithHighFailedLogins(startDate, endDate, threshold)
                    .stream()
                    .limit(maxSize)
                    .map(row -> {
                        String ip = (String) row[0];
                        String country = (String) row[1];
                        String city = (String) row[2];
                        long failedCount = ((Number) row[3]).longValue();
                        long usersTargeted = ((Number) row[4]).longValue();
                        boolean isVpn = row[5] != null && ((Number) row[5]).intValue() > 0;
                        boolean isTor = row[6] != null && ((Number) row[6]).intValue() > 0;

                        return SecurityLogMetricsDto.IpLoginRiskDto.builder()
                                .ipAddressPartial(maskIp(ip))
                                .geoLocation(country != null ? country + ", " + city : null)
                                .failedAttempts(failedCount)
                                .uniqueUsersTargeted(usersTargeted)
                                .isVpn(isVpn)
                                .isTor(isTor)
                                .riskScore(riskCalculator.calculateIpLoginRiskScore(failedCount, usersTargeted, isVpn || isTor))
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        // Hourly distribution
        Map<Integer, Long> failedByHour = new LinkedHashMap<>();
        authLogRepository.getHourlyFailedLogins(startDate, endDate).forEach(row -> {
            failedByHour.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        });

        return SecurityLogMetricsDto.LoginSecurityMetricsDto.builder()
                .totalLoginAttempts(totalLogins)
                .successfulLogins(successful)
                .failedLogins(failed)
                .loginFailureRate(round(failureRate))
                .uniqueUsersWithFailedLogins(uniqueUsers)
                .uniqueIpsWithFailedLogins(uniqueIps)
                .accountsLocked(locked)
                .failuresByReason(failuresByReason)
                .highRiskUsers(highRiskUsers)
                .highRiskIps(highRiskIps)
                .failedLoginsByHour(failedByHour)
                .build();
    }

    private SecurityLogMetricsDto.TokenSecurityMetricsDto collectTokenMetrics(
            LocalDateTime startDate, LocalDateTime endDate) {

        Long totalRefresh = authLogRepository.countTotalTokenRefreshAttempts(startDate, endDate);
        Long failedRefresh = authLogRepository.countFailedTokenRefreshes(startDate, endDate);

        if (totalRefresh == null) totalRefresh = 0L;
        if (failedRefresh == null) failedRefresh = 0L;

        double failureRate = totalRefresh > 0 ? failedRefresh * 100.0 / totalRefresh : 0.0;

        Map<String, Long> errorsByType = new LinkedHashMap<>();
        authLogRepository.getTokenErrorBreakdown(startDate, endDate).forEach(row -> {
            errorsByType.put(row[0].toString(), ((Number) row[1]).longValue());
        });

        return SecurityLogMetricsDto.TokenSecurityMetricsDto.builder()
                .totalTokenRefreshAttempts(totalRefresh)
                .failedTokenRefreshes(failedRefresh)
                .tokenRefreshFailureRate(round(failureRate))
                .expiredTokenUsageAttempts(errorsByType.getOrDefault("TOKEN_EXPIRED", 0L))
                .invalidTokenUsageAttempts(errorsByType.getOrDefault("TOKEN_INVALID", 0L))
                .tokenErrorsByType(errorsByType)
                .build();
    }

    private SecurityLogMetricsDto.RateLimitMetricsDto collectRateLimitMetrics(
            LocalDateTime startDate, LocalDateTime endDate,
            boolean includeDetails, int maxSize) {

        Long totalRateLimited = authLogRepository.countRateLimitedAttempts(startDate, endDate);
        Long uniqueUsers = authLogRepository.countUniqueRateLimitedUsers(startDate, endDate);
        Long uniqueIps = authLogRepository.countUniqueRateLimitedIps(startDate, endDate);

        if (totalRateLimited == null) totalRateLimited = 0L;
        if (uniqueUsers == null) uniqueUsers = 0L;
        if (uniqueIps == null) uniqueIps = 0L;

        List<SecurityLogMetricsDto.RateLimitedEntityDto> topUsers = new ArrayList<>();
        List<SecurityLogMetricsDto.RateLimitedEntityDto> topIps = new ArrayList<>();

        if (includeDetails) {
            topUsers = authLogRepository.getTopRateLimitedUsers(startDate, endDate)
                    .stream()
                    .limit(maxSize)
                    .map(row -> SecurityLogMetricsDto.RateLimitedEntityDto.builder()
                            .identifier("user:" + row[0])
                            .type("USER")
                            .hitCount(((Number) row[1]).longValue())
                            .firstHitAt((LocalDateTime) row[2])
                            .lastHitAt((LocalDateTime) row[3])
                            .build())
                    .collect(Collectors.toList());

            topIps = authLogRepository.getTopRateLimitedIps(startDate, endDate)
                    .stream()
                    .limit(maxSize)
                    .map(row -> SecurityLogMetricsDto.RateLimitedEntityDto.builder()
                            .identifier(maskIp((String) row[0]))
                            .type("IP")
                            .hitCount(((Number) row[1]).longValue())
                            .firstHitAt((LocalDateTime) row[2])
                            .lastHitAt((LocalDateTime) row[3])
                            .build())
                    .collect(Collectors.toList());
        }

        return SecurityLogMetricsDto.RateLimitMetricsDto.builder()
                .totalRateLimitedRequests(totalRateLimited)
                .uniqueRateLimitedUsers(uniqueUsers)
                .uniqueRateLimitedIps(uniqueIps)
                .topRateLimitedUsers(topUsers)
                .topRateLimitedIps(topIps)
                .build();
    }

    private SecurityLogMetricsDto.IpSecurityMetricsDto collectIpSecurityMetrics(
            LocalDateTime startDate, LocalDateTime endDate,
            boolean includeDetails, int maxSize) {

        Long totalIps = authLogRepository.countTotalUniqueIps(startDate, endDate);
        Long vpnIps = authLogRepository.countVpnIps(startDate, endDate);
        Long torIps = authLogRepository.countTorIps(startDate, endDate);
        Long proxyIps = authLogRepository.countProxyIps(startDate, endDate);
        Long suspiciousIps = authLogRepository.countSuspiciousIps(startDate, endDate);

        if (totalIps == null) totalIps = 0L;
        if (vpnIps == null) vpnIps = 0L;
        if (torIps == null) torIps = 0L;
        if (proxyIps == null) proxyIps = 0L;
        if (suspiciousIps == null) suspiciousIps = 0L;

        double suspiciousRate = totalIps > 0 ? suspiciousIps * 100.0 / totalIps : 0.0;

        List<SecurityLogMetricsDto.FlaggedIpDto> flaggedList = new ArrayList<>();
        if (includeDetails) {
            flaggedList = authLogRepository.getFlaggedIpDetails(startDate, endDate)
                    .stream()
                    .limit(maxSize)
                    .map(row -> {
                        boolean isVpn = row[3] != null && ((Number) row[3]).intValue() > 0;
                        boolean isTor = row[4] != null && ((Number) row[4]).intValue() > 0;
                        boolean isProxy = row[5] != null && ((Number) row[5]).intValue() > 0;
                        long requests = ((Number) row[6]).longValue();
                        long failed = ((Number) row[7]).longValue();
                        long users = ((Number) row[8]).longValue();

                        String threatLevel = "LOW";
                        if (isTor || (failed > 50 && users > 5)) threatLevel = "HIGH";
                        else if (isVpn || failed > 20) threatLevel = "MEDIUM";

                        return SecurityLogMetricsDto.FlaggedIpDto.builder()
                                .ipAddressPartial(maskIp((String) row[0]))
                                .geoCountry((String) row[1])
                                .geoCity((String) row[2])
                                .isVpn(isVpn)
                                .isTor(isTor)
                                .isProxy(isProxy)
                                .totalRequests(requests)
                                .failedAuthAttempts(failed)
                                .uniqueUsersFromIp(users)
                                .threatLevel(threatLevel)
                                .riskScore(riskCalculator.calculateIpThreatScore(isVpn, isTor, isProxy, failed, users))
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        return SecurityLogMetricsDto.IpSecurityMetricsDto.builder()
                .totalUniqueIps(totalIps)
                .vpnIps(vpnIps)
                .torIps(torIps)
                .proxyIps(proxyIps)
                .suspiciousIpRate(round(suspiciousRate))
                .flaggedIps(suspiciousIps)
                .flaggedIpsList(flaggedList)
                .build();
    }

    private SecurityLogMetricsDto.BruteForceMetricsDto collectBruteForceMetrics(
            LocalDateTime startDate, LocalDateTime endDate,
            int threshold, int windowMinutes,
            boolean includeDetails, int maxSize) {

        LocalDateTime windowStart = endDate.minusMinutes(windowMinutes);

        List<Object[]> bruteForceAttempts = authLogRepository.detectBruteForceByIp(
                windowStart, endDate, threshold);

        long totalAttempts = bruteForceAttempts.stream()
                .mapToLong(r -> ((Number) r[1]).longValue())
                .sum();

        long uniqueTargeted = bruteForceAttempts.stream()
                .mapToLong(r -> ((Number) r[2]).longValue())
                .sum();

        List<SecurityLogMetricsDto.BruteForceAttackDto> activeAttacks = new ArrayList<>();
        if (includeDetails) {
            int attackId = 0;
            for (Object[] row : bruteForceAttempts) {
                if (activeAttacks.size() >= maxSize) break;
                activeAttacks.add(SecurityLogMetricsDto.BruteForceAttackDto.builder()
                        .attackId("BF-" + (++attackId))
                        .ipAddress(maskIp((String) row[0]))
                        .attemptCount(((Number) row[1]).longValue())
                        .targetedAccounts(((Number) row[2]).longValue())
                        .startedAt((LocalDateTime) row[3])
                        .lastAttemptAt((LocalDateTime) row[4])
                        .attackPattern("PASSWORD_SPRAY")
                        .isBlocked(false)
                        .build());
            }
        }

        return SecurityLogMetricsDto.BruteForceMetricsDto.builder()
                .bruteForceAttempts(totalAttempts)
                .uniqueTargetedAccounts(uniqueTargeted)
                .uniqueAttackingIps((long) bruteForceAttempts.size())
                .activeAttacks(activeAttacks)
                .bruteForceThreshold(threshold)
                .timeWindowMinutes(windowMinutes)
                .build();
    }

    private double calculateSecurityHealthScore(
            SecurityLogMetricsDto.LoginSecurityMetricsDto login,
            SecurityLogMetricsDto.RateLimitMetricsDto rateLimit,
            SecurityLogMetricsDto.BruteForceMetricsDto bruteForce,
            SecurityLogMetricsDto.IpSecurityMetricsDto ipSecurity) {

        double score = 100.0;

        // Deduct for login failures
        if (login.getLoginFailureRate() != null) {
            score -= login.getLoginFailureRate() * 0.5;
        }

        // Deduct for rate limits
        if (rateLimit.getTotalRateLimitedRequests() != null) {
            score -= Math.min(rateLimit.getTotalRateLimitedRequests() * 0.01, 10);
        }

        // Deduct for brute force
        if (bruteForce.getBruteForceAttempts() != null && bruteForce.getBruteForceAttempts() > 0) {
            score -= 20;
        }

        // Deduct for suspicious IPs
        if (ipSecurity.getSuspiciousIpRate() != null) {
            score -= ipSecurity.getSuspiciousIpRate() * 0.3;
        }

        return Math.max(0, Math.min(100, score));
    }

    private String maskIp(String ip) {
        if (ip == null) return "****";
        String[] parts = ip.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1] + ".***";
        }
        return "****";
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
