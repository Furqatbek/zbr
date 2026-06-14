package com.fooddelivery.analytics.financial.repository;

import com.fooddelivery.analytics.financial.model.CommissionType;
import com.fooddelivery.analytics.financial.model.RestaurantCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for restaurant commission data with optimized analytical queries.
 */
@Repository
public interface RestaurantCommissionRepository extends JpaRepository<RestaurantCommission, Long> {

    /**
     * Check if a commission record already exists for an order (idempotency guard).
     */
    boolean existsByOrderId(Long orderId);

    /**
     * Restaurant-scoped revenue summary for a period.
     * Returns: [ sum(orderSubtotal), count(orders), sum(commissionAmount) ].
     */
    @Query("SELECT COALESCE(SUM(rc.orderSubtotal), 0), COUNT(rc), COALESCE(SUM(rc.commissionAmount), 0) " +
           "FROM RestaurantCommission rc " +
           "WHERE rc.restaurantId = :restaurantId AND rc.earnedAt BETWEEN :startDate AND :endDate")
    Object[] getRestaurantRevenueSummary(@Param("restaurantId") Long restaurantId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * Get total commission for a period.
     */
    @Query("SELECT COALESCE(SUM(rc.commissionAmount), 0) FROM RestaurantCommission rc " +
           "WHERE rc.earnedAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalCommission(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Get commission metrics aggregated.
     */
    @Query("SELECT COALESCE(SUM(rc.commissionAmount), 0), COUNT(rc), " +
           "COALESCE(AVG(rc.commissionAmount), 0), COALESCE(AVG(rc.commissionRate), 0) " +
           "FROM RestaurantCommission rc " +
           "WHERE rc.earnedAt BETWEEN :startDate AND :endDate")
    Object[] getCommissionMetrics(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Get commission breakdown by type.
     */
    @Query("SELECT rc.commissionType, SUM(rc.commissionAmount), COUNT(rc) " +
           "FROM RestaurantCommission rc " +
           "WHERE rc.earnedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY rc.commissionType")
    List<Object[]> getCommissionByType(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Get top restaurants by commission generated.
     */
    @Query("SELECT rc.restaurantId, SUM(rc.commissionAmount), AVG(rc.commissionRate), COUNT(rc), " +
           "AVG(rc.orderSubtotal) " +
           "FROM RestaurantCommission rc " +
           "WHERE rc.earnedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY rc.restaurantId " +
           "ORDER BY SUM(rc.commissionAmount) DESC")
    List<Object[]> getTopRestaurantsByCommission(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Get daily commission trend.
     */
    @Query(value = "SELECT DATE(earned_at) as date, SUM(commission_amount), COUNT(*), AVG(commission_amount) " +
                   "FROM restaurant_commissions " +
                   "WHERE earned_at BETWEEN :startDate AND :endDate " +
                   "GROUP BY DATE(earned_at) " +
                   "ORDER BY date", nativeQuery = true)
    List<Object[]> getDailyCommissionTrend(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Get percentage-based vs fixed commission breakdown.
     */
    @Query("SELECT " +
           "SUM(CASE WHEN rc.commissionType = 'PERCENTAGE' THEN rc.commissionAmount ELSE 0 END), " +
           "SUM(CASE WHEN rc.commissionType = 'FIXED' THEN rc.commissionAmount ELSE 0 END), " +
           "SUM(CASE WHEN rc.commissionType = 'HYBRID' THEN rc.commissionAmount ELSE 0 END) " +
           "FROM RestaurantCommission rc " +
           "WHERE rc.earnedAt BETWEEN :startDate AND :endDate")
    Object[] getCommissionByCalculationType(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Get commission for specific restaurants.
     */
    @Query("SELECT rc.restaurantId, SUM(rc.commissionAmount), COUNT(rc) " +
           "FROM RestaurantCommission rc " +
           "WHERE rc.earnedAt BETWEEN :startDate AND :endDate " +
           "AND rc.restaurantId IN :restaurantIds " +
           "GROUP BY rc.restaurantId")
    List<Object[]> getCommissionForRestaurants(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               @Param("restaurantIds") List<Long> restaurantIds);

    /**
     * Get total order subtotal (for GMV calculation).
     */
    @Query("SELECT COALESCE(SUM(rc.orderSubtotal), 0) FROM RestaurantCommission rc " +
           "WHERE rc.earnedAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalOrderSubtotal(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Count orders with commission in period.
     */
    @Query("SELECT COUNT(rc) FROM RestaurantCommission rc " +
           "WHERE rc.earnedAt BETWEEN :startDate AND :endDate")
    Long getOrderCount(@Param("startDate") LocalDateTime startDate,
                       @Param("endDate") LocalDateTime endDate);

    List<RestaurantCommission> findByRestaurantIdAndEarnedAtBetween(
            Long restaurantId, LocalDateTime startDate, LocalDateTime endDate);
}
