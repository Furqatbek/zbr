package com.fooddelivery.analytics.financial.repository;

import com.fooddelivery.analytics.financial.model.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for promotion usage data with optimized analytical queries.
 */
@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long> {

    /**
     * Get promotion metrics aggregated.
     */
    @Query("SELECT COALESCE(SUM(pu.discountAmount), 0), " +
           "COALESCE(SUM(pu.platformCost), 0), " +
           "COALESCE(SUM(pu.restaurantCost), 0), " +
           "COUNT(pu), " +
           "COALESCE(AVG(pu.discountAmount), 0) " +
           "FROM PromotionUsage pu " +
           "WHERE pu.usedAt BETWEEN :startDate AND :endDate")
    Object[] getPromotionMetrics(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Get promotion breakdown by type.
     */
    @Query("SELECT pu.promotionType, SUM(pu.discountAmount), COUNT(pu), AVG(pu.discountAmount) " +
           "FROM PromotionUsage pu " +
           "WHERE pu.usedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY pu.promotionType")
    List<Object[]> getPromotionByType(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Get promotion breakdown by funder.
     */
    @Query("SELECT pu.fundedBy, " +
           "SUM(CASE WHEN pu.fundedBy = 'PLATFORM' THEN pu.discountAmount " +
           "         WHEN pu.fundedBy = 'SHARED' THEN pu.platformCost " +
           "         ELSE 0 END), " +
           "COUNT(pu) " +
           "FROM PromotionUsage pu " +
           "WHERE pu.usedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY pu.fundedBy")
    List<Object[]> getPromotionByFunder(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Get top promotions by usage.
     */
    @Query("SELECT pu.promotionId, pu.promoCode, pu.promotionType, " +
           "SUM(pu.discountAmount), COUNT(pu), SUM(pu.platformCost), SUM(pu.restaurantCost) " +
           "FROM PromotionUsage pu " +
           "WHERE pu.usedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY pu.promotionId, pu.promoCode, pu.promotionType " +
           "ORDER BY COUNT(pu) DESC")
    List<Object[]> getTopPromotionsByUsage(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Get daily promotion trend.
     */
    @Query(value = "SELECT DATE(used_at) as date, " +
                   "SUM(discount_amount), SUM(platform_cost), SUM(restaurant_cost), COUNT(*) " +
                   "FROM promotion_usages " +
                   "WHERE used_at BETWEEN :startDate AND :endDate " +
                   "GROUP BY DATE(used_at) " +
                   "ORDER BY date", nativeQuery = true)
    List<Object[]> getDailyPromotionTrend(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Get total platform cost for promotions.
     */
    @Query("SELECT COALESCE(SUM(pu.platformCost), 0) FROM PromotionUsage pu " +
           "WHERE pu.usedAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalPlatformCost(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Get promotion usage rate (requires total order count from external source).
     */
    @Query("SELECT COUNT(DISTINCT pu.orderId) FROM PromotionUsage pu " +
           "WHERE pu.usedAt BETWEEN :startDate AND :endDate")
    Long getPromotedOrderCount(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    /**
     * Get promotion usage grouped by restaurant cost.
     */
    @Query("SELECT pu.promotionType, SUM(pu.restaurantCost), COUNT(pu) " +
           "FROM PromotionUsage pu " +
           "WHERE pu.usedAt BETWEEN :startDate AND :endDate " +
           "AND pu.restaurantCost > 0 " +
           "GROUP BY pu.promotionType")
    List<Object[]> getPromotionByRestaurantCost(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    List<PromotionUsage> findByOrderIdAndUsedAtBetween(
            Long orderId, LocalDateTime startDate, LocalDateTime endDate);
}
