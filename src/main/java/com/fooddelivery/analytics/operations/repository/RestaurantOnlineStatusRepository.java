package com.fooddelivery.analytics.operations.repository;

import com.fooddelivery.analytics.operations.model.RestaurantOnlineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for restaurant online status analytics.
 */
@Repository
public interface RestaurantOnlineStatusRepository extends JpaRepository<RestaurantOnlineStatus, Long> {

    /**
     * Get total offline time for a restaurant in minutes.
     */
    @Query(value = "SELECT COALESCE(SUM(previous_status_duration_minutes), 0) " +
            "FROM restaurant_online_status " +
            "WHERE restaurant_id = :restaurantId " +
            "AND is_online = true " +  // Sum duration when coming back online (was offline)
            "AND status_changed_at >= :startTime AND status_changed_at <= :endTime", nativeQuery = true)
    Long getTotalOfflineTimeMinutes(
            @Param("restaurantId") Long restaurantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Count number of times restaurant went offline.
     */
    @Query("SELECT COUNT(s) FROM RestaurantOnlineStatus s " +
            "WHERE s.restaurantId = :restaurantId " +
            "AND s.isOnline = false " +
            "AND s.statusChangedAt >= :startTime AND s.statusChangedAt <= :endTime")
    Integer countOfflineEvents(
            @Param("restaurantId") Long restaurantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get last offline time for a restaurant.
     */
    @Query(value = "SELECT status_changed_at FROM restaurant_online_status " +
            "WHERE restaurant_id = :restaurantId " +
            "AND is_online = false " +
            "ORDER BY status_changed_at DESC LIMIT 1", nativeQuery = true)
    LocalDateTime getLastOfflineTime(@Param("restaurantId") Long restaurantId);

    /**
     * Get offline reasons for a restaurant.
     * Returns [reason, count].
     */
    @Query("SELECT s.reason, COUNT(s) FROM RestaurantOnlineStatus s " +
            "WHERE s.restaurantId = :restaurantId " +
            "AND s.isOnline = false " +
            "AND s.statusChangedAt >= :startTime AND s.statusChangedAt <= :endTime " +
            "GROUP BY s.reason")
    List<Object[]> getOfflineReasons(
            @Param("restaurantId") Long restaurantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get daily offline metrics for a restaurant.
     * Returns [date, offlineMinutes, offlineCount].
     */
    @Query(value = "SELECT DATE(status_changed_at) as day, " +
            "COALESCE(SUM(CASE WHEN is_online = true THEN previous_status_duration_minutes ELSE 0 END), 0) as offline_minutes, " +
            "COUNT(CASE WHEN is_online = false THEN 1 END) as offline_count " +
            "FROM restaurant_online_status " +
            "WHERE restaurant_id = :restaurantId " +
            "AND status_changed_at >= :startTime AND status_changed_at <= :endTime " +
            "GROUP BY DATE(status_changed_at) " +
            "ORDER BY day", nativeQuery = true)
    List<Object[]> getDailyOfflineMetrics(
            @Param("restaurantId") Long restaurantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get average uptime percentage across all restaurants.
     */
    @Query(value = "WITH restaurant_metrics AS (" +
            "  SELECT restaurant_id, " +
            "         SUM(CASE WHEN is_online = true THEN previous_status_duration_minutes ELSE 0 END) as offline_minutes " +
            "  FROM restaurant_online_status " +
            "  WHERE status_changed_at >= :startTime AND status_changed_at <= :endTime " +
            "  GROUP BY restaurant_id" +
            ") " +
            "SELECT AVG(100.0 - (offline_minutes * 100.0 / :totalMinutes)) " +
            "FROM restaurant_metrics " +
            "WHERE offline_minutes IS NOT NULL", nativeQuery = true)
    Double getAverageUptimePercentage(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("totalMinutes") Long totalMinutes);
}
