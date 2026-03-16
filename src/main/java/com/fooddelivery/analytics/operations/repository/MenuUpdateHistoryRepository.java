package com.fooddelivery.analytics.operations.repository;

import com.fooddelivery.analytics.operations.model.MenuUpdateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for menu update history analytics.
 */
@Repository
public interface MenuUpdateHistoryRepository extends JpaRepository<MenuUpdateHistory, Long> {

    /**
     * Count total menu updates for a restaurant in a time range.
     */
    @Query("SELECT COUNT(m) FROM MenuUpdateHistory m " +
            "WHERE m.restaurantId = :restaurantId " +
            "AND m.updatedAt >= :startTime AND m.updatedAt <= :endTime")
    Long countByRestaurantIdAndTimeRange(
            @Param("restaurantId") Long restaurantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get last menu update time for a restaurant.
     */
    @Query(value = "SELECT updated_at FROM menu_update_history " +
            "WHERE restaurant_id = :restaurantId " +
            "ORDER BY updated_at DESC LIMIT 1", nativeQuery = true)
    LocalDateTime getLastMenuUpdateTime(@Param("restaurantId") Long restaurantId);

    /**
     * Get menu update summary for a restaurant.
     * Returns [totalUpdates, itemsAdded, itemsRemoved, itemsModified, pricesUpdated].
     */
    @Query("SELECT COUNT(m), " +
            "COALESCE(SUM(m.itemsAdded), 0), " +
            "COALESCE(SUM(m.itemsRemoved), 0), " +
            "COALESCE(SUM(m.itemsModified), 0), " +
            "COALESCE(SUM(m.pricesUpdated), 0) " +
            "FROM MenuUpdateHistory m " +
            "WHERE m.restaurantId = :restaurantId " +
            "AND m.updatedAt >= :startTime AND m.updatedAt <= :endTime")
    Object[] getMenuUpdateSummary(
            @Param("restaurantId") Long restaurantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get daily menu update counts for a restaurant.
     * Returns [date, count].
     */
    @Query(value = "SELECT DATE(updated_at) as day, COUNT(*) as count " +
            "FROM menu_update_history " +
            "WHERE restaurant_id = :restaurantId " +
            "AND updated_at >= :startTime AND updated_at <= :endTime " +
            "GROUP BY DATE(updated_at) " +
            "ORDER BY day", nativeQuery = true)
    List<Object[]> getDailyMenuUpdates(
            @Param("restaurantId") Long restaurantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get update counts by type for a restaurant.
     * Returns [updateType, count].
     */
    @Query("SELECT m.updateType, COUNT(m) FROM MenuUpdateHistory m " +
            "WHERE m.restaurantId = :restaurantId " +
            "AND m.updatedAt >= :startTime AND m.updatedAt <= :endTime " +
            "GROUP BY m.updateType")
    List<Object[]> getUpdateCountsByType(
            @Param("restaurantId") Long restaurantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get average menu updates per day across all restaurants.
     */
    @Query(value = "SELECT AVG(daily_updates) FROM (" +
            "  SELECT restaurant_id, DATE(updated_at) as day, COUNT(*) as daily_updates " +
            "  FROM menu_update_history " +
            "  WHERE updated_at >= :startTime AND updated_at <= :endTime " +
            "  GROUP BY restaurant_id, DATE(updated_at)" +
            ") as daily_counts", nativeQuery = true)
    Double getAverageMenuUpdatesPerDay(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
