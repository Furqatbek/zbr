package com.fooddelivery.admin.dashboard.repository;

import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.entity.RestaurantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for dashboard restaurant queries.
 * Provides optimized queries for restaurant metrics and analytics.
 */
@Repository
public interface DashboardRestaurantRepository extends JpaRepository<Restaurant, Long> {

    // ==================== Count Queries ====================

    /**
     * Count restaurants by status.
     */
    Long countByStatus(RestaurantStatus status);

    /**
     * Count online restaurants (accepting orders).
     */
    @Query("SELECT COUNT(r) FROM Restaurant r WHERE r.isOpen = true AND r.status = 'ACTIVE'")
    Long countOnlineRestaurants();

    /**
     * Count restaurants accepting orders (open and active).
     */
    @Query("SELECT COUNT(r) FROM Restaurant r WHERE r.isOpen = true AND r.status = 'ACTIVE' " +
            "AND r.acceptsDelivery = true")
    Long countAcceptingOrders();

    // ==================== Active Restaurants Queries ====================

    /**
     * Find restaurants that accepted orders in the last X minutes.
     * These are considered "active" restaurants.
     */
    @Query("SELECT DISTINCT o.restaurant FROM Order o " +
            "WHERE o.acceptedAt > :since AND o.restaurant.status = 'ACTIVE'")
    List<Restaurant> findActiveRestaurants(@Param("since") LocalDateTime since);

    /**
     * Count active restaurants (accepted orders recently).
     */
    @Query("SELECT COUNT(DISTINCT o.restaurant) FROM Order o " +
            "WHERE o.acceptedAt > :since AND o.restaurant.status = 'ACTIVE'")
    Long countActiveRestaurants(@Param("since") LocalDateTime since);

    // ==================== List Queries ====================

    /**
     * Find all restaurants with optional status filter.
     */
    @Query("SELECT r FROM Restaurant r WHERE (:status IS NULL OR r.status = :status) " +
            "ORDER BY r.name")
    Page<Restaurant> findByStatusOptional(@Param("status") RestaurantStatus status, Pageable pageable);

    /**
     * Find online restaurants.
     */
    @Query("SELECT r FROM Restaurant r WHERE r.isOpen = true AND r.status = 'ACTIVE' " +
            "ORDER BY r.name")
    Page<Restaurant> findOnlineRestaurants(Pageable pageable);

