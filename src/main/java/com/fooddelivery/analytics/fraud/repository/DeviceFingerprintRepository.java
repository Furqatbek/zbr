package com.fooddelivery.analytics.fraud.repository;

import com.fooddelivery.analytics.fraud.model.DeviceFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for device fingerprint fraud detection queries.
 */
@Repository
public interface DeviceFingerprintRepository extends JpaRepository<DeviceFingerprint, Long> {

    // ==================== Basic Counts ====================

    @Query("SELECT COUNT(d) FROM DeviceFingerprint d WHERE d.firstSeenAt BETWEEN :start AND :end")
    Long countTotalDevices(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(d) FROM DeviceFingerprint d " +
            "WHERE d.isEmulator = true AND d.firstSeenAt BETWEEN :start AND :end")
    Long countEmulatorDevices(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(d) FROM DeviceFingerprint d " +
            "WHERE d.isRooted = true AND d.firstSeenAt BETWEEN :start AND :end")
    Long countRootedDevices(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(d) FROM DeviceFingerprint d " +
            "WHERE d.trustScore IS NOT NULL AND d.trustScore < :threshold " +
            "AND d.firstSeenAt BETWEEN :start AND :end")
    Long countLowTrustDevices(@Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end,
                               @Param("threshold") int threshold);

    // ==================== Multi-User Device Detection ====================

    /**
     * Get devices shared across multiple users.
     * Returns: deviceId, fingerprintHash, userCount, userIds, isEmulator, isRooted, trustScore
     */
    @Query(value = "SELECT device_id, fingerprint_hash, COUNT(DISTINCT user_id), " +
            "STRING_AGG(DISTINCT CAST(user_id AS VARCHAR), ','), " +
            "MAX(CASE WHEN is_emulator = true THEN 1 ELSE 0 END), " +
            "MAX(CASE WHEN is_rooted = true THEN 1 ELSE 0 END), " +
            "MIN(trust_score) " +
            "FROM device_fingerprints " +
            "WHERE first_seen_at BETWEEN :start AND :end " +
            "GROUP BY device_id, fingerprint_hash " +
            "HAVING COUNT(DISTINCT user_id) >= :threshold " +
            "ORDER BY COUNT(DISTINCT user_id) DESC", nativeQuery = true)
    List<Object[]> getDevicesSharedAcrossUsers(@Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end,
                                                @Param("threshold") int threshold);

    @Query("SELECT COUNT(DISTINCT d.deviceId) FROM DeviceFingerprint d " +
            "WHERE d.firstSeenAt BETWEEN :start AND :end " +
            "AND d.deviceId IN (" +
            "  SELECT d2.deviceId FROM DeviceFingerprint d2 " +
            "  WHERE d2.firstSeenAt BETWEEN :start AND :end " +
            "  GROUP BY d2.deviceId HAVING COUNT(DISTINCT d2.userId) >= :threshold)")
    Long countDevicesSharedAcrossUsers(@Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end,
                                        @Param("threshold") int threshold);

    // ==================== Suspicious Device Detection ====================

    /**
     * Get suspicious devices (emulator, rooted, or low trust).
     * Returns: deviceId, userId, deviceType, isEmulator, isRooted, trustScore, firstSeenAt
     */
    @Query("SELECT d.deviceId, d.userId, d.deviceType, d.isEmulator, d.isRooted, d.trustScore, d.firstSeenAt " +
            "FROM DeviceFingerprint d " +
            "WHERE (d.isEmulator = true OR d.isRooted = true OR (d.trustScore IS NOT NULL AND d.trustScore < :trustThreshold)) " +
            "AND d.firstSeenAt BETWEEN :start AND :end " +
            "ORDER BY d.trustScore ASC NULLS LAST")
    List<Object[]> getSuspiciousDevices(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end,
                                         @Param("trustThreshold") int trustThreshold);

    @Query("SELECT COUNT(DISTINCT d.deviceId) FROM DeviceFingerprint d " +
            "WHERE (d.isEmulator = true OR d.isRooted = true OR (d.trustScore IS NOT NULL AND d.trustScore < :trustThreshold)) " +
            "AND d.firstSeenAt BETWEEN :start AND :end")
    Long countSuspiciousDevices(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end,
                                 @Param("trustThreshold") int trustThreshold);

