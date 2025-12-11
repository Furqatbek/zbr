package com.fooddelivery.analytics.operations.repository;

import com.fooddelivery.analytics.operations.model.CourierOrderEvent;
import com.fooddelivery.analytics.operations.model.CourierOrderEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for courier order events analytics.
 */
@Repository
public interface CourierOrderEventRepository extends JpaRepository<CourierOrderEvent, Long> {

    /**
     * Count events by type for a courier in a time range.
     */
    @Query("SELECT COUNT(e) FROM CourierOrderEvent e " +
            "WHERE e.courierId = :courierId " +
            "AND e.eventType = :eventType " +
            "AND e.eventTimestamp >= :startTime AND e.eventTimestamp <= :endTime")
    Long countByCourierIdAndEventType(
            @Param("courierId") Long courierId,
            @Param("eventType") CourierOrderEventType eventType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get event counts by type for a courier.
     * Returns [eventType, count].
     */
    @Query("SELECT e.eventType, COUNT(e) FROM CourierOrderEvent e " +
            "WHERE e.courierId = :courierId " +
            "AND e.eventTimestamp >= :startTime AND e.eventTimestamp <= :endTime " +
            "GROUP BY e.eventType")
    List<Object[]> getEventCountsByType(
            @Param("courierId") Long courierId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get average response time for a courier.
     */
    @Query("SELECT AVG(e.responseTimeSeconds) FROM CourierOrderEvent e " +
            "WHERE e.courierId = :courierId " +
            "AND e.eventType IN ('ACCEPTED', 'REJECTED') " +
            "AND e.responseTimeSeconds IS NOT NULL " +
            "AND e.eventTimestamp >= :startTime AND e.eventTimestamp <= :endTime")
    Double getAverageResponseTime(
            @Param("courierId") Long courierId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get cancellation reasons for a courier.
     * Returns [reason, count].
     */
    @Query("SELECT e.reason, COUNT(e) FROM CourierOrderEvent e " +
            "WHERE e.courierId = :courierId " +
            "AND e.eventType = 'CANCELLED' " +
            "AND e.eventTimestamp >= :startTime AND e.eventTimestamp <= :endTime " +
            "GROUP BY e.reason")
    List<Object[]> getCancellationReasons(
            @Param("courierId") Long courierId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get daily event counts for a courier.
     * Returns [date, offered, accepted, rejected, cancelled].
     */
    @Query(value = "SELECT DATE(event_timestamp) as day, " +
            "COUNT(CASE WHEN event_type = 'OFFERED' THEN 1 END) as offered, " +
            "COUNT(CASE WHEN event_type = 'ACCEPTED' THEN 1 END) as accepted, " +
            "COUNT(CASE WHEN event_type = 'REJECTED' THEN 1 END) as rejected, " +
            "COUNT(CASE WHEN event_type = 'CANCELLED' THEN 1 END) as cancelled " +
            "FROM courier_order_events " +
            "WHERE courier_id = :courierId " +
            "AND event_timestamp >= :startTime AND event_timestamp <= :endTime " +
            "GROUP BY DATE(event_timestamp) " +
            "ORDER BY day", nativeQuery = true)
    List<Object[]> getDailyEventCounts(
            @Param("courierId") Long courierId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Calculate acceptance rate for all couriers in a time range.
     */
    @Query(value = "SELECT courier_id, " +
            "COUNT(CASE WHEN event_type = 'OFFERED' THEN 1 END) as offered, " +
            "COUNT(CASE WHEN event_type = 'ACCEPTED' THEN 1 END) as accepted " +
            "FROM courier_order_events " +
            "WHERE event_timestamp >= :startTime AND event_timestamp <= :endTime " +
            "GROUP BY courier_id " +
            "HAVING COUNT(CASE WHEN event_type = 'OFFERED' THEN 1 END) > 0", nativeQuery = true)
    List<Object[]> getCourierAcceptanceRates(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get total orders cancelled by couriers.
     */
    @Query("SELECT COUNT(e) FROM CourierOrderEvent e " +
            "WHERE e.eventType = 'CANCELLED' " +
            "AND e.eventTimestamp >= :startTime AND e.eventTimestamp <= :endTime")
    Long countTotalCourierCancellations(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
