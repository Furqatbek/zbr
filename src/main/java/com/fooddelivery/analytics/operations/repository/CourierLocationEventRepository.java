package com.fooddelivery.analytics.operations.repository;

import com.fooddelivery.analytics.operations.model.CourierLocationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for courier location events analytics.
 */
@Repository
public interface CourierLocationEventRepository extends JpaRepository<CourierLocationEvent, Long> {

    /**
     * Count location updates for a courier in a time range.
     */
    @Query("SELECT COUNT(e) FROM CourierLocationEvent e " +
            "WHERE e.courierId = :courierId " +
            "AND e.recordedAt >= :startTime AND e.recordedAt <= :endTime")
    Long countByCourierIdAndTimeRange(
            @Param("courierId") Long courierId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get location update counts per hour for a courier.
     * Returns [hour, count].
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM recorded_at) as hour, COUNT(*) as count " +
            "FROM courier_location_events " +
            "WHERE courier_id = :courierId " +
            "AND recorded_at >= :startTime AND recorded_at <= :endTime " +
            "GROUP BY EXTRACT(HOUR FROM recorded_at) " +
            "ORDER BY hour", nativeQuery = true)
    List<Object[]> getLocationUpdatesPerHour(
            @Param("courierId") Long courierId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get average location updates per hour for all couriers.
     */
    @Query(value = "SELECT AVG(hourly_count) FROM (" +
            "  SELECT courier_id, DATE(recorded_at) as day, " +
            "         EXTRACT(HOUR FROM recorded_at) as hour, " +
            "         COUNT(*) as hourly_count " +
            "  FROM courier_location_events " +
            "  WHERE recorded_at >= :startTime AND recorded_at <= :endTime " +
            "  GROUP BY courier_id, DATE(recorded_at), EXTRACT(HOUR FROM recorded_at)" +
            ") as hourly_counts", nativeQuery = true)
    Double getAverageLocationUpdatesPerHour(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get the latest location event for a courier.
     */
    @Query("SELECT e FROM CourierLocationEvent e " +
            "WHERE e.courierId = :courierId " +
            "ORDER BY e.recordedAt DESC LIMIT 1")
    CourierLocationEvent findLatestByCourierId(@Param("courierId") Long courierId);

    /**
     * Count couriers with location updates in the last N minutes.
     */
    @Query("SELECT COUNT(DISTINCT e.courierId) FROM CourierLocationEvent e " +
            "WHERE e.recordedAt >= :since")
    Long countCouriersWithRecentLocation(@Param("since") LocalDateTime since);

    /**
     * Get daily location update counts for a courier.
     * Returns [date, count].
     */
    @Query(value = "SELECT DATE(recorded_at) as day, COUNT(*) as count " +
            "FROM courier_location_events " +
            "WHERE courier_id = :courierId " +
            "AND recorded_at >= :startTime AND recorded_at <= :endTime " +
            "GROUP BY DATE(recorded_at) " +
            "ORDER BY day", nativeQuery = true)
    List<Object[]> getDailyLocationUpdates(
            @Param("courierId") Long courierId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
