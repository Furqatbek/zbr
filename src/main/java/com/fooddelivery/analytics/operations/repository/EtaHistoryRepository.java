package com.fooddelivery.analytics.operations.repository;

import com.fooddelivery.analytics.operations.model.EtaHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ETA history analytics.
 */
@Repository
public interface EtaHistoryRepository extends JpaRepository<EtaHistory, Long> {

    /**
     * Get ETA record for an order.
     */
    @Query("SELECT e FROM EtaHistory e WHERE e.orderId = :orderId")
    EtaHistory findByOrderId(@Param("orderId") Long orderId);

    /**
     * Get overall ETA accuracy statistics for a time range.
     * Returns [avgError, avgAbsError, count].
     */
    @Query("SELECT AVG(e.etaErrorMinutes), AVG(e.etaErrorAbsMinutes), COUNT(e) " +
            "FROM EtaHistory e " +
            "WHERE e.wasCompleted = true " +
            "AND e.actualDeliveredAt IS NOT NULL " +
            "AND e.predictedAt >= :startTime AND e.predictedAt <= :endTime")
    Object[] getOverallEtaStats(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get median absolute ETA error.
     */
    @Query(value = "SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY eta_error_abs_minutes) " +
            "FROM eta_history " +
            "WHERE was_completed = true " +
            "AND actual_delivered_at IS NOT NULL " +
            "AND predicted_at >= :startTime AND predicted_at <= :endTime", nativeQuery = true)
    Double getMedianAbsoluteError(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get 90th percentile absolute ETA error.
     */
    @Query(value = "SELECT PERCENTILE_CONT(0.9) WITHIN GROUP (ORDER BY eta_error_abs_minutes) " +
            "FROM eta_history " +
            "WHERE was_completed = true " +
            "AND actual_delivered_at IS NOT NULL " +
            "AND predicted_at >= :startTime AND predicted_at <= :endTime", nativeQuery = true)
    Double getP90AbsoluteError(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Count deliveries within N minutes of estimate.
     */
    @Query("SELECT COUNT(e) FROM EtaHistory e " +
            "WHERE e.wasCompleted = true " +
            "AND e.etaErrorAbsMinutes IS NOT NULL " +
            "AND e.etaErrorAbsMinutes <= :minutes " +
            "AND e.predictedAt >= :startTime AND e.predictedAt <= :endTime")
    Long countWithinMinutes(
            @Param("minutes") Integer minutes,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Count over-estimations (delivered early) and under-estimations (delivered late).
     * Returns [overCount, underCount, avgOverMinutes, avgUnderMinutes].
     */
    @Query("SELECT " +
            "COUNT(CASE WHEN e.etaErrorMinutes < 0 THEN 1 END), " +
            "COUNT(CASE WHEN e.etaErrorMinutes > 0 THEN 1 END), " +
            "AVG(CASE WHEN e.etaErrorMinutes < 0 THEN ABS(e.etaErrorMinutes) END), " +
            "AVG(CASE WHEN e.etaErrorMinutes > 0 THEN e.etaErrorMinutes END) " +
            "FROM EtaHistory e " +
            "WHERE e.wasCompleted = true " +
            "AND e.etaErrorMinutes IS NOT NULL " +
            "AND e.predictedAt >= :startTime AND e.predictedAt <= :endTime")
    Object[] getOverUnderEstimationStats(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get ETA error by hour of day.
     * Returns [hour, avgError, avgAbsError, count].
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM business_ts(predicted_at)) as hour, " +
            "AVG(eta_error_minutes) as avg_error, " +
            "AVG(eta_error_abs_minutes) as avg_abs_error, " +
            "COUNT(*) as count " +
            "FROM eta_history " +
            "WHERE was_completed = true " +
            "AND eta_error_minutes IS NOT NULL " +
            "AND predicted_at >= :startTime AND predicted_at <= :endTime " +
            "GROUP BY EXTRACT(HOUR FROM business_ts(predicted_at)) " +
            "ORDER BY hour", nativeQuery = true)
    List<Object[]> getErrorByHour(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get ETA error by day of week.
     * Returns [dayOfWeek, avgError, avgAbsError, count].
     */
    @Query(value = "SELECT EXTRACT(DOW FROM business_ts(predicted_at)) as dow, " +
            "AVG(eta_error_minutes) as avg_error, " +
            "AVG(eta_error_abs_minutes) as avg_abs_error, " +
            "COUNT(*) as count " +
            "FROM eta_history " +
            "WHERE was_completed = true " +
            "AND eta_error_minutes IS NOT NULL " +
            "AND predicted_at >= :startTime AND predicted_at <= :endTime " +
            "GROUP BY EXTRACT(DOW FROM business_ts(predicted_at)) " +
            "ORDER BY dow", nativeQuery = true)
    List<Object[]> getErrorByDayOfWeek(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get ETA accuracy by restaurant.
     * Returns [restaurantId, totalDeliveries, avgError, accuracyRate].
     */
    @Query(value = "SELECT restaurant_id, " +
            "COUNT(*) as total, " +
            "AVG(eta_error_abs_minutes) as avg_error, " +
            "COUNT(CASE WHEN eta_error_abs_minutes <= 5 THEN 1 END) * 100.0 / COUNT(*) as accuracy_rate " +
            "FROM eta_history " +
            "WHERE was_completed = true " +
            "AND eta_error_minutes IS NOT NULL " +
            "AND predicted_at >= :startTime AND predicted_at <= :endTime " +
            "GROUP BY restaurant_id " +
            "ORDER BY accuracy_rate DESC", nativeQuery = true)
    List<Object[]> getAccuracyByRestaurant(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get phase-level accuracy (preparation, pickup, delivery).
     * Returns [avgEstPrep, avgActPrep, avgEstPickup, avgActPickup, avgEstDelivery, avgActDelivery].
     */
    @Query("SELECT " +
            "AVG(e.etaPrepMinutes), AVG(e.actualPrepMinutes), " +
            "AVG(e.etaPickupMinutes), AVG(e.actualPickupMinutes), " +
            "AVG(e.etaDeliveryMinutes), AVG(e.actualTravelMinutes) " +
            "FROM EtaHistory e " +
            "WHERE e.wasCompleted = true " +
            "AND e.predictedAt >= :startTime AND e.predictedAt <= :endTime")
    Object[] getPhaseAccuracy(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get daily ETA metrics.
     * Returns [date, totalDeliveries, avgError, avgAbsError, within5Min, late, early].
     */
    @Query(value = "SELECT DATE(business_ts(predicted_at)) as day, " +
            "COUNT(*) as total, " +
            "AVG(eta_error_minutes) as avg_error, " +
            "AVG(eta_error_abs_minutes) as avg_abs_error, " +
            "COUNT(CASE WHEN eta_error_abs_minutes <= 5 THEN 1 END) as within_5_min, " +
            "COUNT(CASE WHEN eta_error_minutes > 0 THEN 1 END) as late, " +
            "COUNT(CASE WHEN eta_error_minutes < 0 THEN 1 END) as early " +
            "FROM eta_history " +
            "WHERE was_completed = true " +
            "AND eta_error_minutes IS NOT NULL " +
            "AND predicted_at >= :startTime AND predicted_at <= :endTime " +
            "GROUP BY DATE(business_ts(predicted_at)) " +
            "ORDER BY day", nativeQuery = true)
    List<Object[]> getDailyMetrics(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
