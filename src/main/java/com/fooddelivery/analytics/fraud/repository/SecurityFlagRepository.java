package com.fooddelivery.analytics.fraud.repository;

import com.fooddelivery.analytics.fraud.model.SecurityFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for security flag queries.
 */
@Repository
public interface SecurityFlagRepository extends JpaRepository<SecurityFlag, Long> {

    // ==================== Basic Counts ====================

    @Query("SELECT COUNT(f) FROM SecurityFlag f WHERE f.createdAt BETWEEN :start AND :end")
    Long countTotalFlags(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(f) FROM SecurityFlag f " +
            "WHERE f.status IN ('ACTIVE', 'UNDER_REVIEW') AND f.createdAt BETWEEN :start AND :end")
    Long countActiveFlags(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT f.userId) FROM SecurityFlag f " +
            "WHERE f.status IN ('ACTIVE', 'UNDER_REVIEW') AND f.createdAt BETWEEN :start AND :end")
    Long countFlaggedUsers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Flags by Severity ====================

    @Query("SELECT f.severity, COUNT(f) FROM SecurityFlag f " +
            "WHERE f.createdAt BETWEEN :start AND :end " +
            "GROUP BY f.severity ORDER BY f.severity")
    List<Object[]> countFlagsBySeverity(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(f) FROM SecurityFlag f " +
            "WHERE f.severity = 'CRITICAL' AND f.status IN ('ACTIVE', 'UNDER_REVIEW') " +
            "AND f.createdAt BETWEEN :start AND :end")
    Long countCriticalFlags(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(f) FROM SecurityFlag f " +
            "WHERE f.severity = 'HIGH' AND f.status IN ('ACTIVE', 'UNDER_REVIEW') " +
            "AND f.createdAt BETWEEN :start AND :end")
    Long countHighSeverityFlags(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Flags by Type ====================

    @Query("SELECT f.flagType, COUNT(f) FROM SecurityFlag f " +
            "WHERE f.createdAt BETWEEN :start AND :end " +
            "GROUP BY f.flagType ORDER BY COUNT(f) DESC")
    List<Object[]> countFlagsByType(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Flags by Status ====================

    @Query("SELECT f.status, COUNT(f) FROM SecurityFlag f " +
            "WHERE f.createdAt BETWEEN :start AND :end " +
            "GROUP BY f.status")
    List<Object[]> countFlagsByStatus(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== High Risk Users ====================

    /**
     * Get high risk users (multiple active flags or high severity).
     * Returns: userId, flagCount, highestSeverity, flagTypes
     */
    @Query(value = "SELECT user_id, COUNT(*), MAX(severity), STRING_AGG(DISTINCT CAST(flag_type AS TEXT), ',') " +
            "FROM security_flags " +
            "WHERE status IN ('ACTIVE', 'UNDER_REVIEW') " +
            "AND created_at BETWEEN :start AND :end " +
            "GROUP BY user_id " +
            "HAVING COUNT(*) >= :minFlags OR MAX(severity) IN ('HIGH', 'CRITICAL') " +
            "ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> getHighRiskUsers(@Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end,
                                     @Param("minFlags") int minFlags);

    @Query("SELECT COUNT(DISTINCT f.userId) FROM SecurityFlag f " +
            "WHERE f.status IN ('ACTIVE', 'UNDER_REVIEW') " +
            "AND (f.severity IN ('HIGH', 'CRITICAL') OR f.userId IN (" +
            "  SELECT f2.userId FROM SecurityFlag f2 " +
            "  WHERE f2.status IN ('ACTIVE', 'UNDER_REVIEW') AND f2.createdAt BETWEEN :start AND :end " +
            "  GROUP BY f2.userId HAVING COUNT(f2) >= :minFlags)) " +
            "AND f.createdAt BETWEEN :start AND :end")
    Long countHighRiskUsers(@Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end,
                            @Param("minFlags") int minFlags);

    // ==================== Daily Trend ====================

    /**
     * Get daily flag trend.
     * Returns: date, paymentFraud, behavioralFraud, accountFraud, referralFraud, security
     */
    @Query("SELECT CAST(f.createdAt AS date), " +
            "SUM(CASE WHEN f.flagType IN ('FAILED_PAYMENT_SPIKE', 'PAYMENT_METHOD_ABUSE', 'CHARGEBACK_HISTORY') THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN f.flagType IN ('ABNORMAL_ORDER_PATTERN', 'VELOCITY_VIOLATION', 'ADDRESS_FRAUD', 'REFUND_ABUSE') THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN f.flagType IN ('FAKE_ACCOUNT', 'MULTI_ACCOUNT', 'ACCOUNT_TAKEOVER', 'IDENTITY_FRAUD') THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN f.flagType IN ('REFERRAL_ABUSE', 'PROMO_ABUSE', 'CIRCULAR_REFERRAL') THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN f.flagType IN ('BRUTE_FORCE_ATTACK', 'SUSPICIOUS_IP', 'DEVICE_MISMATCH', 'VPN_TOR_USAGE') THEN 1 ELSE 0 END) " +
            "FROM SecurityFlag f " +
            "WHERE f.createdAt BETWEEN :start AND :end " +
            "GROUP BY CAST(f.createdAt AS date) " +
            "ORDER BY CAST(f.createdAt AS date)")
    List<Object[]> getDailyFlagTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Detection Rule Analysis ====================

    /**
     * Get flag counts by detection rule.
     */
    @Query("SELECT f.detectionRule, COUNT(f) FROM SecurityFlag f " +
            "WHERE f.autoDetected = true AND f.createdAt BETWEEN :start AND :end " +
            "GROUP BY f.detectionRule ORDER BY COUNT(f) DESC")
    List<Object[]> countFlagsByDetectionRule(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Resolution Analysis ====================

    /**
     * Get false positive rate by flag type.
     * Returns: flagType, totalFlags, falsePositives, falsePositiveRate
     */
    @Query("SELECT f.flagType, COUNT(f), " +
            "SUM(CASE WHEN f.status = 'FALSE_POSITIVE' THEN 1 ELSE 0 END), " +
            "CAST(SUM(CASE WHEN f.status = 'FALSE_POSITIVE' THEN 1 ELSE 0 END) AS double) / COUNT(f) " +
            "FROM SecurityFlag f " +
            "WHERE f.createdAt BETWEEN :start AND :end " +
            "GROUP BY f.flagType " +
            "ORDER BY COUNT(f) DESC")
    List<Object[]> getFalsePositiveRateByType(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Category Specific Queries ====================

    @Query("SELECT COUNT(f) FROM SecurityFlag f " +
            "WHERE f.flagType IN ('FAILED_PAYMENT_SPIKE', 'PAYMENT_METHOD_ABUSE', 'CHARGEBACK_HISTORY') " +
            "AND f.createdAt BETWEEN :start AND :end")
    Long countPaymentFraudFlags(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(f) FROM SecurityFlag f " +
            "WHERE f.flagType IN ('ABNORMAL_ORDER_PATTERN', 'VELOCITY_VIOLATION', 'ADDRESS_FRAUD', 'REFUND_ABUSE') " +
            "AND f.createdAt BETWEEN :start AND :end")
    Long countBehavioralFraudFlags(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(f) FROM SecurityFlag f " +
            "WHERE f.flagType IN ('FAKE_ACCOUNT', 'MULTI_ACCOUNT', 'ACCOUNT_TAKEOVER', 'IDENTITY_FRAUD') " +
            "AND f.createdAt BETWEEN :start AND :end")
    Long countAccountFraudFlags(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(f) FROM SecurityFlag f " +
            "WHERE f.flagType IN ('REFERRAL_ABUSE', 'PROMO_ABUSE', 'CIRCULAR_REFERRAL') " +
            "AND f.createdAt BETWEEN :start AND :end")
    Long countReferralFraudFlags(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(f) FROM SecurityFlag f " +
            "WHERE f.flagType IN ('BRUTE_FORCE_ATTACK', 'SUSPICIOUS_IP', 'DEVICE_MISMATCH', 'VPN_TOR_USAGE') " +
            "AND f.createdAt BETWEEN :start AND :end")
    Long countSecurityFlags(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
