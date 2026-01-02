package com.fooddelivery.analytics.repository;

import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.courier.entity.CourierStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Courier analytics with optimized queries.
 */
@Repository
public interface AnalyticsCourierRepository extends JpaRepository<Courier, Long> {

    /**
     * Count couriers by status.
     */
    @Query("SELECT COUNT(c) FROM Courier c WHERE c.status = :status")
    Long countByStatus(@Param("status") CourierStatus status);

    /**
     * Count verified couriers.
     */
    @Query("SELECT COUNT(c) FROM Courier c WHERE c.verified = true")
    Long countVerifiedCouriers();

    /**
     * Count couriers who completed deliveries since a given time.
     */
    @Query("SELECT COUNT(DISTINCT o.courier.id) FROM Order o " +
            "WHERE o.courier IS NOT NULL " +
            "AND o.status IN ('DELIVERED', 'COMPLETED') " +
            "AND o.deliveredAt >= :since")
    Long countActiveCouriersSince(@Param("since") LocalDateTime since);

    /**
     * Count inactive couriers (no deliveries in period).
     */
    @Query(value = "SELECT COUNT(*) FROM couriers c " +
            "WHERE c.is_verified = true " +
            "AND c.id NOT IN (" +
            "  SELECT DISTINCT courier_id FROM orders " +
            "  WHERE courier_id IS NOT NULL " +
            "  AND status IN ('DELIVERED', 'COMPLETED') " +
            "  AND delivered_at >= :since" +
            ")", nativeQuery = true)
    Long countInactiveCouriersSince(@Param("since") LocalDateTime since);

    /**
     * Count couriers at risk (low activity in period).
     * Returns couriers who had some activity but below threshold.
     */
    @Query(value = "SELECT COUNT(*) FROM (" +
            "  SELECT c.id, COUNT(o.id) as delivery_count " +
            "  FROM couriers c " +
            "  LEFT JOIN orders o ON c.id = o.courier_id " +
            "    AND o.delivered_at >= :since " +
            "    AND o.status IN ('DELIVERED', 'COMPLETED') " +
            "  WHERE c.is_verified = true " +
            "  GROUP BY c.id " +
            "  HAVING COUNT(o.id) BETWEEN :minDeliveries AND :maxDeliveries" +
            ") as at_risk", nativeQuery = true)
    Long countCouriersAtRisk(
            @Param("since") LocalDateTime since,
            @Param("minDeliveries") int minDeliveries,
            @Param("maxDeliveries") int maxDeliveries);

    /**
     * Get courier performance summary.
     * Returns [courierId, deliveryCount, avgRating, totalEarnings].
     */
    @Query(value = "SELECT c.id, " +
            "COUNT(o.id) as delivery_count, " +
            "c.average_rating, " +
            "c.total_earnings " +
            "FROM couriers c " +
            "LEFT JOIN orders o ON c.id = o.courier_id " +
            "  AND o.delivered_at >= :since " +
            "  AND o.status IN ('DELIVERED', 'COMPLETED') " +
            "WHERE c.is_verified = true " +
            "GROUP BY c.id, c.average_rating, c.total_earnings " +
            "ORDER BY delivery_count DESC", nativeQuery = true)
    List<Object[]> getCourierPerformanceSince(@Param("since") LocalDateTime since);

    /**
     * Get courier activity by hour distribution.
     * Returns [hour, deliveryCount].
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM o.delivered_at) as hour, COUNT(*) as count " +
            "FROM orders o " +
            "WHERE o.courier_id IS NOT NULL " +
            "AND o.status IN ('DELIVERED', 'COMPLETED') " +
            "AND o.delivered_at >= :since " +
            "GROUP BY EXTRACT(HOUR FROM o.delivered_at) " +
            "ORDER BY hour", nativeQuery = true)
    List<Object[]> getCourierActivityByHour(@Param("since") LocalDateTime since);

    /**
     * Count couriers who updated their location recently.
     */
    @Query("SELECT COUNT(c) FROM Courier c " +
            "WHERE c.locationUpdatedAt >= :since")
    Long countCouriersWithRecentLocationUpdate(@Param("since") LocalDateTime since);
}