    // ==================== Device Trust Distribution ====================

    /**
     * Get device counts by trust level.
     * Returns: trustLevel, count
     */
    @Query("SELECT CASE " +
            "  WHEN d.trustScore >= 80 THEN 'HIGH' " +
            "  WHEN d.trustScore >= 50 THEN 'MEDIUM' " +
            "  WHEN d.trustScore >= 20 THEN 'LOW' " +
            "  ELSE 'VERY_LOW' END, " +
            "COUNT(d) " +
            "FROM DeviceFingerprint d " +
            "WHERE d.trustScore IS NOT NULL AND d.firstSeenAt BETWEEN :start AND :end " +
            "GROUP BY CASE " +
            "  WHEN d.trustScore >= 80 THEN 'HIGH' " +
            "  WHEN d.trustScore >= 50 THEN 'MEDIUM' " +
            "  WHEN d.trustScore >= 20 THEN 'LOW' " +
            "  ELSE 'VERY_LOW' END")
    List<Object[]> getDevicesByTrustLevel(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Device Reuse Analysis ====================

    /**
     * Calculate device reuse rate.
     * Returns: totalDevices, reusedDevices
     */
    @Query("SELECT COUNT(DISTINCT d.deviceId), " +
            "(SELECT COUNT(DISTINCT d2.deviceId) FROM DeviceFingerprint d2 " +
            "  WHERE d2.firstSeenAt BETWEEN :start AND :end " +
            "  AND d2.deviceId IN (" +
            "    SELECT d3.deviceId FROM DeviceFingerprint d3 " +
            "    WHERE d3.firstSeenAt BETWEEN :start AND :end " +
            "    GROUP BY d3.deviceId HAVING COUNT(DISTINCT d3.userId) > 1)) " +
            "FROM DeviceFingerprint d WHERE d.firstSeenAt BETWEEN :start AND :end")
    Object[] getDeviceReuseStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== User Device Correlation ====================

    /**
     * Get users with multiple devices.
     * Returns: userId, deviceCount
     */
    @Query("SELECT d.userId, COUNT(DISTINCT d.deviceId) FROM DeviceFingerprint d " +
            "WHERE d.firstSeenAt BETWEEN :start AND :end " +
            "GROUP BY d.userId " +
            "HAVING COUNT(DISTINCT d.deviceId) > 1 " +
            "ORDER BY COUNT(DISTINCT d.deviceId) DESC")
    List<Object[]> getUsersWithMultipleDevices(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Fingerprint Clustering ====================

    /**
     * Get fingerprint clusters (same fingerprint across devices/users).
     * Returns: fingerprintHash, deviceCount, userCount
     */
    @Query("SELECT d.fingerprintHash, COUNT(DISTINCT d.deviceId), COUNT(DISTINCT d.userId) " +
            "FROM DeviceFingerprint d " +
            "WHERE d.fingerprintHash IS NOT NULL AND d.firstSeenAt BETWEEN :start AND :end " +
            "GROUP BY d.fingerprintHash " +
            "HAVING COUNT(DISTINCT d.userId) > 1 " +
            "ORDER BY COUNT(DISTINCT d.userId) DESC")
    List<Object[]> getFingerprintClusters(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== User-Specific Device Queries ====================

    /**
     * Count distinct devices used by a specific user.
     */
    @Query("SELECT COUNT(DISTINCT d.deviceId) FROM DeviceFingerprint d " +
            "WHERE d.userId = :userId AND d.firstSeenAt BETWEEN :start AND :end")
    Long countUserDevices(@Param("userId") Long userId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    /**
     * Check if user has shared a device with other users.
     */
    @Query("SELECT CASE WHEN COUNT(DISTINCT d2.userId) > 1 THEN true ELSE false END " +
            "FROM DeviceFingerprint d " +
            "JOIN DeviceFingerprint d2 ON d.deviceId = d2.deviceId " +
            "WHERE d.userId = :userId AND d.firstSeenAt BETWEEN :start AND :end")
    Boolean hasSharedDevice(@Param("userId") Long userId,
                            @Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);
}
