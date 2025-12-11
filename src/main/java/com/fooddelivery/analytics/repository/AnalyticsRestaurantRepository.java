package com.fooddelivery.analytics.repository;

import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.entity.RestaurantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Restaurant analytics with optimized queries.
 */
@Repository
public interface AnalyticsRestaurantRepository extends JpaRepository<Restaurant, Long> {

    /**
     * Count restaurants by status.
     */
    @Query("SELECT COUNT(r) FROM Restaurant r WHERE r.status = :status")
    Long countByStatus(@Param("status") RestaurantStatus status);

    /**
     * Count active restaurants (status = ACTIVE).
     */
    default Long countActiveRestaurants() {
        return countByStatus(RestaurantStatus.ACTIVE);
    }

    /**
     * Count restaurants with orders since a given time.
     */
    @Query("SELECT COUNT(DISTINCT o.restaurant.id) FROM Order o " +
            "WHERE o.createdAt >= :since")
    Long countRestaurantsWithOrdersSince(@Param("since") LocalDateTime since);

    /**
     * Count restaurants with zero orders since a given time (churned).
     */
    @Query(value = "SELECT COUNT(*) FROM restaurants r " +
            "WHERE r.status = 'ACTIVE' " +
            "AND r.id NOT IN (" +
            "  SELECT DISTINCT restaurant_id FROM orders " +
            "  WHERE created_at >= :since" +
            ")", nativeQuery = true)
    Long countRestaurantsWithNoOrdersSince(@Param("since") LocalDateTime since);

    /**
     * Get restaurants at risk (low order count in period).
     */
    @Query(value = "SELECT COUNT(*) FROM (" +
            "  SELECT r.id, COUNT(o.id) as order_count " +
            "  FROM restaurants r " +
            "  LEFT JOIN orders o ON r.id = o.restaurant_id " +
            "    AND o.created_at >= :since " +
            "  WHERE r.status = 'ACTIVE' " +
            "  GROUP BY r.id " +
            "  HAVING COUNT(o.id) BETWEEN 1 AND :threshold" +
            ") as at_risk", nativeQuery = true)
    Long countRestaurantsAtRisk(
            @Param("since") LocalDateTime since,
            @Param("threshold") int threshold);

    /**
     * Get restaurant performance summary.
     * Returns [restaurantId, orderCount, revenue, avgRating].
     */
    @Query(value = "SELECT r.id, " +
            "COUNT(o.id) as order_count, " +
            "COALESCE(SUM(o.total), 0) as revenue, " +
            "r.average_rating " +
            "FROM restaurants r " +
            "LEFT JOIN orders o ON r.id = o.restaurant_id " +
            "  AND o.created_at >= :since " +
            "  AND o.status IN ('DELIVERED', 'COMPLETED') " +
            "WHERE r.status = 'ACTIVE' " +
            "GROUP BY r.id, r.average_rating " +
            "ORDER BY order_count DESC", nativeQuery = true)
    List<Object[]> getRestaurantPerformanceSince(@Param("since") LocalDateTime since);

    /**
     * Get restaurant registration by month (cohort).
     * Returns [cohortMonth, count].
     */
    @Query(value = "SELECT TO_CHAR(created_at, 'YYYY-MM') as cohort_month, COUNT(*) " +
            "FROM restaurants " +
            "WHERE created_at >= :since " +
            "GROUP BY TO_CHAR(created_at, 'YYYY-MM') " +
            "ORDER BY cohort_month", nativeQuery = true)
    List<Object[]> getRestaurantRegistrationsByCohort(@Param("since") LocalDateTime since);

    /**
     * Get restaurant retention cohorts.
     */
    @Query(value = "WITH cohorts AS (" +
            "  SELECT " +
            "    r.id as restaurant_id, " +
            "    TO_CHAR(r.created_at, 'YYYY-MM') as cohort_month, " +
            "    r.created_at as registration_date " +
            "  FROM restaurants r " +
            "  WHERE r.created_at >= :since AND r.status = 'ACTIVE' " +
            "), " +
            "restaurant_activity AS (" +
            "  SELECT " +
            "    c.restaurant_id, " +
            "    c.cohort_month, " +
            "    c.registration_date, " +
            "    MAX(o.created_at) as last_order_date " +
            "  FROM cohorts c " +
            "  LEFT JOIN orders o ON c.restaurant_id = o.restaurant_id " +
            "  GROUP BY c.restaurant_id, c.cohort_month, c.registration_date " +
            ") " +
            "SELECT " +
            "  cohort_month, " +
            "  COUNT(*) as cohort_size, " +
            "  COUNT(CASE WHEN last_order_date >= registration_date + INTERVAL '30 days' THEN 1 END) as active_30d, " +
            "  COUNT(CASE WHEN last_order_date >= registration_date + INTERVAL '60 days' THEN 1 END) as active_60d, " +
            "  COUNT(CASE WHEN last_order_date >= registration_date + INTERVAL '90 days' THEN 1 END) as active_90d " +
            "FROM restaurant_activity " +
            "GROUP BY cohort_month " +
            "ORDER BY cohort_month", nativeQuery = true)
    List<Object[]> getRestaurantRetentionCohorts(@Param("since") LocalDateTime since);
}