    /**
     * Search restaurants by name.
     */
    @Query("SELECT r FROM Restaurant r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Restaurant> searchByName(@Param("name") String name, Pageable pageable);

    // ==================== Performance Queries ====================

    /**
     * Restaurant performance summary.
     * Returns: restaurantId, name, totalOrders, completedOrders, avgRating, avgPrepTime
     */
    @Query("SELECT r.id, r.name, r.status, r.isOpen, r.totalOrders, r.averageRating, " +
            "r.totalRatings, r.averagePrepTimeMinutes " +
            "FROM Restaurant r WHERE r.status = 'ACTIVE' " +
            "ORDER BY r.totalOrders DESC")
    List<Object[]> getRestaurantPerformanceSummary(Pageable pageable);

    /**
     * Top performing restaurants by order count.
     */
    @Query("SELECT r FROM Restaurant r WHERE r.status = 'ACTIVE' " +
            "ORDER BY r.totalOrders DESC")
    List<Restaurant> findTopRestaurantsByOrders(Pageable pageable);

    /**
     * Top rated restaurants.
     */
    @Query("SELECT r FROM Restaurant r WHERE r.status = 'ACTIVE' " +
            "AND r.totalRatings >= :minRatings " +
            "ORDER BY r.averageRating DESC")
    List<Restaurant> findTopRatedRestaurants(@Param("minRatings") int minRatings, Pageable pageable);

    /**
     * Under-performing restaurants (high prep time, low rating).
     */
    @Query("SELECT r FROM Restaurant r WHERE r.status = 'ACTIVE' " +
            "AND (r.averagePrepTimeMinutes > :maxPrepTime OR r.averageRating < :minRating) " +
            "ORDER BY r.averageRating ASC")
    List<Restaurant> findUnderPerformingRestaurants(@Param("maxPrepTime") int maxPrepTime,
                                                     @Param("minRating") java.math.BigDecimal minRating,
                                                     Pageable pageable);

    // ==================== Geographic Queries ====================

    /**
     * Find restaurants near a location.
     */
    @Query(value = "SELECT * FROM restaurants r WHERE r.status = 'ACTIVE' " +
            "AND ST_Distance_Sphere(point(r.longitude, r.latitude), point(:lng, :lat)) <= :radiusMeters",
            nativeQuery = true)
    List<Restaurant> findNearbyRestaurants(@Param("lat") double lat,
                                            @Param("lng") double lng,
                                            @Param("radiusMeters") int radiusMeters);

    // ==================== Status Change Queries ====================

    /**
     * Find restaurants that recently went offline.
     */
    @Query("SELECT r FROM Restaurant r WHERE r.isOpen = false " +
            "AND r.status = 'ACTIVE' AND r.updatedAt > :since")
    List<Restaurant> findRecentlyOfflineRestaurants(@Param("since") LocalDateTime since);

    // ==================== Dashboard Collector Queries ====================

    /**
     * Count total restaurants.
     */
    @Query("SELECT COUNT(r) FROM Restaurant r")
    Long countTotalRestaurants();

    /**
     * Count offline restaurants.
     */
    @Query("SELECT COUNT(r) FROM Restaurant r WHERE r.isOpen = false")
    Long countOfflineRestaurants();

    /**
     * Count busy restaurants.
     */
    @Query("SELECT COUNT(r) FROM Restaurant r WHERE r.status = 'BUSY'")
    Long countBusyRestaurants();

    /**
     * Count temporarily closed restaurants.
     */
    @Query("SELECT COUNT(r) FROM Restaurant r WHERE r.status = 'TEMPORARILY_CLOSED'")
    Long countTemporarilyClosedRestaurants();

    /**
     * Average preparation time across all restaurants.
     */
    @Query("SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, o.acceptedAt, o.readyAt)), 0) " +
            "FROM Order o WHERE o.readyAt IS NOT NULL AND o.acceptedAt IS NOT NULL " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    Double avgPreparationTime(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    /**
     * Average order acceptance latency in seconds.
     */
    @Query("SELECT COALESCE(AVG(TIMESTAMPDIFF(SECOND, o.createdAt, o.acceptedAt)), 0) " +
            "FROM Order o WHERE o.acceptedAt IS NOT NULL " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    Double avgOrderAcceptanceLatency(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Average restaurant rating.
     */
    @Query("SELECT COALESCE(AVG(r.averageRating), 0) FROM Restaurant r WHERE r.status = 'ACTIVE'")
    Double avgRestaurantRating();

    /**
     * Average order acceptance rate.
     */
    @Query("SELECT COALESCE(AVG(CASE WHEN o.status NOT IN ('REJECTED', 'CANCELLED') THEN 100.0 ELSE 0.0 END), 0) " +
            "FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    Double avgOrderAcceptanceRate(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Find restaurant details with filters.
     */
    @Query("SELECT r.id, r.name, r.status, r.averageRating, r.totalOrders, r.averagePrepTimeMinutes, " +
            "0, r.totalOrders, 0, r.cuisineType, r.city, r.updatedAt, r.isOpen " +
            "FROM Restaurant r WHERE r.id IN :restaurantIds " +
            "ORDER BY r.name")
    List<Object[]> findRestaurantDetailsFiltered(@Param("restaurantIds") List<Long> restaurantIds,
                                                  Pageable pageable);

    /**
     * Find all restaurant details.
     */
    @Query("SELECT r.id, r.name, r.status, r.averageRating, r.totalOrders, r.averagePrepTimeMinutes, " +
            "0, r.totalOrders, 0, r.cuisineType, r.city, r.updatedAt, r.isOpen " +
            "FROM Restaurant r WHERE r.status = 'ACTIVE' " +
            "ORDER BY r.name")
    List<Object[]> findAllRestaurantDetails(Pageable pageable);

    /**
     * Count restaurants by cuisine type.
     */
    @Query("SELECT r.cuisineType, COUNT(r) FROM Restaurant r WHERE r.status = 'ACTIVE' " +
            "GROUP BY r.cuisineType ORDER BY COUNT(r) DESC")
    List<Object[]> countByCuisineType();

    /**
     * Find top performing restaurants by rating and order count.
     */
    @Query("SELECT r.id, r.name, r.status, r.averageRating, r.totalOrders, r.averagePrepTimeMinutes, " +
            "0, r.totalOrders, 0, r.cuisineType, r.city, r.updatedAt, r.isOpen " +
            "FROM Restaurant r WHERE r.status = 'ACTIVE' " +
            "ORDER BY r.averageRating DESC, r.totalOrders DESC")
    List<Object[]> findTopPerformingRestaurants(Pageable pageable);

    /**
     * Find underperforming restaurants.
     */
    @Query("SELECT r.id, r.name, r.status, r.averageRating, r.totalOrders, r.averagePrepTimeMinutes, " +
            "0, r.totalOrders, 0, r.cuisineType, r.city, r.updatedAt, r.isOpen " +
            "FROM Restaurant r WHERE r.status = 'ACTIVE' " +
            "AND (r.averageRating < :minRating OR r.totalOrders < :minOrders) " +
            "ORDER BY r.averageRating ASC")
    List<Object[]> findUnderperformingRestaurants(@Param("minRating") Double minRating,
                                                   @Param("minOrders") Double minOrders,
                                                   Pageable pageable);

    /**
     * Count restaurants by city.
     */
    @Query("SELECT r.city, COUNT(r) FROM Restaurant r WHERE r.status = 'ACTIVE' " +
            "GROUP BY r.city ORDER BY COUNT(r) DESC")
    List<Object[]> countByCity();
}
