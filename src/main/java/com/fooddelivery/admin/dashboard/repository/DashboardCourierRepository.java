package com.fooddelivery.admin.dashboard.repository;

import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.courier.entity.CourierStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for dashboard courier queries.
 * Provides optimized queries for courier metrics and analytics.
 */
@Repository
public interface DashboardCourierRepository extends JpaRepository<Courier, Long> {

    // ==================== Count Queries ====================

    /**
     * Count couriers by status.
     */
    Long countByStatus(CourierStatus status);

    /**
     * Count active couriers (location ping within last X minutes).
     */
    @Query("SELECT COUNT(c) FROM Courier c WHERE c.locationUpdatedAt > :since " +
            "AND c.status IN ('AVAILABLE', 'BUSY')")
    Long countActiveCouriers(@Param("since") LocalDateTime since);

    /**
     * Count available couriers.
     */
    @Query("SELECT COUNT(c) FROM Courier c WHERE c.status = 'AVAILABLE' " +
            "AND c.verified = true AND c.currentOrderCount < c.maxConcurrentOrders")
    Long countAvailableCouriers();

    /**
     * Count online couriers (not offline).
     */
    @Query("SELECT COUNT(c) FROM Courier c WHERE c.status != 'OFFLINE'")
    Long countOnlineCouriers();

    /**
     * Count couriers by vehicle type.
     */
    @Query("SELECT c.vehicleType, COUNT(c) FROM Courier c " +
            "WHERE c.status != 'OFFLINE' GROUP BY c.vehicleType")
    List<Object[]> countByVehicleType();

    // ==================== Active Couriers Queries ====================

    /**
     * Find active couriers with recent location updates.
     */
    @Query("SELECT c FROM Courier c WHERE c.locationUpdatedAt > :since " +
            "AND c.status IN ('AVAILABLE', 'BUSY') " +
            "ORDER BY c.locationUpdatedAt DESC")
    List<Courier> findActiveCouriers(@Param("since") LocalDateTime since);

    /**
     * Find couriers with their current locations.
     */
    @Query("SELECT c FROM Courier c WHERE c.status IN :statuses " +
            "AND c.currentLat IS NOT NULL AND c.currentLng IS NOT NULL " +
            "ORDER BY c.locationUpdatedAt DESC")
    List<Courier> findCouriersWithLocation(@Param("statuses") List<CourierStatus> statuses);

    /**
     * Find available couriers near a location.
     */
    @Query(value = "SELECT * FROM couriers c WHERE c.status = 'AVAILABLE' " +
            "AND c.verified = true AND c.current_order_count < c.max_concurrent_orders " +
            "AND c.current_lat IS NOT NULL AND c.current_lng IS NOT NULL " +
            "AND ST_Distance_Sphere(point(c.current_lng, c.current_lat), point(:lng, :lat)) <= :radiusMeters " +
            "ORDER BY ST_Distance_Sphere(point(c.current_lng, c.current_lat), point(:lng, :lat))",
            nativeQuery = true)
    List<Courier> findAvailableCouriersNear(@Param("lat") double lat,
                                             @Param("lng") double lng,
                                             @Param("radiusMeters") int radiusMeters);

    // ==================== List Queries ====================

    /**
     * Find couriers with optional status filter.
     */
    @Query("SELECT c FROM Courier c WHERE (:status IS NULL OR c.status = :status) " +
            "ORDER BY c.totalDeliveries DESC")
    Page<Courier> findByStatusOptional(@Param("status") CourierStatus status, Pageable pageable);

    /**
     * Find couriers with stale locations (haven't updated in X minutes).
     */
    @Query("SELECT c FROM Courier c WHERE c.status IN ('AVAILABLE', 'BUSY') " +
            "AND (c.locationUpdatedAt IS NULL OR c.locationUpdatedAt < :staleTime)")
    List<Courier> findCouriersWithStaleLocation(@Param("staleTime") LocalDateTime staleTime);

    // ==================== Performance Queries ====================

    /**
     * Top performing couriers by delivery count.
     */
    @Query("SELECT c FROM Courier c WHERE c.verified = true " +
            "ORDER BY c.totalDeliveries DESC")
    List<Courier> findTopCouriersByDeliveries(Pageable pageable);

    /**
     * Top rated couriers.
     */
    @Query("SELECT c FROM Courier c WHERE c.verified = true " +
            "AND c.totalRatings >= :minRatings " +
            "ORDER BY c.averageRating DESC")
    List<Courier> findTopRatedCouriers(@Param("minRatings") int minRatings, Pageable pageable);

    /**
     * Couriers with low acceptance rate.
     */
    @Query("SELECT c FROM Courier c WHERE c.verified = true " +
            "AND c.acceptanceRate < :threshold " +
            "ORDER BY c.acceptanceRate ASC")
    List<Courier> findLowAcceptanceCouriers(@Param("threshold") java.math.BigDecimal threshold,
                                             Pageable pageable);

    /**
     * Courier performance summary.
     */
    @Query("SELECT c.id, c.status, c.vehicleType, c.totalDeliveries, c.averageRating, " +
            "c.totalRatings, c.acceptanceRate, c.totalEarnings, c.currentOrderCount " +
            "FROM Courier c WHERE c.verified = true " +
            "ORDER BY c.totalDeliveries DESC")
    List<Object[]> getCourierPerformanceSummary(Pageable pageable);

    // ==================== Availability Queries ====================

    /**
     * Hourly courier availability.
     * Returns hour and count of available couriers.
     */
    @Query(value = "SELECT HOUR(location_updated_at), COUNT(DISTINCT id) " +
            "FROM couriers WHERE status IN ('AVAILABLE', 'BUSY') " +
            "AND DATE(location_updated_at) = DATE(:date) " +
            "GROUP BY HOUR(location_updated_at) " +
            "ORDER BY HOUR(location_updated_at)",
            nativeQuery = true)
    List<Object[]> courierAvailabilityByHour(@Param("date") LocalDateTime date);

    // ==================== Earnings Queries ====================

    /**
     * Top earning couriers within date range.
     */
    @Query("SELECT c FROM Courier c WHERE c.verified = true " +
            "ORDER BY c.totalEarnings DESC")
    List<Courier> findTopEarningCouriers(Pageable pageable);
}
