package com.fooddelivery.analytics.financial.repository;

import com.fooddelivery.analytics.financial.model.PaymentStatus;
import com.fooddelivery.analytics.financial.model.ReferralReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for referral reward data with optimized analytical queries.
 */
@Repository
public interface ReferralRewardRepository extends JpaRepository<ReferralReward, Long> {

    /**
     * Get total referral rewards paid for period.
     */
    @Query("SELECT COALESCE(SUM(rr.rewardAmount), 0) FROM ReferralReward rr " +
           "WHERE rr.status = 'COMPLETED' " +
           "AND rr.awardedAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRewardsPaid(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Get referral reward metrics.
     */
    @Query("SELECT COALESCE(SUM(rr.rewardAmount), 0), " +
           "COUNT(rr), " +
           "COUNT(DISTINCT rr.referrerId), " +
           "COUNT(DISTINCT rr.referredUserId), " +
           "COALESCE(AVG(rr.rewardAmount), 0) " +
           "FROM ReferralReward rr " +
           "WHERE rr.awardedAt BETWEEN :startDate AND :endDate")
    Object[] getReferralMetrics(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    /**
     * Get rewards by type (referrer vs referred).
     */
    @Query("SELECT rr.rewardType, SUM(rr.rewardAmount), COUNT(rr) " +
           "FROM ReferralReward rr " +
           "WHERE rr.awardedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY rr.rewardType")
    List<Object[]> getRewardsByType(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Get rewards by status.
     */
    @Query("SELECT rr.status, SUM(rr.rewardAmount), COUNT(rr) " +
           "FROM ReferralReward rr " +
           "WHERE rr.awardedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY rr.status")
    List<Object[]> getRewardsByStatus(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Get daily referral reward trend.
     */
    @Query(value = "SELECT DATE(awarded_at) as date, " +
                   "SUM(reward_amount), COUNT(*), COUNT(DISTINCT referrer_id) " +
                   "FROM referral_rewards " +
                   "WHERE awarded_at BETWEEN :startDate AND :endDate " +
                   "GROUP BY DATE(awarded_at) " +
                   "ORDER BY date", nativeQuery = true)
    List<Object[]> getDailyReferralTrend(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * Get top referrers by rewards earned.
     */
    @Query("SELECT rr.referrerId, SUM(rr.rewardAmount), COUNT(rr) " +
           "FROM ReferralReward rr " +
           "WHERE rr.rewardType = 'REFERRER' " +
           "AND rr.awardedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY rr.referrerId " +
           "ORDER BY SUM(rr.rewardAmount) DESC")
    List<Object[]> getTopReferrers(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Get pending rewards total.
     */
    @Query("SELECT COALESCE(SUM(rr.rewardAmount), 0) FROM ReferralReward rr " +
           "WHERE rr.status = :status")
    BigDecimal getTotalByStatus(@Param("status") PaymentStatus status);

    List<ReferralReward> findByReferrerIdAndAwardedAtBetween(
            Long referrerId, LocalDateTime startDate, LocalDateTime endDate);

    List<ReferralReward> findByReferredUserIdAndAwardedAtBetween(
            Long referredUserId, LocalDateTime startDate, LocalDateTime endDate);
}
