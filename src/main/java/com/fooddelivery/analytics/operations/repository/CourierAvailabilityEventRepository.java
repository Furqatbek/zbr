package com.fooddelivery.analytics.operations.repository;

import com.fooddelivery.analytics.operations.model.CourierAvailabilityEvent;
import com.fooddelivery.analytics.operations.model.CourierAvailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for courier availability events analytics.
 */
@Repository
public interface CourierAvailabilityEventRepository extends JpaRepository<CourierAvailabilityEvent, Long> {

    /**
     * Get total active (online) time for a courier in minutes.
     * Sums duration of AVAILABLE, BUSY, and RETURNING statuses.
     */
    @Query(value = "SELECT COALESCE(SUM(previous_status_duration_minutes), 0) " +
            "FROM courier_availability_events " +
            "WHERE courier_id = :courierId " +
            "AND previous_status IN ('AVAILABLE', 'BUSY', 'RETURNING') " +
            "AND status_changed_at >= :startTime AND status_changed_at <= :endTime", nativeQuery = true)
    Long getTotalActiveTimeMinutes(
            @Param("courierId") Long courierId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get total available (not busy) time for a courier in minutes.
     */
    @Query(value = "SELECT COALESCE(SUM(previous_status_duration_minutes), 0) " +
            "FROM courier_availability_events " +
            "WHERE courier_id = :courierId " +
            "AND previous_status = 'AVAILABLE' " +
            "AND status_changed_at >= :startTime AND status_changed_at <= :endTime", nativeQuery = true)
    Long getTotalAvailableTimeMinutes(
            @Param("courierId") Long courierId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get availability by hour for a courier.
     * Returns [hour, availableMinutes].
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM business_ts(status_changed_at)) as hour, " +
            "COALESCE(SUM(CASE WHEN previous_status = 'AVAILABLE' THEN previous_status_duration_minutes ELSE 0 END), 0) as available_minutes " +
            "FROM courier_availability_events " +
            "WHERE courier_id = :courierId " +
            "AND status_changed_at >= :startTime AND status_changed_at <= :endTime " +
            "GROUP BY EXTRACT(HOUR FROM business_ts(status_changed_at)) " +
            "ORDER BY hour", nativeQuery = true)
    List<Object[]> getAvailabilityByHour(
            @Param("courierId") Long courierId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get daily active time for a courier.
     * Returns [date, activeMinutes, availableMinutes].
     */
    @Query(value = "SELECT DATE(business_ts(status_changed_at)) as day, " +
            "COALESCE(SUM(CASE WHEN previous_status IN ('AVAILABLE', 'BUSY', 'RETURNING') " +
            "  THEN previous_status_duration_minutes ELSE 0 END), 0) as active_minutes, " +
            "COALESCE(SUM(CASE WHEN previous_status = 'AVAILABLE' " +
            "  THEN previous_status_duration_minutes ELSE 0 END), 0) as available_minutes " +
            "FROM courier_availability_events " +
            "WHERE courier_id = :courierId " +
            "AND status_changed_at >= :startTime AND status_changed_at <= :endTime " +
            "GROUP BY DATE(business_ts(status_changed_at)) " +
            "ORDER BY day", nativeQuery = true)
    List<Object[]> getDailyAvailability(
            @Param("courierId") Long courierId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Count distinct active couriers in a time range.
     */
    @Query("SELECT COUNT(DISTINCT e.courierId) FROM CourierAvailabilityEvent e " +
            "WHERE e.status IN ('AVAILABLE', 'BUSY') " +
            "AND e.statusChangedAt >= :startTime AND e.statusChangedAt <= :endTime")
    Long countActiveCouriers(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get average active hours per day across all couriers.
     */
    @Query(value = "SELECT AVG(daily_active_hours) FROM (" +
            "  SELECT courier_id, DATE(business_ts(status_changed_at)) as day, " +
            "         SUM(CASE WHEN previous_status IN ('AVAILABLE', 'BUSY', 'RETURNING') " +
            "             THEN previous_status_duration_minutes ELSE 0 END) / 60.0 as daily_active_hours " +
            "  FROM courier_availability_events " +
            "  WHERE status_changed_at >= :startTime AND status_changed_at <= :endTime " +
            "  GROUP BY courier_id, DATE(business_ts(status_changed_at))" +
            ") as daily_hours", nativeQuery = true)
    Double getAverageActiveHoursPerDay(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
