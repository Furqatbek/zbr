package com.fooddelivery.analytics.operations.repository;

import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for order-related operations analytics.
 */
@Repository
public interface OperationsOrderRepository extends JpaRepository<Order, Long> {

    // ============ Order Fulfillment Queries ============

    /**
     * Get average acceptance time (minutes from placed to accepted).
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (o.acceptedAt - o.createdAt)) / 60) " +
            "FROM Order o " +
            "WHERE o.acceptedAt IS NOT NULL " +
            "AND o.createdAt >= :startTime AND o.createdAt <= :endTime")
    Double getAverageAcceptanceTimeMinutes(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get average preparation time (minutes from accepted to ready).
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (o.readyAt - o.acceptedAt)) / 60) " +
            "FROM Order o " +
            "WHERE o.readyAt IS NOT NULL AND o.acceptedAt IS NOT NULL " +
            "AND o.createdAt >= :startTime AND o.createdAt <= :endTime")
    Double getAveragePreparationTimeMinutes(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get average pickup wait time (minutes from ready to picked up).
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (o.pickedUpAt - o.readyAt)) / 60) " +
            "FROM Order o " +
            "WHERE o.pickedUpAt IS NOT NULL AND o.readyAt IS NOT NULL " +
            "AND o.createdAt >= :startTime AND o.createdAt <= :endTime")
    Double getAveragePickupWaitTimeMinutes(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get average delivery time (minutes from picked up to delivered).
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (o.deliveredAt - o.pickedUpAt)) / 60) " +
            "FROM Order o " +
            "WHERE o.deliveredAt IS NOT NULL AND o.pickedUpAt IS NOT NULL " +
            "AND o.createdAt >= :startTime AND o.createdAt <= :endTime")
    Double getAverageDeliveryTimeMinutes(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get average total fulfillment time (minutes from placed to delivered).
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (o.deliveredAt - o.createdAt)) / 60) " +
            "FROM Order o " +
            "WHERE o.deliveredAt IS NOT NULL " +
            "AND o.createdAt >= :startTime AND o.createdAt <= :endTime")
    Double getAverageTotalFulfillmentTimeMinutes(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // ============ Delivery Success Rate Queries ============

    /**
     * Count total orders in a time range.
     */
    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.createdAt >= :startTime AND o.createdAt <= :endTime")
    Long countTotalOrders(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Count successfully delivered orders.
     */
    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.status IN ('DELIVERED', 'COMPLETED') " +
            "AND o.createdAt >= :startTime AND o.createdAt <= :endTime")
    Long countSuccessfulDeliveries(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Count cancelled orders by cancellation reason pattern.
     */
    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.status = 'CANCELLED' " +
            "AND o.cancellationReason LIKE :reasonPattern " +
            "AND o.createdAt >= :startTime AND o.createdAt <= :endTime")
    Long countCancelledByReasonPattern(
            @Param("reasonPattern") String reasonPattern,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get cancellation reasons breakdown.
     * Returns [reason, count].
     */
    @Query("SELECT o.cancellationReason, COUNT(o) FROM Order o " +
            "WHERE o.status = 'CANCELLED' " +
            "AND o.createdAt >= :startTime AND o.createdAt <= :endTime " +
            "GROUP BY o.cancellationReason")
    List<Object[]> getCancellationReasons(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Count orders with major delays (> 15 min late).
     */
    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.status IN ('DELIVERED', 'COMPLETED') " +
            "AND o.estimatedDeliveryTime IS NOT NULL " +
            "AND o.deliveredAt IS NOT NULL " +
            "AND EXTRACT(EPOCH FROM (o.deliveredAt - o.estimatedDeliveryTime)) / 60 > 15 " +
            "AND o.createdAt >= :startTime AND o.createdAt <= :endTime")
    Long countOrdersWithMajorDelays(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Count on-time deliveries (within 5 min of estimate).
     */
    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.status IN ('DELIVERED', 'COMPLETED') " +
            "AND o.estimatedDeliveryTime IS NOT NULL " +
            "AND o.deliveredAt IS NOT NULL " +
            "AND ABS(EXTRACT(EPOCH FROM (o.deliveredAt - o.estimatedDeliveryTime)) / 60) <= 5 " +
            "AND o.createdAt >= :startTime AND o.createdAt <= :endTime")
    Long countOnTimeDeliveries(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get success rate by hour of day.
     * Returns [hour, total, successful, successRate].
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM created_at) as hour, " +
            "COUNT(*) as total, " +
            "COUNT(CASE WHEN status IN ('DELIVERED', 'COMPLETED') THEN 1 END) as successful, " +
            "COUNT(CASE WHEN status IN ('DELIVERED', 'COMPLETED') THEN 1 END) * 100.0 / COUNT(*) as rate " +
            "FROM orders " +
            "WHERE created_at >= :startTime AND created_at <= :endTime " +
            "GROUP BY EXTRACT(HOUR FROM created_at) " +
            "ORDER BY hour", nativeQuery = true)
    List<Object[]> getSuccessRateByHour(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get success rate by day of week.
     * Returns [dayOfWeek, total, successful, successRate].
     */
    @Query(value = "SELECT EXTRACT(DOW FROM created_at) as dow, " +
            "COUNT(*) as total, " +
            "COUNT(CASE WHEN status IN ('DELIVERED', 'COMPLETED') THEN 1 END) as successful, " +
            "COUNT(CASE WHEN status IN ('DELIVERED', 'COMPLETED') THEN 1 END) * 100.0 / COUNT(*) as rate " +
            "FROM orders " +
            "WHERE created_at >= :startTime AND created_at <= :endTime " +
            "GROUP BY EXTRACT(DOW FROM created_at) " +
            "ORDER BY dow", nativeQuery = true)
    List<Object[]> getSuccessRateByDayOfWeek(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get success rate by restaurant.
     * Returns [restaurantId, restaurantName, total, successful, successRate].
     */
    @Query(value = "SELECT o.restaurant_id, r.name, " +
            "COUNT(*) as total, " +
            "COUNT(CASE WHEN o.status IN ('DELIVERED', 'COMPLETED') THEN 1 END) as successful, " +
            "COUNT(CASE WHEN o.status IN ('DELIVERED', 'COMPLETED') THEN 1 END) * 100.0 / COUNT(*) as rate " +
            "FROM orders o " +
            "JOIN restaurants r ON o.restaurant_id = r.id " +
            "WHERE o.created_at >= :startTime AND o.created_at <= :endTime " +
            "GROUP BY o.restaurant_id, r.name " +
            "ORDER BY rate DESC", nativeQuery = true)
    List<Object[]> getSuccessRateByRestaurant(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get daily success metrics.
     * Returns [date, total, successful, cancelled, majorDelays].
     */
    @Query(value = "SELECT DATE(created_at) as day, " +
            "COUNT(*) as total, " +
            "COUNT(CASE WHEN status IN ('DELIVERED', 'COMPLETED') THEN 1 END) as successful, " +
            "COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) as cancelled, " +
            "COUNT(CASE WHEN status IN ('DELIVERED', 'COMPLETED') " +
            "  AND estimated_delivery_time IS NOT NULL " +
            "  AND delivered_at IS NOT NULL " +
            "  AND EXTRACT(EPOCH FROM (delivered_at - estimated_delivery_time)) / 60 > 15 THEN 1 END) as major_delays " +
            "FROM orders " +
            "WHERE created_at >= :startTime AND created_at <= :endTime " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY day", nativeQuery = true)
    List<Object[]> getDailySuccessMetrics(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // ============ Restaurant-specific Queries ============

    /**
     * Get average acceptance time for a specific restaurant.
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (o.acceptedAt - o.createdAt)) / 60) " +
            "FROM Order o " +
            "WHERE o.restaurant.id = :restaurantId " +
            "AND o.acceptedAt IS NOT NULL " +
            "AND o.createdAt >= :startTime AND o.createdAt <= :endTime")
    Double getRestaurantAvgAcceptanceTime(
            @Param("restaurantId") Long restaurantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Count orders for a restaurant in time range.
     */
    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.restaurant.id = :restaurantId " +
            "AND o.createdAt >= :startTime AND o.createdAt <= :endTime")
    Long countRestaurantOrders(
            @Param("restaurantId") Long restaurantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // ============ Courier-specific Queries ============

    /**
     * Get average delivery time for a specific courier.
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (o.deliveredAt - o.pickedUpAt)) / 60) " +
            "FROM Order o " +
            "WHERE o.courier.id = :courierId " +
            "AND o.deliveredAt IS NOT NULL AND o.pickedUpAt IS NOT NULL " +
            "AND o.createdAt >= :startTime AND o.createdAt <= :endTime")
    Double getCourierAvgDeliveryTime(
            @Param("courierId") Long courierId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Count deliveries completed by a courier.
     */
    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.courier.id = :courierId " +
            "AND o.status IN ('DELIVERED', 'COMPLETED') " +
            "AND o.createdAt >= :startTime AND o.createdAt <= :endTime")
    Long countCourierDeliveries(
            @Param("courierId") Long courierId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get delivery time statistics for a courier.
     * Returns [avg, min, max, count].
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (o.deliveredAt - o.pickedUpAt)) / 60), " +
            "MIN(EXTRACT(EPOCH FROM (o.deliveredAt - o.pickedUpAt)) / 60), " +
            "MAX(EXTRACT(EPOCH FROM (o.deliveredAt - o.pickedUpAt)) / 60), " +
            "COUNT(o) " +
            "FROM Order o " +
            "WHERE o.courier.id = :courierId " +
            "AND o.deliveredAt IS NOT NULL AND o.pickedUpAt IS NOT NULL " +
            "AND o.createdAt >= :startTime AND o.createdAt <= :endTime")
    Object[] getCourierDeliveryTimeStats(
            @Param("courierId") Long courierId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
