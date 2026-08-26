package com.fooddelivery.analytics.fraud.repository;

import com.fooddelivery.analytics.fraud.model.AuthLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for authentication log security queries.
 */
@Repository
public interface AuthLogRepository extends JpaRepository<AuthLog, Long> {

    // ==================== Login Metrics ====================

    @Query("SELECT COUNT(a) FROM AuthLog a WHERE a.authType = 'LOGIN' AND a.createdAt BETWEEN :start AND :end")
    Long countTotalLoginAttempts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM AuthLog a WHERE a.authType = 'LOGIN' AND a.status = 'SUCCESS' " +
            "AND a.createdAt BETWEEN :start AND :end")
    Long countSuccessfulLogins(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM AuthLog a WHERE a.authType = 'LOGIN' AND a.status = 'FAILED' " +
            "AND a.createdAt BETWEEN :start AND :end")
    Long countFailedLogins(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM AuthLog a WHERE a.status = 'LOCKED' AND a.createdAt BETWEEN :start AND :end")
    Long countLockedAccounts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM AuthLog a WHERE a.status = 'RATE_LIMITED' AND a.createdAt BETWEEN :start AND :end")
    Long countRateLimitedAttempts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== User-Level Security Queries ====================

    /**
     * Get users with high failed login attempts.
     * Returns: userId, username, failedCount, successCount, lastFailedAt
     */
    @Query("SELECT a.userId, MAX(a.username), " +
            "SUM(CASE WHEN a.status = 'FAILED' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN a.status = 'SUCCESS' THEN 1 ELSE 0 END), " +
            "MAX(CASE WHEN a.status = 'FAILED' THEN a.createdAt END) " +
            "FROM AuthLog a " +
            "WHERE a.authType = 'LOGIN' AND a.createdAt BETWEEN :start AND :end " +
            "GROUP BY a.userId " +
            "HAVING SUM(CASE WHEN a.status = 'FAILED' THEN 1 ELSE 0 END) >= :threshold " +
            "ORDER BY SUM(CASE WHEN a.status = 'FAILED' THEN 1 ELSE 0 END) DESC")
    List<Object[]> getUsersWithHighFailedLogins(@Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end,
                                                 @Param("threshold") int threshold);

    @Query("SELECT COUNT(DISTINCT a.userId) FROM AuthLog a " +
            "WHERE a.authType = 'LOGIN' AND a.status = 'FAILED' " +
            "AND a.createdAt BETWEEN :start AND :end")
    Long countUniqueUsersWithFailedLogins(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== IP-Level Security Queries ====================

    /**
     * Get IPs with high failed login attempts.
     * Returns: ipAddress, geoCountry, geoCity, failedCount, uniqueUsersTargeted, isVpn, isTor
     */
    @Query("SELECT a.ipAddress, MAX(a.geoCountry), MAX(a.geoCity), " +
            "SUM(CASE WHEN a.status = 'FAILED' THEN 1 ELSE 0 END), " +
            "COUNT(DISTINCT a.userId), " +
            "MAX(CASE WHEN a.isVpn = true THEN 1 ELSE 0 END), " +
            "MAX(CASE WHEN a.isTor = true THEN 1 ELSE 0 END) " +
            "FROM AuthLog a " +
            "WHERE a.authType = 'LOGIN' AND a.createdAt BETWEEN :start AND :end " +
            "GROUP BY a.ipAddress " +
            "HAVING SUM(CASE WHEN a.status = 'FAILED' THEN 1 ELSE 0 END) >= :threshold " +
            "ORDER BY SUM(CASE WHEN a.status = 'FAILED' THEN 1 ELSE 0 END) DESC")
    List<Object[]> getIpsWithHighFailedLogins(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end,
                                               @Param("threshold") int threshold);

    @Query("SELECT COUNT(DISTINCT a.ipAddress) FROM AuthLog a " +
            "WHERE a.authType = 'LOGIN' AND a.status = 'FAILED' " +
            "AND a.createdAt BETWEEN :start AND :end")
    Long countUniqueIpsWithFailedLogins(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== VPN/TOR/Proxy Detection ====================

    @Query("SELECT COUNT(DISTINCT a.ipAddress) FROM AuthLog a " +
            "WHERE a.isVpn = true AND a.createdAt BETWEEN :start AND :end")
    Long countVpnIps(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT a.ipAddress) FROM AuthLog a " +
            "WHERE a.isTor = true AND a.createdAt BETWEEN :start AND :end")
    Long countTorIps(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT a.ipAddress) FROM AuthLog a " +
            "WHERE a.isProxy = true AND a.createdAt BETWEEN :start AND :end")
    Long countProxyIps(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT a.ipAddress) FROM AuthLog a " +
            "WHERE (a.isVpn = true OR a.isTor = true OR a.isProxy = true) " +
            "AND a.createdAt BETWEEN :start AND :end")
    Long countSuspiciousIps(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get flagged IPs with details.
     * Returns: ipAddress, geoCountry, geoCity, isVpn, isTor, isProxy, totalRequests, failedAuth, uniqueUsers
     */
    @Query("SELECT a.ipAddress, MAX(a.geoCountry), MAX(a.geoCity), " +
            "MAX(CASE WHEN a.isVpn = true THEN 1 ELSE 0 END), " +
            "MAX(CASE WHEN a.isTor = true THEN 1 ELSE 0 END), " +
            "MAX(CASE WHEN a.isProxy = true THEN 1 ELSE 0 END), " +
            "COUNT(a), " +
            "SUM(CASE WHEN a.status = 'FAILED' THEN 1 ELSE 0 END), " +
            "COUNT(DISTINCT a.userId) " +
            "FROM AuthLog a " +
            "WHERE (a.isVpn = true OR a.isTor = true OR a.isProxy = true) " +
            "AND a.createdAt BETWEEN :start AND :end " +
            "GROUP BY a.ipAddress " +
            "ORDER BY COUNT(a) DESC")
    List<Object[]> getFlaggedIpDetails(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Token Security Queries ====================

    @Query("SELECT COUNT(a) FROM AuthLog a WHERE a.authType = 'TOKEN_REFRESH' " +
            "AND a.createdAt BETWEEN :start AND :end")
    Long countTotalTokenRefreshAttempts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM AuthLog a WHERE a.authType = 'TOKEN_REFRESH' " +
            "AND a.status IN ('FAILED', 'TOKEN_EXPIRED', 'TOKEN_INVALID') " +
            "AND a.createdAt BETWEEN :start AND :end")
    Long countFailedTokenRefreshes(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get token error breakdown.
     * Returns: status, count
     */
    @Query("SELECT a.status, COUNT(a) FROM AuthLog a " +
            "WHERE a.authType = 'TOKEN_REFRESH' AND a.status != 'SUCCESS' " +
            "AND a.createdAt BETWEEN :start AND :end " +
            "GROUP BY a.status ORDER BY COUNT(a) DESC")
    List<Object[]> getTokenErrorBreakdown(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Brute Force Detection ====================

    /**
     * Detect brute force attacks by IP in time window.
     * Returns: ipAddress, attemptCount, uniqueTargets, firstAttempt, lastAttempt
     */
    @Query("SELECT a.ipAddress, COUNT(a), COUNT(DISTINCT a.userId), MIN(a.createdAt), MAX(a.createdAt) " +
            "FROM AuthLog a " +
            "WHERE a.authType = 'LOGIN' AND a.status = 'FAILED' " +
            "AND a.createdAt BETWEEN :windowStart AND :windowEnd " +
            "GROUP BY a.ipAddress " +
            "HAVING COUNT(a) >= :threshold " +
            "ORDER BY COUNT(a) DESC")
    List<Object[]> detectBruteForceByIp(@Param("windowStart") LocalDateTime windowStart,
                                         @Param("windowEnd") LocalDateTime windowEnd,
                                         @Param("threshold") int threshold);

    /**
     * Detect credential stuffing (many users from same IP in short time).
     * Returns: ipAddress, uniqueUsersAttempted, totalAttempts, successCount
     */
    @Query("SELECT a.ipAddress, COUNT(DISTINCT a.userId), COUNT(a), " +
            "SUM(CASE WHEN a.status = 'SUCCESS' THEN 1 ELSE 0 END) " +
            "FROM AuthLog a " +
            "WHERE a.authType = 'LOGIN' AND a.createdAt BETWEEN :windowStart AND :windowEnd " +
            "GROUP BY a.ipAddress " +
            "HAVING COUNT(DISTINCT a.userId) >= :userThreshold " +
            "ORDER BY COUNT(DISTINCT a.userId) DESC")
    List<Object[]> detectCredentialStuffing(@Param("windowStart") LocalDateTime windowStart,
                                             @Param("windowEnd") LocalDateTime windowEnd,
                                             @Param("userThreshold") int userThreshold);

    // ==================== Rate Limit Queries ====================

    /**
     * Get top rate limited users.
     * Returns: userId, hitCount, firstHit, lastHit
     */
    @Query("SELECT a.userId, COUNT(a), MIN(a.createdAt), MAX(a.createdAt) " +
            "FROM AuthLog a " +
            "WHERE a.status = 'RATE_LIMITED' AND a.createdAt BETWEEN :start AND :end " +
            "GROUP BY a.userId " +
            "ORDER BY COUNT(a) DESC")
    List<Object[]> getTopRateLimitedUsers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get top rate limited IPs.
     * Returns: ipAddress, hitCount, firstHit, lastHit
     */
    @Query("SELECT a.ipAddress, COUNT(a), MIN(a.createdAt), MAX(a.createdAt) " +
            "FROM AuthLog a " +
            "WHERE a.status = 'RATE_LIMITED' AND a.createdAt BETWEEN :start AND :end " +
            "GROUP BY a.ipAddress " +
            "ORDER BY COUNT(a) DESC")
    List<Object[]> getTopRateLimitedIps(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT a.userId) FROM AuthLog a " +
            "WHERE a.status = 'RATE_LIMITED' AND a.createdAt BETWEEN :start AND :end")
    Long countUniqueRateLimitedUsers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT a.ipAddress) FROM AuthLog a " +
            "WHERE a.status = 'RATE_LIMITED' AND a.createdAt BETWEEN :start AND :end")
    Long countUniqueRateLimitedIps(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Hourly Distribution ====================

    /**
     * Get hourly failed login distribution.
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM business_ts(created_at)), COUNT(*) FROM auth_logs " +
            "WHERE auth_type = 'LOGIN' AND status = 'FAILED' " +
            "AND created_at BETWEEN :start AND :end " +
            "GROUP BY EXTRACT(HOUR FROM business_ts(created_at)) " +
            "ORDER BY EXTRACT(HOUR FROM business_ts(created_at))", nativeQuery = true)
    List<Object[]> getHourlyFailedLogins(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Failure Reason Breakdown ====================

    @Query("SELECT a.failureReason, COUNT(a) FROM AuthLog a " +
            "WHERE a.status = 'FAILED' AND a.createdAt BETWEEN :start AND :end " +
            "GROUP BY a.failureReason ORDER BY COUNT(a) DESC")
    List<Object[]> getFailuresByReason(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT a.ipAddress) FROM AuthLog a WHERE a.createdAt BETWEEN :start AND :end")
    Long countTotalUniqueIps(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== User-Specific Security Queries ====================

    /**
     * Count failed login attempts for a specific user.
     */
    @Query("SELECT COUNT(a) FROM AuthLog a " +
            "WHERE a.userId = :userId AND a.authType = 'LOGIN' AND a.status = 'FAILED' " +
            "AND a.createdAt BETWEEN :start AND :end")
    Long countFailedLoginsByUser(@Param("userId") Long userId,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    /**
     * Count total login attempts for a specific user.
     */
    @Query("SELECT COUNT(a) FROM AuthLog a " +
            "WHERE a.userId = :userId AND a.authType = 'LOGIN' " +
            "AND a.createdAt BETWEEN :start AND :end")
    Long countTotalLoginsByUser(@Param("userId") Long userId,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    /**
     * Check if user has used VPN.
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM AuthLog a " +
            "WHERE a.userId = :userId AND a.isVpn = true " +
            "AND a.createdAt BETWEEN :start AND :end")
    Boolean hasVpnUsage(@Param("userId") Long userId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

    /**
     * Check if user has used Tor.
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM AuthLog a " +
            "WHERE a.userId = :userId AND a.isTor = true " +
            "AND a.createdAt BETWEEN :start AND :end")
    Boolean hasTorUsage(@Param("userId") Long userId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

    /**
     * Check if user has rate limit violations.
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM AuthLog a " +
            "WHERE a.userId = :userId AND a.status = 'RATE_LIMITED' " +
            "AND a.createdAt BETWEEN :start AND :end")
    Boolean hasRateLimitViolations(@Param("userId") Long userId,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);
}
