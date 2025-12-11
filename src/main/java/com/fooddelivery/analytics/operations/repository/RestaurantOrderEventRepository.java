package com.fooddelivery.analytics.operations.repository;

import com.fooddelivery.analytics.operations.model.RestaurantOrderEvent;
import com.fooddelivery.analytics.operations.model.RestaurantOrderEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for restaurant order events analytics.
 */
@Repository
public interface RestaurantOrderEventRepository extends JpaRepository<RestaurantOrderEvent, Long> {

    /**
     * Count events by type for a restaurant in a time range.
     */
    @Query("SELECT COUNT(e) FROM RestaurantOrderEvent e " +
            "WHERE e.restaurantId = :restaurantId " +
            "AND e.eventType = :eventType " +
            "AND e.eventTimestamp >= :startTime AND e.eventTimestamp <= :endTime")
    Long countByRestaurantIdAndEventType(
            @Param("restaurantId") Long restaurantId,
            @Param("eventType") RestaurantOrderEventType eventType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get event counts by type for a restaurant.
     * Returns [eventType, count].
     */
    @Query("SELECT e.eventType, COUNT(e) FROM RestaurantOrderEvent e " +
            "WHERE e.restaurantId = :restaurantId " +
            "AND e.eventTimestamp >= :startTime AND e.eventTimestamp <= :endTime " +
            "GROUP BY e.eventType")
    List<Object[]> getEventCountsByType(
            @Param("restaurantId") Long restaurantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get average preparation time for a restaurant.
     */
    @Query("SELECT AVG(e.actualPrepMinutes) FROM RestaurantOrderEvent e " +
            "WHERE e.restaurantId = :restaurantId " +
            "AND e.eventType = 'READY' " +
            "AND e.actualPrepMinutes IS NOT NULL " +
            "AND e.eventTimestamp >= :startTime AND e.eventTimestamp <= :endTime")
    Double getAveragePreparationTime(
            @Param("restaurantId") Long restaurantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get preparation time statistics (avg, min, max, count).
     * Returns [avg, min, max, count].
     */
    @Query("SELECT AVG(e.actualPrepMinutes), MIN(e.actualPrepMinutes), " +
            "MAX(e.actualPrepMinutes), COUNT(e) " +
            "FROM RestaurantOrderEvent e " +
            "WHERE e.restaurantId = :restaurantId " +
            "AND e.eventType = 'READY' " +
            "AND e.actualPrepMinutes IS NOT NULL " +
            "AND e.eventTimestamp >= :startTime AND e.eventTimestamp <= :endTime")
    Object[] getPreparationTimeStats(
            @Param("restaurantId") Long restaurantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get median preparation time using percentile.
     */
    @Query(value = "SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY actual_prep_minutes) " +
            "FROM restaurant_order_events " +
            "WHERE restaurant_id = :restaurantId " +
            "AND event_type = 'READY' " +
            "AND actual_prep_minutes IS NOT NULL " +
            "AND event_timestamp >= :startTime AND event_timestamp <= :endTime", nativeQuery = true)
    Double getMedianPreparationTime(
            @Param("restaurantId") Long restaurantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get 90th percentile preparation time.
     */
    @Query(value = "SELECT PERCENTILE_CONT(0.9) WITHIN GROUP (ORDER BY actual_prep_minutes) " +
            "FROM restaurant_order_events " +
            "WHERE restaurant_id = :restaurantId " +
            "AND event_type = 'READY' " +
            "AND actual_prep_minutes IS NOT NULL " +
            "AND event_timestamp >= :startTime AND event_timestamp <= :endTime", nativeQuery = true)
    Double getP90PreparationTime(
            @Param("restaurantId") Long restaurantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Count orders prepared on time (within estimated prep time).
     */
    @Query("SELECT COUNT(e) FROM RestaurantOrderEvent e " +
            "WHERE e.restaurantId = :restaurantId " +
            "AND e.eventType = 'READY' " +
            "AND e.actualPrepMinutes IS NOT NULL " +
            "AND e.estimatedPrepMinutes IS NOT NULL " +
            "AND e.actualPrepMinutes <= e.estimatedPrepMinutes " +
            "AND e.eventTimestamp >= :startTime AND e.eventTimestamp <= :endTime")
    Long countOrdersPreparedOnTime(
            @Param("restaurantId") Long restaurantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get orders by hour for a restaurant.
     * Returns [hour, count].
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM event_timestamp) as hour, COUNT(*) as count " +
            "FROM restaurant_order_events " +
            "WHERE restaurant_id = :restaurantId " +
            "AND event_type = 'RECEIVED' " +
            "AND event_timestamp >= :startTime AND event_timestamp <= :endTime " +
            "GROUP BY EXTRACT(HOUR FROM event_timestamp) " +
            "ORDER BY hour", nativeQuery = true)
    List<Object[]> getOrdersByHour(
            @Param("restaurantId") Long restaurantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get daily metrics for a restaurant.
     * Returns [date, received, accepted, rejected, ready, cancelled].
     */
    @Query(value = "SELECT DATE(event_timestamp) as day, " +
            "COUNT(CASE WHEN event_type = 'RECEIVED' THEN 1 END) as received, " +
            "COUNT(CASE WHEN event_type = 'ACCEPTED' THEN 1 END) as accepted, " +
            "COUNT(CASE WHEN event_type = 'REJECTED' THEN 1 END) as rejected, " +
            "COUNT(CASE WHEN event_type = 'READY' THEN 1 END) as ready, " +
            "COUNT(CASE WHEN event_type = 'CANCELLED' THEN 1 END) as cancelled, " +
            "AVG(CASE WHEN event_type = 'READY' AND actual_prep_minutes IS NOT NULL " +
            "    THEN actual_prep_minutes END) as avg_prep_time " +
            "FROM restaurant_order_events " +
            "WHERE restaurant_id = :restaurantId " +
            "AND event_timestamp >= :startTime AND event_timestamp <= :endTime " +
            "GROUP BY DATE(event_timestamp) " +
            "ORDER BY day", nativeQuery = true)
    List<Object[]> getDailyMetrics(
            @Param("restaurantId") Long restaurantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get rejection reasons for a restaurant.
     * Returns [reason, count].
     */
    @Query("SELECT e.reason, COUNT(e) FROM RestaurantOrderEvent e " +
            "WHERE e.restaurantId = :restaurantId " +
            "AND e.eventType = 'REJECTED' " +
            "AND e.eventTimestamp >= :startTime AND e.eventTimestamp <= :endTime " +
            "GROUP BY e.reason")
    List<Object[]> getRejectionReasons(
            @Param("restaurantId") Long restaurantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
