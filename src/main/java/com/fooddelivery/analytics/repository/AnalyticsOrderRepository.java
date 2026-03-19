package com.fooddelivery.analytics.repository;

import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Order analytics with optimized queries.
 * Extends the base Order repository with analytics-specific queries.
 */
@Repository
public interface AnalyticsOrderRepository extends JpaRepository<Order, Long> {

    // ============ Order Volume Queries ============

    /**
     * Count all orders since a given time.
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :since")
    Long countOrdersSince(@Param("since") LocalDateTime since);

    /**
     * Count orders by status since a given time.
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.createdAt >= :since")
    Long countOrdersByStatusSince(
            @Param("status") OrderStatus status,
            @Param("since") LocalDateTime since);

    /**
     * Count completed orders (DELIVERED or COMPLETED) since a given time.
     */
    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.status IN ('DELIVERED', 'COMPLETED') AND o.createdAt >= :since")
    Long countCompletedOrdersSince(@Param("since") LocalDateTime since);

    /**
     * Count cancelled orders since a given time.
     */
    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.status = 'CANCELLED' AND o.createdAt >= :since")
    Long countCancelledOrdersSince(@Param("since") LocalDateTime since);

    /**
     * Count orders by status in a date range.
     * Returns [status, count] pairs.
     */
    @Query("SELECT o.status, COUNT(o) FROM Order o " +
            "WHERE o.createdAt >= :startDate AND o.createdAt < :endDate " +
            "GROUP BY o.status")
    List<Object[]> countOrdersByStatusBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ============ First Order vs Repeat Order Queries ============

    /**
     * Count first-time orders (users placing their first ever order) since a given time.
     * Uses a subquery to identify first orders.
     */
    @Query(value = "SELECT COUNT(*) FROM orders o " +
            "WHERE o.created_at >= :since " +
            "AND o.created_at = (SELECT MIN(o2.created_at) FROM orders o2 WHERE o2.consumer_id = o.consumer_id)",
            nativeQuery = true)
    Long countFirstTimeOrdersSince(@Param("since") LocalDateTime since);

    /**
     * Count repeat orders (not the user's first order) since a given time.
     */
    @Query(value = "SELECT COUNT(*) FROM orders o " +
            "WHERE o.created_at >= :since " +
            "AND o.created_at > (SELECT MIN(o2.created_at) FROM orders o2 WHERE o2.consumer_id = o.consumer_id)",
            nativeQuery = true)
    Long countRepeatOrdersSince(@Param("since") LocalDateTime since);

    /**
     * Get orders per hour distribution for a date range.
     * Returns [hour, count] pairs.
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM created_at) as hour, COUNT(*) as count " +
            "FROM orders " +
            "WHERE created_at >= :startDate AND created_at < :endDate " +
            "GROUP BY EXTRACT(HOUR FROM created_at) " +
            "ORDER BY hour", nativeQuery = true)
    List<Object[]> getOrdersPerHourBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get daily order counts for a date range.
     * Returns [date, totalCount, completedCount, cancelledCount].
     */
    @Query(value = "SELECT DATE(created_at) as order_date, " +
            "COUNT(*) as total_count, " +
            "COUNT(CASE WHEN status IN ('DELIVERED', 'COMPLETED') THEN 1 END) as completed_count, " +
            "COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) as cancelled_count " +
            "FROM orders " +
            "WHERE created_at >= :startDate AND created_at < :endDate " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY order_date", nativeQuery = true)
    List<Object[]> getDailyOrderCountsBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ============ AOV (Average Order Value) Queries ============

    /**
     * Calculate average order value for completed orders since a given time.
     */
    @Query("SELECT AVG(o.total) FROM Order o " +
            "WHERE o.status IN ('DELIVERED', 'COMPLETED') AND o.createdAt >= :since")
    BigDecimal getAverageOrderValueSince(@Param("since") LocalDateTime since);

    /**
     * Get total revenue from completed orders since a given time.
     */
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o " +
            "WHERE o.status IN ('DELIVERED', 'COMPLETED') AND o.createdAt >= :since")
    BigDecimal getTotalRevenueSince(@Param("since") LocalDateTime since);

    /**
     * Get order value statistics (min, max, avg, sum, count).
     * Returns a list containing a single [min, max, avg, sum, count] row.
     */
    @Query("SELECT MIN(o.total), MAX(o.total), AVG(o.total), SUM(o.total), COUNT(o) " +
            "FROM Order o WHERE o.status IN ('DELIVERED', 'COMPLETED') " +
            "AND o.createdAt >= :since")
    List<Object[]> getOrderValueStatsSince(@Param("since") LocalDateTime since);

    /**
     * Get median order value using percentile calculation.
     */
    @Query(value = "SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY total) " +
            "FROM orders WHERE status IN ('DELIVERED', 'COMPLETED') " +
            "AND created_at >= :since", nativeQuery = true)
    BigDecimal getMedianOrderValueSince(@Param("since") LocalDateTime since);

    /**
     * Get average items per order.
     */
    @Query(value = "SELECT AVG(item_count) FROM (" +
            "SELECT o.id, COUNT(oi.id) as item_count " +
            "FROM orders o " +
            "JOIN order_items oi ON o.id = oi.order_id " +
            "WHERE o.status IN ('DELIVERED', 'COMPLETED') " +
            "AND o.created_at >= :since " +
            "GROUP BY o.id) as order_items_count", nativeQuery = true)
    Double getAverageItemsPerOrderSince(@Param("since") LocalDateTime since);

