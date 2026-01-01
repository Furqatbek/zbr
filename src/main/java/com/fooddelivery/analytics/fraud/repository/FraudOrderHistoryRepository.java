package com.fooddelivery.analytics.fraud.repository;

import com.fooddelivery.analytics.fraud.model.FraudOrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for order-based fraud detection queries.
 */
@Repository
public interface FraudOrderHistoryRepository extends JpaRepository<FraudOrderHistory, Long> {

    // ==================== Order Velocity Detection ====================

    /**
     * Get users exceeding order velocity threshold.
     * Returns: userId, orderCount, windowStart, windowEnd
     */
    @Query("SELECT o.userId, COUNT(o) " +
            "FROM FraudOrderHistory o " +
            "WHERE o.createdAt BETWEEN :windowStart AND :windowEnd " +
            "GROUP BY o.userId " +
            "HAVING COUNT(o) >= :threshold " +
            "ORDER BY COUNT(o) DESC")
    List<Object[]> getUsersExceedingVelocity(@Param("windowStart") LocalDateTime windowStart,
                                              @Param("windowEnd") LocalDateTime windowEnd,
                                              @Param("threshold") int threshold);

    /**
     * Get hourly order distribution for a user.
     */
    @Query("SELECT EXTRACT(HOUR FROM o.createdAt), COUNT(o) FROM FraudOrderHistory o " +
            "WHERE o.userId = :userId AND o.createdAt BETWEEN :start AND :end " +
            "GROUP BY EXTRACT(HOUR FROM o.createdAt)")
    List<Object[]> getUserOrdersByHour(@Param("userId") Long userId,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    /**
     * Get overall hourly order distribution.
     */
    @Query("SELECT EXTRACT(HOUR FROM o.createdAt), COUNT(o) FROM FraudOrderHistory o " +
            "WHERE o.createdAt BETWEEN :start AND :end " +
            "GROUP BY EXTRACT(HOUR FROM o.createdAt) " +
            "ORDER BY EXTRACT(HOUR FROM o.createdAt)")
    List<Object[]> getHourlyOrderDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Order Value Anomaly Detection ====================

    /**
     * Get order value statistics for anomaly detection.
     * Returns: avg, stddev, median, min, max
     */
    @Query("SELECT AVG(o.amount), STDDEV(o.amount), " +
            "PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY o.amount), " +
            "MIN(o.amount), MAX(o.amount) " +
            "FROM FraudOrderHistory o " +
            "WHERE o.createdAt BETWEEN :start AND :end")
    Object[] getOrderValueStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get orders with abnormal values (z-score based).
     * Returns: orderId, userId, amount, createdAt
     */
    @Query("SELECT o.orderId, o.userId, o.amount, o.createdAt " +
            "FROM FraudOrderHistory o " +
            "WHERE o.createdAt BETWEEN :start AND :end " +
            "AND (o.amount > :upperThreshold OR o.amount < :lowerThreshold) " +
            "ORDER BY o.amount DESC")
    List<Object[]> getAbnormalValueOrders(@Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end,
                                           @Param("upperThreshold") BigDecimal upperThreshold,
                                           @Param("lowerThreshold") BigDecimal lowerThreshold);

    // ==================== Refund Abuse Detection ====================

    /**
     * Get users with high refund rates.
     * Returns: userId, totalOrders, refundedOrders, totalRefundAmount
     */
    @Query("SELECT o.userId, COUNT(o), " +
            "SUM(CASE WHEN o.isRefunded = true THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(o.refundAmount), 0) " +
            "FROM FraudOrderHistory o " +
            "WHERE o.createdAt BETWEEN :start AND :end " +
            "GROUP BY o.userId " +
            "HAVING COUNT(o) >= :minOrders " +
            "AND (CAST(SUM(CASE WHEN o.isRefunded = true THEN 1 ELSE 0 END) AS double) / COUNT(o)) >= :refundRateThreshold " +
            "ORDER BY SUM(CASE WHEN o.isRefunded = true THEN 1 ELSE 0 END) DESC")
    List<Object[]> getUsersWithHighRefundRate(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end,
                                               @Param("minOrders") int minOrders,
                                               @Param("refundRateThreshold") double refundRateThreshold);

    /**
     * Get platform-wide refund stats.
     */
    @Query("SELECT COUNT(o), " +
            "SUM(CASE WHEN o.isRefunded = true THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(o.refundAmount), 0) " +
            "FROM FraudOrderHistory o WHERE o.createdAt BETWEEN :start AND :end")
    Object[] getPlatformRefundStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get refund breakdown by reason.
     */
    @Query("SELECT o.refundReason, COUNT(o), SUM(o.refundAmount) FROM FraudOrderHistory o " +
            "WHERE o.isRefunded = true AND o.createdAt BETWEEN :start AND :end " +
            "GROUP BY o.refundReason ORDER BY COUNT(o) DESC")
    List<Object[]> getRefundsByReason(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Restaurant Refund Anomalies ====================

    /**
     * Get restaurants with abnormal refund rates.
     * Returns: restaurantId, totalOrders, refundedOrders, refundRate, totalRefundAmount
     */
    @Query("SELECT o.restaurantId, COUNT(o), " +
            "SUM(CASE WHEN o.isRefunded = true THEN 1 ELSE 0 END), " +
            "CAST(SUM(CASE WHEN o.isRefunded = true THEN 1 ELSE 0 END) AS double) / COUNT(o), " +
            "COALESCE(SUM(o.refundAmount), 0) " +
            "FROM FraudOrderHistory o " +
            "WHERE o.restaurantId IS NOT NULL AND o.createdAt BETWEEN :start AND :end " +
            "GROUP BY o.restaurantId " +
            "HAVING COUNT(o) >= :minOrders " +
            "ORDER BY CAST(SUM(CASE WHEN o.isRefunded = true THEN 1 ELSE 0 END) AS double) / COUNT(o) DESC")
    List<Object[]> getRestaurantRefundRates(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end,
                                             @Param("minOrders") int minOrders);

    // ==================== Address Fraud Detection ====================

    /**
     * Detect addresses used by multiple users.
     * Returns: addressHash, userCount, totalOrders, userIds
     */
    @Query("SELECT o.deliveryAddressHash, COUNT(DISTINCT o.userId), COUNT(o), " +
            "STRING_AGG(DISTINCT CAST(o.userId AS string), ',') " +
            "FROM FraudOrderHistory o " +
            "WHERE o.deliveryAddressHash IS NOT NULL AND o.createdAt BETWEEN :start AND :end " +
            "GROUP BY o.deliveryAddressHash " +
            "HAVING COUNT(DISTINCT o.userId) >= :threshold " +
            "ORDER BY COUNT(DISTINCT o.userId) DESC")
    List<Object[]> getAddressesWithMultipleUsers(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end,
                                                  @Param("threshold") int threshold);

    @Query("SELECT COUNT(DISTINCT o.deliveryAddressHash) FROM FraudOrderHistory o " +
            "WHERE o.deliveryAddressHash IS NOT NULL AND o.createdAt BETWEEN :start AND :end")
    Long countUniqueAddresses(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Fake Account Detection ====================

    /**
     * Get new account orders (account age < threshold).
     * Returns: userId, accountAgeHours, hasPromo, orderId
     */
    @Query("SELECT o.userId, o.userAccountAgeHours, o.isPromoOrder, o.orderId " +
            "FROM FraudOrderHistory o " +
            "WHERE o.userAccountAgeHours IS NOT NULL AND o.userAccountAgeHours < :ageThreshold " +
            "AND o.createdAt BETWEEN :start AND :end " +
            "ORDER BY o.userAccountAgeHours ASC")
    List<Object[]> getNewAccountOrders(@Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end,
                                        @Param("ageThreshold") int ageThreshold);

    /**
     * Count suspected fake accounts.
     */
    @Query("SELECT COUNT(DISTINCT o.userId) FROM FraudOrderHistory o " +
            "WHERE o.userAccountAgeHours IS NOT NULL AND o.userAccountAgeHours < :ageThreshold " +
            "AND o.isFirstOrder = true AND o.createdAt BETWEEN :start AND :end")
    Long countSuspectedFakeAccounts(@Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end,
                                     @Param("ageThreshold") int ageThreshold);

    // ==================== Device Correlation ====================

    /**
     * Detect devices used by multiple users.
     * Returns: deviceId, userCount, totalOrders, userIds
     */
    @Query("SELECT o.deviceId, COUNT(DISTINCT o.userId), COUNT(o), " +
            "STRING_AGG(DISTINCT CAST(o.userId AS string), ',') " +
            "FROM FraudOrderHistory o " +
            "WHERE o.deviceId IS NOT NULL AND o.createdAt BETWEEN :start AND :end " +
            "GROUP BY o.deviceId " +
            "HAVING COUNT(DISTINCT o.userId) >= :threshold " +
            "ORDER BY COUNT(DISTINCT o.userId) DESC")
    List<Object[]> getDevicesWithMultipleUsers(@Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end,
                                                @Param("threshold") int threshold);

    // ==================== Promo Order Analysis ====================

    /**
     * Get users with only promo orders (never paid).
     */
    @Query("SELECT o.userId, COUNT(o), SUM(o.amount), SUM(o.discountAmount) " +
            "FROM FraudOrderHistory o " +
            "WHERE o.createdAt BETWEEN :start AND :end " +
            "GROUP BY o.userId " +
            "HAVING SUM(o.userPaidAmount) = 0 AND COUNT(o) >= :minOrders " +
            "ORDER BY COUNT(o) DESC")
    List<Object[]> getPromoOnlyOrderUsers(@Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end,
                                           @Param("minOrders") int minOrders);

    // ==================== Average Metrics ====================

    @Query("SELECT AVG(sub.orderCount) FROM (" +
            "SELECT COUNT(o) as orderCount FROM FraudOrderHistory o " +
            "WHERE o.createdAt BETWEEN :start AND :end " +
            "GROUP BY o.userId) sub")
    Double getAvgOrdersPerUser(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get order statistics for a specific user.
     * Returns: totalOrders, refundedOrders, avgOrderValue
     */
    @Query("SELECT COUNT(o), " +
            "SUM(CASE WHEN o.isRefunded = true THEN 1 ELSE 0 END), " +
            "AVG(o.amount) " +
            "FROM FraudOrderHistory o " +
            "WHERE o.userId = :userId AND o.createdAt BETWEEN :start AND :end")
    List<Object[]> getUserOrderStats(@Param("userId") Long userId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);
}
