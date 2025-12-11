package com.fooddelivery.analytics.fraud.repository;

import com.fooddelivery.analytics.fraud.model.PromoUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for promo abuse detection queries.
 */
@Repository
public interface PromoUsageLogRepository extends JpaRepository<PromoUsageLog, Long> {

    // ==================== Basic Counts ====================

    @Query("SELECT COUNT(DISTINCT p.userId) FROM PromoUsageLog p WHERE p.createdAt BETWEEN :start AND :end")
    Long countTotalPromoUsers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(p.discountValue), 0) FROM PromoUsageLog p " +
            "WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :start AND :end")
    BigDecimal getTotalPromoCreditsUsed(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Promo-Only Users Detection ====================

    /**
     * Get users who only use promo codes (never paid).
     * Returns: userId, totalOrders, promoOrders, totalSpent, totalPromo, netPaid
     */
    @Query("SELECT p.userId, COUNT(p), " +
            "COUNT(p), " +
            "COALESCE(SUM(p.orderTotal), 0), " +
            "COALESCE(SUM(p.discountValue), 0), " +
            "COALESCE(SUM(p.userPaidAmount), 0) " +
            "FROM PromoUsageLog p " +
            "WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :start AND :end " +
            "GROUP BY p.userId " +
            "HAVING COALESCE(SUM(p.userPaidAmount), 0) = 0 AND COUNT(p) >= :minOrders " +
            "ORDER BY COUNT(p) DESC")
    List<Object[]> getPromoOnlyUsers(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end,
                                      @Param("minOrders") int minOrders);

    /**
     * Count users with no paying orders.
     */
    @Query("SELECT COUNT(DISTINCT p.userId) FROM PromoUsageLog p " +
            "WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :start AND :end " +
            "AND p.userId NOT IN (" +
            "  SELECT DISTINCT p2.userId FROM PromoUsageLog p2 " +
            "  WHERE p2.userPaidAmount > 0 AND p2.createdAt BETWEEN :start AND :end)")
    Long countUsersWithNoPayingOrders(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Promo Abuse Detection ====================

    /**
     * Get top promo abusers (high promo usage, low actual payment).
     * Returns: userId, totalOrders, totalSpent, totalPromo, netPaid, promoRate
     */
    @Query("SELECT p.userId, COUNT(p), " +
            "COALESCE(SUM(p.orderTotal), 0), " +
            "COALESCE(SUM(p.discountValue), 0), " +
            "COALESCE(SUM(p.userPaidAmount), 0) " +
            "FROM PromoUsageLog p " +
            "WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :start AND :end " +
            "GROUP BY p.userId " +
            "HAVING COUNT(p) >= :minOrders " +
            "ORDER BY COALESCE(SUM(p.discountValue), 0) DESC")
    List<Object[]> getTopPromoAbusers(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end,
                                       @Param("minOrders") int minOrders);

    // ==================== Promo Type Analysis ====================

    /**
     * Get abuse counts by promo type.
     */
    @Query("SELECT p.promoType, COUNT(p), SUM(p.discountValue) FROM PromoUsageLog p " +
            "WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :start AND :end " +
            "GROUP BY p.promoType ORDER BY SUM(p.discountValue) DESC")
    List<Object[]> getUsageByPromoType(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get referral promo abuse.
     */
    @Query("SELECT p.userId, COUNT(p), SUM(p.discountValue) FROM PromoUsageLog p " +
            "WHERE p.isReferralPromo = true AND p.status = 'COMPLETED' " +
            "AND p.createdAt BETWEEN :start AND :end " +
            "GROUP BY p.userId " +
            "HAVING COUNT(p) >= :threshold " +
            "ORDER BY COUNT(p) DESC")
    List<Object[]> getReferralPromoAbusers(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end,
                                            @Param("threshold") int threshold);

    // ==================== Device/IP Correlation ====================

    /**
     * Detect promo abuse from same device.
     * Returns: deviceId, userCount, totalPromoUsed
     */
    @Query("SELECT p.deviceId, COUNT(DISTINCT p.userId), SUM(p.discountValue) " +
            "FROM PromoUsageLog p " +
            "WHERE p.deviceId IS NOT NULL AND p.status = 'COMPLETED' " +
            "AND p.createdAt BETWEEN :start AND :end " +
            "GROUP BY p.deviceId " +
            "HAVING COUNT(DISTINCT p.userId) > 1 " +
            "ORDER BY SUM(p.discountValue) DESC")
    List<Object[]> detectPromoAbuseByDevice(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Detect promo abuse from same IP.
     */
    @Query("SELECT p.ipAddress, COUNT(DISTINCT p.userId), SUM(p.discountValue) " +
            "FROM PromoUsageLog p " +
            "WHERE p.ipAddress IS NOT NULL AND p.status = 'COMPLETED' " +
            "AND p.createdAt BETWEEN :start AND :end " +
            "GROUP BY p.ipAddress " +
            "HAVING COUNT(DISTINCT p.userId) > 1 " +
            "ORDER BY SUM(p.discountValue) DESC")
    List<Object[]> detectPromoAbuseByIp(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== First Order Promo Analysis ====================

    /**
     * Get first order promo usage patterns.
     */
    @Query("SELECT COUNT(p) FROM PromoUsageLog p " +
            "WHERE p.isFirstOrder = true AND p.createdAt BETWEEN :start AND :end")
    Long countFirstOrderPromos(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get users who only used first order promos then disappeared.
     */
    @Query("SELECT p.userId, MAX(p.createdAt) FROM PromoUsageLog p " +
            "WHERE p.isFirstOrder = true AND p.status = 'COMPLETED' " +
            "AND p.createdAt BETWEEN :start AND :end " +
            "GROUP BY p.userId " +
            "HAVING COUNT(p) = 1")
    List<Object[]> getOneTimePromoUsers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Average Metrics ====================

    @Query("SELECT AVG(p.discountValue) FROM PromoUsageLog p " +
            "WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :start AND :end")
    BigDecimal getAvgPromoCreditsPerUsage(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
