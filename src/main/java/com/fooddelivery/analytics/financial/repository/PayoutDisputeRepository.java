package com.fooddelivery.analytics.financial.repository;

import com.fooddelivery.analytics.financial.model.DisputeStatus;
import com.fooddelivery.analytics.financial.model.PayoutDispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for payout dispute data with optimized analytical queries.
 */
@Repository
public interface PayoutDisputeRepository extends JpaRepository<PayoutDispute, Long> {

    /**
     * Get dispute summary by status.
     */
    @Query("SELECT pd.status, COUNT(pd), SUM(pd.disputedAmount), " +
           "AVG(CASE WHEN pd.resolvedAt IS NOT NULL THEN " +
           "    FUNCTION('TIMESTAMPDIFF', DAY, pd.raisedAt, pd.resolvedAt) ELSE NULL END) " +
           "FROM PayoutDispute pd " +
           "WHERE pd.raisedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY pd.status")
    List<Object[]> getDisputeSummaryByStatus(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    /**
     * Get dispute summary by type.
     */
    @Query("SELECT pd.disputeType, COUNT(pd), SUM(pd.disputedAmount) " +
           "FROM PayoutDispute pd " +
           "WHERE pd.raisedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY pd.disputeType")
    List<Object[]> getDisputeSummaryByType(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Get total pending dispute amount.
     */
    @Query("SELECT COALESCE(SUM(pd.disputedAmount), 0) FROM PayoutDispute pd " +
           "WHERE pd.status IN ('OPEN', 'INVESTIGATING', 'ESCALATED')")
    BigDecimal getTotalPendingDisputeAmount();

    /**
     * Get dispute count for period.
     */
    @Query("SELECT COUNT(pd) FROM PayoutDispute pd " +
           "WHERE pd.raisedAt BETWEEN :startDate AND :endDate")
    Long getDisputeCount(@Param("startDate") LocalDateTime startDate,
                         @Param("endDate") LocalDateTime endDate);

    /**
     * Get disputes by entity type.
     */
    @Query("SELECT pd.entityType, COUNT(pd), SUM(pd.disputedAmount) " +
           "FROM PayoutDispute pd " +
           "WHERE pd.raisedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY pd.entityType")
    List<Object[]> getDisputesByEntityType(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Get daily dispute trend.
     */
    @Query(value = "SELECT DATE(raised_at) as date, COUNT(*), SUM(disputed_amount) " +
                   "FROM payout_disputes " +
                   "WHERE raised_at BETWEEN :startDate AND :endDate " +
                   "GROUP BY DATE(raised_at) " +
                   "ORDER BY date", nativeQuery = true)
    List<Object[]> getDailyDisputeTrend(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Get resolution statistics.
     */
    @Query("SELECT pd.status, COUNT(pd), SUM(pd.resolvedAmount) " +
           "FROM PayoutDispute pd " +
           "WHERE pd.resolvedAt BETWEEN :startDate AND :endDate " +
           "AND pd.status IN ('RESOLVED_FOR_CLAIMANT', 'RESOLVED_FOR_PLATFORM', 'PARTIALLY_RESOLVED') " +
           "GROUP BY pd.status")
    List<Object[]> getResolutionStatistics(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    List<PayoutDispute> findByEntityIdAndEntityTypeAndRaisedAtBetween(
            Long entityId, String entityType, LocalDateTime startDate, LocalDateTime endDate);

    List<PayoutDispute> findByStatusIn(List<DisputeStatus> statuses);
}