    /**
     * Get average delivery fee.
     */
    @Query("SELECT AVG(o.deliveryFee) FROM Order o " +
            "WHERE o.status IN ('DELIVERED', 'COMPLETED') " +
            "AND o.deliveryFee IS NOT NULL AND o.createdAt >= :since")
    BigDecimal getAverageDeliveryFeeSince(@Param("since") LocalDateTime since);

    /**
     * Get average tip amount.
     */
    @Query("SELECT AVG(o.tipAmount) FROM Order o " +
            "WHERE o.status IN ('DELIVERED', 'COMPLETED') " +
            "AND o.tipAmount IS NOT NULL AND o.tipAmount > 0 " +
            "AND o.createdAt >= :since")
    BigDecimal getAverageTipAmountSince(@Param("since") LocalDateTime since);

    /**
     * Get daily AOV trend.
     * Returns [date, aov, revenue, orderCount].
     */
    @Query(value = "SELECT DATE(created_at) as order_date, " +
            "AVG(total) as aov, " +
            "SUM(total) as revenue, " +
            "COUNT(*) as order_count " +
            "FROM orders " +
            "WHERE status IN ('DELIVERED', 'COMPLETED') " +
            "AND created_at >= :startDate AND created_at < :endDate " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY order_date", nativeQuery = true)
    List<Object[]> getDailyAovTrendBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ============ User Activation Queries ============

    /**
     * Count users who completed their first order (delivery) since a given time.
     */
    @Query(value = "SELECT COUNT(DISTINCT consumer_id) FROM orders " +
            "WHERE status IN ('DELIVERED', 'COMPLETED') " +
            "AND delivered_at >= :since " +
            "AND delivered_at = (SELECT MIN(o2.delivered_at) FROM orders o2 " +
            "WHERE o2.consumer_id = orders.consumer_id " +
            "AND o2.status IN ('DELIVERED', 'COMPLETED'))",
            nativeQuery = true)
    Long countFirstDeliveryCompletedSince(@Param("since") LocalDateTime since);

    /**
     * Get activation time statistics (time from user creation to first order).
     * Returns [avgHours, minHours, maxHours, medianHours].
     */
    @Query(value = "SELECT " +
            "AVG(EXTRACT(EPOCH FROM (first_order - user_created)) / 3600) as avg_hours, " +
            "MIN(EXTRACT(EPOCH FROM (first_order - user_created)) / 3600) as min_hours, " +
            "MAX(EXTRACT(EPOCH FROM (first_order - user_created)) / 3600) as max_hours " +
            "FROM (" +
            "  SELECT u.id, u.created_at as user_created, MIN(o.created_at) as first_order " +
            "  FROM users u " +
            "  JOIN orders o ON u.id = o.consumer_id " +
            "  WHERE u.created_at >= :since " +
            "  GROUP BY u.id, u.created_at" +
            ") as activation_times " +
            "WHERE first_order IS NOT NULL", nativeQuery = true)
    Object[] getActivationTimeStatsSince(@Param("since") LocalDateTime since);

    // ============ Churn Queries ============

    /**
     * Count users who have placed at least one order ever.
     */
    @Query("SELECT COUNT(DISTINCT o.consumer.id) FROM Order o")
    Long countTotalUsersWithOrders();

    /**
     * Count users who placed orders in the last N days.
     */
    @Query("SELECT COUNT(DISTINCT o.consumer.id) FROM Order o WHERE o.createdAt >= :since")
    Long countUsersWithOrdersSince(@Param("since") LocalDateTime since);

    /**
     * Get users at risk of churning (last order between two date ranges).
     */
    @Query(value = "SELECT COUNT(DISTINCT consumer_id) FROM (" +
            "  SELECT consumer_id, MAX(created_at) as last_order " +
            "  FROM orders " +
            "  GROUP BY consumer_id " +
            "  HAVING MAX(created_at) BETWEEN :riskStart AND :riskEnd" +
            ") as at_risk_users", nativeQuery = true)
    Long countUsersAtRiskOfChurn(
            @Param("riskStart") LocalDateTime riskStart,
            @Param("riskEnd") LocalDateTime riskEnd);

    /**
     * Get restaurant order activity for churn analysis.
     * Returns [restaurantId, lastOrderDate, orderCount30Days].
     */
    @Query(value = "SELECT r.id as restaurant_id, " +
            "MAX(o.created_at) as last_order_date, " +
            "COUNT(CASE WHEN o.created_at >= :since THEN 1 END) as order_count " +
            "FROM restaurants r " +
            "LEFT JOIN orders o ON r.id = o.restaurant_id " +
            "WHERE r.status = 'ACTIVE' " +
            "GROUP BY r.id", nativeQuery = true)
    List<Object[]> getRestaurantOrderActivity(@Param("since") LocalDateTime since);

    /**
     * Count restaurants with orders since a given time.
     */
    @Query("SELECT COUNT(DISTINCT o.restaurant.id) FROM Order o WHERE o.createdAt >= :since")
    Long countRestaurantsWithOrdersSince(@Param("since") LocalDateTime since);
}
