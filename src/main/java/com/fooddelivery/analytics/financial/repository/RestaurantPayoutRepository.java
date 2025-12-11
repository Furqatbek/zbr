package com.fooddelivery.analytics.financial.repository;

import com.fooddelivery.analytics.financial.model.PaymentStatus;
import com.fooddelivery.analytics.financial.model.RestaurantPayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for restaurant payout data with optimized analytical queries.
 */
@Repository
public interface RestaurantPayoutRepository extends JpaRepository<RestaurantPayout, Long> {

    /**
     * Get restaurant payout metrics aggregated.
     */
    @Query("SELECT COALESCE(SUM(rp.grossAmount), 0), " +
           "COALESCE(SUM(rp.commissionDeducted), 0), " +
           "COALESCE(SUM(rp.deliverySubsidies), 0), " +
           "COALESCE(SUM(rp.promotionCosts), 0), " +
           "COALESCE(SUM(rp.adjustments), 0), " +
           "COALESCE(SUM(rp.fees), 0), " +
           "COALESCE(SUM(rp.netPayout), 0), " +
           "COUNT(DISTINCT rp.restaurantId), " +
           "COUNT(rp) " +
           "FROM RestaurantPayout rp " +
           "WHERE rp.periodStart >= :startDate AND rp.periodEnd <= :endDate")
    Object[] getPayoutMetrics(@Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate);

    /**
     * Get payouts by status.
     */
    @Query("SELECT rp.status, SUM(rp.netPayout), COUNT(rp) " +
           "FROM RestaurantPayout rp " +
           "WHERE rp.periodStart >= :startDate AND rp.periodEnd <= :endDate " +
           "GROUP BY rp.status")
    List<Object[]> getPayoutsByStatus(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Get top restaurants by payout.
     */
    @Query("SELECT rp.restaurantId, SUM(rp.grossAmount), SUM(rp.commissionDeducted), " +
           "SUM(rp.netPayout), COUNT(rp) " +
           "FROM RestaurantPayout rp " +
           "WHERE rp.periodStart >= :startDate AND rp.periodEnd <= :endDate " +
           "GROUP BY rp.restaurantId " +
           "ORDER BY SUM(rp.netPayout) DESC")
    List<Object[]> getTopRestaurantsByPayout(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    /**
     * Get daily payout trend.
     */
    @Query(value = "SELECT DATE(period_start) as date, " +
                   "SUM(gross_amount), SUM(net_payout), COUNT(DISTINCT restaurant_id) " +
                   "FROM restaurant_payouts " +
                   "WHERE period_start >= :startDate AND period_end <= :endDate " +
                   "GROUP BY DATE(period_start) " +
                   "ORDER BY date", nativeQuery = true)
    List<Object[]> getDailyPayoutTrend(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Get pending payouts total.
     */
    @Query("SELECT COALESCE(SUM(rp.netPayout), 0) FROM RestaurantPayout rp " +
           "WHERE rp.status = :status")
    BigDecimal getTotalByStatus(@Param("status") PaymentStatus status);

    /**
     * Get average processing time for completed payouts.
     */
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (paid_at - created_at)) / 3600) " +
                   "FROM restaurant_payouts " +
                   "WHERE status = 'COMPLETED' " +
                   "AND period_start >= :startDate AND period_end <= :endDate", nativeQuery = true)
    BigDecimal getAverageProcessingTimeHours(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    /**
     * Get payouts for specific restaurants.
     */
    @Query("SELECT rp.restaurantId, SUM(rp.grossAmount), SUM(rp.netPayout), rp.status " +
           "FROM RestaurantPayout rp " +
           "WHERE rp.periodStart >= :startDate AND rp.periodEnd <= :endDate " +
           "AND rp.restaurantId IN :restaurantIds " +
           "GROUP BY rp.restaurantId, rp.status")
    List<Object[]> getPayoutsForRestaurants(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate,
                                            @Param("restaurantIds") List<Long> restaurantIds);

    List<RestaurantPayout> findByRestaurantIdAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
            Long restaurantId, LocalDateTime periodStart, LocalDateTime periodEnd);
}
