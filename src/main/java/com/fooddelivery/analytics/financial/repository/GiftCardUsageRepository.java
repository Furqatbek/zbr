package com.fooddelivery.analytics.financial.repository;

import com.fooddelivery.analytics.financial.model.GiftCardUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for gift card usage data with optimized analytical queries.
 */
@Repository
public interface GiftCardUsageRepository extends JpaRepository<GiftCardUsage, Long> {

    /**
     * Get total gift card redemptions for period.
     */
    @Query("SELECT COALESCE(SUM(gcu.redeemedAmount), 0) FROM GiftCardUsage gcu " +
           "WHERE gcu.usedAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRedemptions(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Get gift card usage metrics.
     */
    @Query("SELECT COALESCE(SUM(gcu.redeemedAmount), 0), " +
           "COUNT(gcu), " +
           "COUNT(DISTINCT gcu.giftCardId), " +
           "COUNT(DISTINCT gcu.userId), " +
           "COALESCE(AVG(gcu.redeemedAmount), 0) " +
           "FROM GiftCardUsage gcu " +
           "WHERE gcu.usedAt BETWEEN :startDate AND :endDate")
    Object[] getGiftCardMetrics(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    /**
     * Get daily gift card usage trend.
     */
    @Query(value = "SELECT DATE(business_ts(used_at)) as date, " +
                   "SUM(redeemed_amount), COUNT(*), COUNT(DISTINCT gift_card_id) " +
                   "FROM gift_card_usages " +
                   "WHERE used_at BETWEEN :startDate AND :endDate " +
                   "GROUP BY DATE(business_ts(used_at)) " +
                   "ORDER BY date", nativeQuery = true)
    List<Object[]> getDailyGiftCardTrend(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * Get top gift cards by usage.
     */
    @Query("SELECT gcu.giftCardId, gcu.giftCardCode, SUM(gcu.redeemedAmount), COUNT(gcu) " +
           "FROM GiftCardUsage gcu " +
           "WHERE gcu.usedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY gcu.giftCardId, gcu.giftCardCode " +
           "ORDER BY SUM(gcu.redeemedAmount) DESC")
    List<Object[]> getTopGiftCardsByUsage(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Get usage by user.
     */
    @Query("SELECT gcu.userId, SUM(gcu.redeemedAmount), COUNT(gcu) " +
           "FROM GiftCardUsage gcu " +
           "WHERE gcu.usedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY gcu.userId " +
           "ORDER BY SUM(gcu.redeemedAmount) DESC")
    List<Object[]> getUsageByUser(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    List<GiftCardUsage> findByUserIdAndUsedAtBetween(
            Long userId, LocalDateTime startDate, LocalDateTime endDate);

    List<GiftCardUsage> findByGiftCardIdAndUsedAtBetween(
            Long giftCardId, LocalDateTime startDate, LocalDateTime endDate);
}
