package com.fooddelivery.analytics.fraud.repository;

import com.fooddelivery.analytics.fraud.model.ReferralEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for referral fraud detection queries.
 */
@Repository
public interface ReferralEventRepository extends JpaRepository<ReferralEvent, Long> {

    // ==================== Basic Counts ====================

    @Query("SELECT COUNT(r) FROM ReferralEvent r WHERE r.createdAt BETWEEN :start AND :end")
    Long countTotalReferrals(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(r) FROM ReferralEvent r WHERE r.status = 'COMPLETED' " +
            "AND r.createdAt BETWEEN :start AND :end")
    Long countCompletedReferrals(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(r) FROM ReferralEvent r WHERE r.isSuspicious = true " +
            "AND r.createdAt BETWEEN :start AND :end")
    Long countSuspiciousReferrals(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(r) FROM ReferralEvent r WHERE r.status = 'FRAUD_DETECTED' " +
            "AND r.createdAt BETWEEN :start AND :end")
    Long countFraudDetectedReferrals(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Device-Based Fraud Detection ====================

    /**
     * Detect devices with multiple signups.
     * Returns: deviceId, signupCount, referredUserIds as comma-separated
     */
    @Query("SELECT r.deviceId, COUNT(r), STRING_AGG(CAST(r.referredUserId AS string), ',') " +
            "FROM ReferralEvent r " +
            "WHERE r.deviceId IS NOT NULL AND r.createdAt BETWEEN :start AND :end " +
            "GROUP BY r.deviceId " +
            "HAVING COUNT(r) >= :threshold " +
            "ORDER BY COUNT(r) DESC")
    List<Object[]> getDevicesWithMultipleSignups(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end,
                                                  @Param("threshold") int threshold);

    @Query("SELECT COUNT(DISTINCT r.deviceId) FROM ReferralEvent r " +
            "WHERE r.deviceId IS NOT NULL AND r.createdAt BETWEEN :start AND :end " +
            "AND r.deviceId IN (" +
            "  SELECT r2.deviceId FROM ReferralEvent r2 " +
            "  WHERE r2.createdAt BETWEEN :start AND :end " +
            "  GROUP BY r2.deviceId HAVING COUNT(r2) >= :threshold)")
    Long countDevicesWithMultipleSignups(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          @Param("threshold") int threshold);

    /**
     * Get device fingerprint clusters.
     * Returns: fingerprintHash, deviceId, signupCount, referredUserIds
     */
    @Query("SELECT r.deviceFingerprint, r.deviceId, COUNT(r), STRING_AGG(CAST(r.referredUserId AS string), ',') " +
            "FROM ReferralEvent r " +
            "WHERE r.deviceFingerprint IS NOT NULL AND r.createdAt BETWEEN :start AND :end " +
            "GROUP BY r.deviceFingerprint, r.deviceId " +
            "HAVING COUNT(r) >= :threshold " +
            "ORDER BY COUNT(r) DESC")
    List<Object[]> getDeviceFingerprintClusters(@Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end,
                                                 @Param("threshold") int threshold);

    // ==================== IP-Based Fraud Detection ====================

    /**
     * Detect IPs with multiple signups.
     * Returns: ipAddress, signupCount, referredUserIds
     */
    @Query("SELECT r.ipAddress, COUNT(r), STRING_AGG(CAST(r.referredUserId AS string), ',') " +
            "FROM ReferralEvent r " +
            "WHERE r.ipAddress IS NOT NULL AND r.createdAt BETWEEN :start AND :end " +
            "GROUP BY r.ipAddress " +
            "HAVING COUNT(r) >= :threshold " +
            "ORDER BY COUNT(r) DESC")
    List<Object[]> getIpsWithMultipleSignups(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end,
                                              @Param("threshold") int threshold);

    @Query("SELECT COUNT(DISTINCT r.ipAddress) FROM ReferralEvent r " +
            "WHERE r.ipAddress IS NOT NULL AND r.createdAt BETWEEN :start AND :end " +
            "AND r.ipAddress IN (" +
            "  SELECT r2.ipAddress FROM ReferralEvent r2 " +
            "  WHERE r2.createdAt BETWEEN :start AND :end " +
            "  GROUP BY r2.ipAddress HAVING COUNT(r2) >= :threshold)")
    Long countIpsWithMultipleSignups(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end,
                                      @Param("threshold") int threshold);

    // ==================== Circular Referral Detection ====================

    /**
     * Get referral chains for circular detection.
     * Returns: referrerId, referredUserId
     */
    @Query("SELECT r.referrerId, r.referredUserId FROM ReferralEvent r " +
            "WHERE r.status = 'COMPLETED' AND r.createdAt BETWEEN :start AND :end")
    List<Object[]> getReferralChains(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Check if user has been both referrer and referred.
     * Returns: userId, asReferrer, asReferred
     */
    @Query("SELECT u.userId, " +
            "(SELECT COUNT(r1) FROM ReferralEvent r1 WHERE r1.referrerId = u.userId AND r1.createdAt BETWEEN :start AND :end), " +
            "(SELECT COUNT(r2) FROM ReferralEvent r2 WHERE r2.referredUserId = u.userId AND r2.createdAt BETWEEN :start AND :end) " +
            "FROM (SELECT DISTINCT r.referrerId as userId FROM ReferralEvent r WHERE r.createdAt BETWEEN :start AND :end " +
            "      UNION SELECT DISTINCT r.referredUserId FROM ReferralEvent r WHERE r.createdAt BETWEEN :start AND :end) u")
    List<Object[]> getUsersInBothRoles(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Fraudulent Referrers ====================

    /**
     * Get top referrers with suspicious activity.
     * Returns: referrerId, totalReferrals, suspiciousCount, completedCount, totalBonuses
     */
    @Query("SELECT r.referrerId, COUNT(r), " +
            "SUM(CASE WHEN r.isSuspicious = true THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN r.status = 'COMPLETED' THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(r.referrerBonus), 0) " +
            "FROM ReferralEvent r " +
            "WHERE r.createdAt BETWEEN :start AND :end " +
            "GROUP BY r.referrerId " +
            "HAVING SUM(CASE WHEN r.isSuspicious = true THEN 1 ELSE 0 END) > 0 " +
            "ORDER BY SUM(CASE WHEN r.isSuspicious = true THEN 1 ELSE 0 END) DESC")
    List<Object[]> getTopFraudulentReferrers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Phone/Email Fraud Detection ====================

    /**
     * Detect phone number reuse.
     * Returns: phoneHash, userCount, userIds
     */
    @Query("SELECT r.phoneNumberHash, COUNT(DISTINCT r.referredUserId), " +
            "STRING_AGG(CAST(r.referredUserId AS string), ',') " +
            "FROM ReferralEvent r " +
            "WHERE r.phoneNumberHash IS NOT NULL AND r.createdAt BETWEEN :start AND :end " +
            "GROUP BY r.phoneNumberHash " +
            "HAVING COUNT(DISTINCT r.referredUserId) > 1 " +
            "ORDER BY COUNT(DISTINCT r.referredUserId) DESC")
    List<Object[]> detectPhoneNumberReuse(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Financial Impact ====================

    @Query("SELECT COALESCE(SUM(r.referrerBonus), 0) + COALESCE(SUM(r.referredBonus), 0) " +
            "FROM ReferralEvent r WHERE r.status = 'COMPLETED' AND r.createdAt BETWEEN :start AND :end")
    BigDecimal getTotalReferralBonusesPaid(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(r.referrerBonus), 0) + COALESCE(SUM(r.referredBonus), 0) " +
            "FROM ReferralEvent r WHERE r.isSuspicious = true AND r.createdAt BETWEEN :start AND :end")
    BigDecimal getSuspectedFraudulentBonuses(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Signup Pattern Analysis ====================

    /**
     * Get signups by device type.
     */
    @Query("SELECT SUBSTRING(r.deviceId, 1, POSITION('-' IN r.deviceId) - 1), COUNT(r) " +
            "FROM ReferralEvent r " +
            "WHERE r.deviceId IS NOT NULL AND r.createdAt BETWEEN :start AND :end " +
            "GROUP BY SUBSTRING(r.deviceId, 1, POSITION('-' IN r.deviceId) - 1)")
    List<Object[]> getSignupsByDeviceType(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
