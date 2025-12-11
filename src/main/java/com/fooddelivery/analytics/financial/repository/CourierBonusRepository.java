package com.fooddelivery.analytics.financial.repository;

import com.fooddelivery.analytics.financial.model.BonusType;
import com.fooddelivery.analytics.financial.model.CourierBonus;
import com.fooddelivery.analytics.financial.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for courier bonus data with optimized analytical queries.
 */
@Repository
public interface CourierBonusRepository extends JpaRepository<CourierBonus, Long> {

    /**
     * Get total bonuses by type.
     */
    @Query("SELECT cb.bonusType, SUM(cb.bonusAmount), COUNT(DISTINCT cb.courierId), AVG(cb.bonusAmount) " +
           "FROM CourierBonus cb " +
           "WHERE cb.earnedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY cb.bonusType")
    List<Object[]> getBonusesByType(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Get total bonus amount for period.
     */
    @Query("SELECT COALESCE(SUM(cb.bonusAmount), 0) FROM CourierBonus cb " +
           "WHERE cb.earnedAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalBonuses(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    /**
     * Get bonus metrics.
     */
    @Query("SELECT COALESCE(SUM(cb.bonusAmount), 0), COUNT(cb), COUNT(DISTINCT cb.courierId), " +
           "COALESCE(AVG(cb.bonusAmount), 0) " +
           "FROM CourierBonus cb " +
           "WHERE cb.earnedAt BETWEEN :startDate AND :endDate")
    Object[] getBonusMetrics(@Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate);

    /**
     * Get bonuses by status.
     */
    @Query("SELECT cb.status, SUM(cb.bonusAmount), COUNT(cb) " +
           "FROM CourierBonus cb " +
           "WHERE cb.earnedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY cb.status")
    List<Object[]> getBonusesByStatus(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Get daily bonus trend.
     */
    @Query(value = "SELECT DATE(earned_at) as date, SUM(bonus_amount), COUNT(*), " +
                   "COUNT(DISTINCT courier_id) " +
                   "FROM courier_bonuses " +
                   "WHERE earned_at BETWEEN :startDate AND :endDate " +
                   "GROUP BY DATE(earned_at) " +
                   "ORDER BY date", nativeQuery = true)
    List<Object[]> getDailyBonusTrend(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Get top couriers by bonus earned.
     */
    @Query("SELECT cb.courierId, SUM(cb.bonusAmount), COUNT(cb) " +
           "FROM CourierBonus cb " +
           "WHERE cb.earnedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY cb.courierId " +
           "ORDER BY SUM(cb.bonusAmount) DESC")
    List<Object[]> getTopCouriersByBonus(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * Get pending bonus amount.
     */
    @Query("SELECT COALESCE(SUM(cb.bonusAmount), 0) FROM CourierBonus cb " +
           "WHERE cb.status = :status " +
           "AND cb.earnedAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalByStatus(@Param("status") PaymentStatus status,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    List<CourierBonus> findByCourierIdAndEarnedAtBetween(
            Long courierId, LocalDateTime startDate, LocalDateTime endDate);
}
