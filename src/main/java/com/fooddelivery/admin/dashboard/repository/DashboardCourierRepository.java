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

    // ==================== CourierCollector Required Methods ====================

    /**
     * Count total couriers.
     */
    default Long countTotalCouriers() {
        return count();
    }

    /**
     * Count couriers on delivery.
     */
    @Query("SELECT COUNT(c) FROM Courier c WHERE c.status = 'BUSY' AND c.currentOrderCount > 0")
    Long countCouriersOnDelivery();

    /**
     * Count available couriers with timestamp filter.
     */
    @Query("SELECT COUNT(c) FROM Courier c WHERE c.status = 'AVAILABLE' " +
            "AND c.verified = true AND c.locationUpdatedAt > :since")
    Long countAvailableCouriers(@Param("since") LocalDateTime since);

    /**
     * Count couriers returning.
     */
    @Query("SELECT COUNT(c) FROM Courier c WHERE c.status = 'RETURNING'")
    Long countCouriersReturning();

    /**
     * Count couriers on break.
     */
    @Query("SELECT COUNT(c) FROM Courier c WHERE c.status = 'ON_BREAK'")
    Long countCouriersOnBreak();

    /**
     * Count offline couriers.
     */
    @Query("SELECT COUNT(c) FROM Courier c WHERE c.status = 'OFFLINE' " +
            "OR c.locationUpdatedAt < :since")
    Long countOfflineCouriers(@Param("since") LocalDateTime since);

    /**
     * Average delivery time.
     */
    @Query("SELECT COALESCE(AVG(c.avgDeliveryTimeMinutes), 0) FROM Courier c " +
            "WHERE c.verified = true AND c.totalDeliveries > 0")
    Double avgDeliveryTime(@Param("startDate") LocalDateTime startDate,
                           @Param("endDate") LocalDateTime endDate);

    /**
     * Average courier rating.
     */
    @Query("SELECT COALESCE(AVG(c.averageRating), 0) FROM Courier c " +
            "WHERE c.verified = true AND c.totalRatings > 0")
    Double avgCourierRating();

    /**
     * Count total deliveries within date range.
     */
    @Query("SELECT COALESCE(SUM(c.totalDeliveries), 0) FROM Courier c WHERE c.verified = true")
    Long countTotalDeliveries(@Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate);

    /**
     * Count on-time deliveries within date range.
     */
    @Query("SELECT COALESCE(SUM(c.onTimeDeliveries), 0) FROM Courier c WHERE c.verified = true")
    Long countOnTimeDeliveries(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    /**
     * Average deliveries per courier.
     */
    @Query("SELECT COALESCE(AVG(c.totalDeliveries), 0) FROM Courier c WHERE c.verified = true")
    Double avgDeliveriesPerCourier(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Average distance per delivery.
     */
    @Query("SELECT COALESCE(AVG(c.avgDistancePerDeliveryKm), 0) FROM Courier c WHERE c.verified = true")
    Double avgDistancePerDelivery(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Find courier details with filtering.
     */
    @Query("SELECT c.id, CONCAT(c.firstName, ' ', c.lastName), c.status, c.averageRating, " +
            "c.totalDeliveries, c.avgDeliveryTimeMinutes, c.onTimeDeliveries, c.vehicleType, " +
            "c.currentLat, c.currentLng, c.locationUpdatedAt, c.currentOrderId, " +
            "(c.locationUpdatedAt > :activeThreshold) " +
            "FROM Courier c WHERE c.id IN :courierIds AND c.verified = true")
    List<Object[]> findCourierDetailsFiltered(@Param("courierIds") List<Long> courierIds,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               @Param("activeThreshold") LocalDateTime activeThreshold,
                                               Pageable pageable);

    /**
     * Find all courier details.
     */
    @Query("SELECT c.id, CONCAT(c.firstName, ' ', c.lastName), c.status, c.averageRating, " +
            "c.totalDeliveries, c.avgDeliveryTimeMinutes, c.onTimeDeliveries, c.vehicleType, " +
            "c.currentLat, c.currentLng, c.locationUpdatedAt, c.currentOrderId, " +
            "(c.locationUpdatedAt > :activeThreshold) " +
            "FROM Courier c WHERE c.verified = true")
    List<Object[]> findAllCourierDetails(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          @Param("activeThreshold") LocalDateTime activeThreshold,
                                          Pageable pageable);

    /**
     * Find active courier locations.
     */
    @Query("SELECT c.id, CONCAT(c.firstName, ' ', c.lastName), c.currentLat, c.currentLng, " +
            "c.status, c.currentOrderId, c.locationUpdatedAt " +
            "FROM Courier c WHERE c.locationUpdatedAt > :since " +
            "AND c.currentLat IS NOT NULL AND c.currentLng IS NOT NULL")
    List<Object[]> findActiveCourierLocations(@Param("since") LocalDateTime since);

    /**
     * Get courier availability by hour.
     */
    @Query(value = "SELECT DAYOFWEEK(location_updated_at), HOUR(location_updated_at), COUNT(DISTINCT id) " +
            "FROM couriers WHERE status IN ('AVAILABLE', 'BUSY') " +
            "AND location_updated_at BETWEEN :startDate AND :endDate " +
            "GROUP BY DAYOFWEEK(location_updated_at), HOUR(location_updated_at)",
            nativeQuery = true)
    List<Object[]> getCourierAvailabilityByHour(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Find top performing couriers.
     */
    @Query("SELECT c.id, CONCAT(c.firstName, ' ', c.lastName), c.status, c.averageRating, " +
            "c.totalDeliveries, c.avgDeliveryTimeMinutes, c.onTimeDeliveries, c.vehicleType, " +
            "c.currentLat, c.currentLng, c.locationUpdatedAt, c.currentOrderId, " +
            "(c.locationUpdatedAt > :activeThreshold) " +
            "FROM Courier c WHERE c.verified = true " +
            "ORDER BY c.averageRating DESC, c.totalDeliveries DESC")
    List<Object[]> findTopPerformingCouriers(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate,
                                              @Param("activeThreshold") LocalDateTime activeThreshold,
                                              Pageable pageable);
}
