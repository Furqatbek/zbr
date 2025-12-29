package com.fooddelivery.analytics.fraud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for security log metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityLogMetricsDto {

    // Login metrics
    private LoginSecurityMetricsDto loginMetrics;

    // Token metrics
    private TokenSecurityMetricsDto tokenMetrics;

    // Rate limiting metrics
    private RateLimitMetricsDto rateLimitMetrics;

    // IP security metrics
    private IpSecurityMetricsDto ipSecurityMetrics;

    // Device security metrics
    private DeviceSecurityMetricsDto deviceSecurityMetrics;

    // Brute force detection
    private BruteForceMetricsDto bruteForceMetrics;

    // Overall security posture
    private Long totalSecurityEvents;
    private Long criticalEvents;
    private Long highSeverityEvents;
    private Double securityHealthScore;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginSecurityMetricsDto {
        private Long totalLoginAttempts;
        private Long successfulLogins;
        private Long failedLogins;
        private Double loginFailureRate;
        private Double failedLoginRate; // Alias for loginFailureRate, used by FraudSummaryMapper
        private Long uniqueUsersWithFailedLogins;
        private Long uniqueIpsWithFailedLogins;
        private Long accountsLocked;
        private Map<String, Long> failuresByReason;
        private List<UserLoginRiskDto> highRiskUsers;
        private List<IpLoginRiskDto> highRiskIps;
        private Map<Integer, Long> failedLoginsByHour;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserLoginRiskDto {
        private Long userId;
        private String username;
        private Long failedAttempts;
        private Long successfulAttempts;
        private Double failureRate;
        private Long uniqueIpsUsed;
        private LocalDateTime lastFailedAt;
        private Boolean isLocked;
        private Integer riskScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IpLoginRiskDto {
        private String ipAddressPartial; // Masked
        private String geoLocation;
        private Long failedAttempts;
        private Long uniqueUsersTargeted;
        private Boolean isVpn;
        private Boolean isTor;
        private Boolean isProxy;
        private LocalDateTime firstSeenAt;
        private LocalDateTime lastSeenAt;
        private Integer riskScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenSecurityMetricsDto {
        private Long totalTokenRefreshAttempts;
        private Long failedTokenRefreshes;
        private Double tokenRefreshFailureRate;
        private Long expiredTokenUsageAttempts;
        private Long invalidTokenUsageAttempts;
        private Long suspiciousTokenActivity;
        private Map<String, Long> tokenErrorsByType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateLimitMetricsDto {
        private Long totalRateLimitedRequests;
        private Long totalRateLimitViolations; // Used by FraudSummaryMapper
        private Long uniqueRateLimitedUsers;
        private Long uniqueRateLimitedIps;
        private List<RateLimitedEntityDto> topRateLimitedUsers;
        private List<RateLimitedEntityDto> topRateLimitedIps;
        private Map<String, Long> rateLimitsByEndpoint;
        private Map<Integer, Long> rateLimitsByHour;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateLimitedEntityDto {
        private String identifier;
        private String type; // USER or IP
        private Long hitCount;
        private LocalDateTime firstHitAt;
        private LocalDateTime lastHitAt;
        private List<String> targetedEndpoints;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IpSecurityMetricsDto {
        private Long totalUniqueIps;
        private Long vpnIps;
        private Long torIps;
        private Long proxyIps;
        private Double suspiciousIpRate;
        private Long flaggedIps;
        private List<FlaggedIpDto> flaggedIpsList;
        private Long suspiciousIpCount;
        private List<SuspiciousIpDto> suspiciousIpsList;
        private Map<String, Long> ipsByCountry;
        private Map<String, Long> suspiciousIpsByCountry;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlaggedIpDto {
        private String ipAddressPartial;
        private String geoCountry;
        private String geoCity;
        private Boolean isVpn;
        private Boolean isTor;
        private Boolean isProxy;
        private Long totalRequests;
        private Long failedAuthAttempts;
        private Long uniqueUsersFromIp;
        private String threatLevel;
        private Integer riskScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceSecurityMetricsDto {
        private Long totalUniqueDevices;
        private Long deviceFingerprintMismatches;
        private Long suspiciousDeviceChanges;
        private Double deviceMismatchRate;
        private List<DeviceMismatchDto> deviceMismatches;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceMismatchDto {
        private Long userId;
        private String previousDeviceId;
        private String newDeviceId;
        private LocalDateTime changedAt;
        private String changeContext; // LOGIN, ORDER, etc.
        private Boolean isSuspicious;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BruteForceMetricsDto {
        private Long bruteForceAttempts;
        private Long uniqueTargetedAccounts;
        private Long uniqueAttackingIps;
        private Long blockedAttacks;
        private Long detectedAttacks;
        private List<BruteForceAttackDto> activeAttacks;
        private Integer bruteForceThreshold;
        private Integer timeWindowMinutes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BruteForceAttackDto {
        private String attackId;
        private String ipAddress;
        private Long targetedAccounts;
        private Long attemptCount;
        private LocalDateTime startedAt;
        private LocalDateTime lastAttemptAt;
        private String attackPattern;
        private Boolean isBlocked;
    }

    /**
     * Security flag DTO for flagged accounts/activities.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityFlagDto {
        private Long flagId;
        private String flagType;
        private String severityLevel;
        private LocalDateTime flaggedAt;
        private String description;
    }

    /**
     * Suspicious IP DTO for IP-based threat detection.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuspiciousIpDto {
        private String ipAddress;
        private Long failedAttempts;
        private Long uniqueUsersTargeted;
        private Boolean isVpn;
        private Boolean isTor;
        private Boolean isProxy;
        private LocalDateTime lastAttemptAt;
        private Integer threatScore;
    }
}
