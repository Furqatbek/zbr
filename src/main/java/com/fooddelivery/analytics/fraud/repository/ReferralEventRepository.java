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
    @Query(value = "SELECT device_id, COUNT(*), STRING_AGG(CAST(referred_user_id AS TEXT), ',') " +
            "FROM referral_events " +
            "WHERE device_id IS NOT NULL AND created_at BETWEEN :start AND :end " +
            "GROUP BY device_id " +
            "HAVING COUNT(*) >= :threshold " +
            "ORDER BY COUNT(*) DESC", nativeQuery = true)
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
    @Query(value = "SELECT device_fingerprint, device_id, COUNT(*), STRING_AGG(CAST(referred_user_id AS TEXT), ',') " +
            "FROM referral_events " +
            "WHERE device_fingerprint IS NOT NULL AND created_at BETWEEN :start AND :end " +
            "GROUP BY device_fingerprint, device_id " +
            "HAVING COUNT(*) >= :threshold " +
            "ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> getDeviceFingerprintClusters(@Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end,
                                                 @Param("threshold") int threshold);

    // ==================== IP-Based Fraud Detection ====================

    /**
     * Detect IPs with multiple signups.
     * Returns: ipAddress, signupCount, referredUserIds
     */
    @Query(value = "SELECT ip_address, COUNT(*), STRING_AGG(CAST(referred_user_id AS TEXT), ',') " +
            "FROM referral_events " +
            "WHERE ip_address IS NOT NULL AND created_at BETWEEN :start AND :end " +
            "GROUP BY ip_address " +
            "HAVING COUNT(*) >= :threshold " +
            "ORDER BY COUNT(*) DESC", nativeQuery = true)
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
    @Query(value = "SELECT u.user_id, " +
            "(SELECT COUNT(*) FROM referral_events r1 WHERE r1.referrer_id = u.user_id AND r1.created_at BETWEEN :start AND :end), " +
            "(SELECT COUNT(*) FROM referral_events r2 WHERE r2.referred_user_id = u.user_id AND r2.created_at BETWEEN :start AND :end) " +
            "FROM (SELECT DISTINCT referrer_id as user_id FROM referral_events WHERE created_at BETWEEN :start AND :end " +
            "      UNION SELECT DISTINCT referred_user_id FROM referral_events WHERE created_at BETWEEN :start AND :end) u", nativeQuery = true)
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
    @Query(value = "SELECT phone_number_hash, COUNT(DISTINCT referred_user_id), " +
            "STRING_AGG(CAST(referred_user_id AS TEXT), ',') " +
            "FROM referral_events " +
            "WHERE phone_number_hash IS NOT NULL AND created_at BETWEEN :start AND :end " +
            "GROUP BY phone_number_hash " +
            "HAVING COUNT(DISTINCT referred_user_id) > 1 " +
            "ORDER BY COUNT(DISTINCT referred_user_id) DESC", nativeQuery = true)
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
    @Query(value = "SELECT SUBSTRING(device_id, 1, POSITION('-' IN device_id) - 1), COUNT(*) " +
            "FROM referral_events " +
            "WHERE device_id IS NOT NULL AND created_at BETWEEN :start AND :end " +
            "GROUP BY SUBSTRING(device_id, 1, POSITION('-' IN device_id) - 1)", nativeQuery = true)
    List<Object[]> getSignupsByDeviceType(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== User-Specific Referral Queries ====================

    /**
     * Count total referrals made by a specific referrer.
     */
    @Query("SELECT COUNT(r) FROM ReferralEvent r WHERE r.referrerId = :referrerId")
    Long countByReferrerId(@Param("referrerId") Long referrerId);

    /**
     * Count suspicious referrals made by a specific referrer in date range.
     */
    @Query("SELECT COUNT(r) FROM ReferralEvent r " +
            "WHERE r.referrerId = :referrerId AND r.isSuspicious = true " +
            "AND r.createdAt BETWEEN :start AND :end")
    Long countSuspiciousReferralsByReferrer(@Param("referrerId") Long referrerId,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);
}
