package com.fooddelivery.analytics.fraud.repository;

import com.fooddelivery.analytics.fraud.model.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for payment attempt fraud detection queries.
 */
@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    // ==================== Count Queries ====================

    @Query("SELECT COUNT(p) FROM PaymentAttempt p WHERE p.createdAt BETWEEN :start AND :end")
    Long countTotalAttempts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(p) FROM PaymentAttempt p WHERE p.status = 'SUCCESS' AND p.createdAt BETWEEN :start AND :end")
    Long countSuccessfulPayments(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(p) FROM PaymentAttempt p WHERE p.status = 'FAILED' AND p.createdAt BETWEEN :start AND :end")
    Long countFailedPayments(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(p) FROM PaymentAttempt p WHERE p.status = 'DECLINED' AND p.createdAt BETWEEN :start AND :end")
    Long countDeclinedPayments(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(p) FROM PaymentAttempt p WHERE p.status = 'TIMEOUT' AND p.createdAt BETWEEN :start AND :end")
    Long countTimeoutPayments(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== User-Level Failed Payment Queries ====================

    /**
     * Get users with failed payment counts exceeding threshold.
     * Returns: userId, failedCount, totalCount
     */
    @Query("SELECT p.userId, " +
            "SUM(CASE WHEN p.status IN ('FAILED', 'DECLINED') THEN 1 ELSE 0 END), " +
            "COUNT(p) " +
            "FROM PaymentAttempt p " +
            "WHERE p.createdAt BETWEEN :start AND :end " +
            "GROUP BY p.userId " +
            "HAVING SUM(CASE WHEN p.status IN ('FAILED', 'DECLINED') THEN 1 ELSE 0 END) >= :threshold " +
            "ORDER BY SUM(CASE WHEN p.status IN ('FAILED', 'DECLINED') THEN 1 ELSE 0 END) DESC")
    List<Object[]> getUsersWithHighFailedPayments(@Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end,
                                                   @Param("threshold") int threshold);

    /**
     * Count unique users with at least one failed payment.
     */
    @Query("SELECT COUNT(DISTINCT p.userId) FROM PaymentAttempt p " +
            "WHERE p.status IN ('FAILED', 'DECLINED') AND p.createdAt BETWEEN :start AND :end")
    Long countUsersWithFailedPayments(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get failed payment details for a specific user.
     * Returns: failureReason, count
     */
    @Query("SELECT p.failureReason, COUNT(p) FROM PaymentAttempt p " +
            "WHERE p.userId = :userId AND p.status IN ('FAILED', 'DECLINED') " +
            "AND p.createdAt BETWEEN :start AND :end " +
            "GROUP BY p.failureReason")
    List<Object[]> getFailureReasonsByUser(@Param("userId") Long userId,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    // ==================== Failure Breakdown Queries ====================

    /**
     * Get failure counts by reason.
     */
    @Query("SELECT p.failureReason, COUNT(p) FROM PaymentAttempt p " +
            "WHERE p.status IN ('FAILED', 'DECLINED') AND p.createdAt BETWEEN :start AND :end " +
            "GROUP BY p.failureReason ORDER BY COUNT(p) DESC")
    List<Object[]> countFailuresByReason(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get failure counts by payment method.
     */
    @Query("SELECT p.paymentMethod, COUNT(p) FROM PaymentAttempt p " +
            "WHERE p.status IN ('FAILED', 'DECLINED') AND p.createdAt BETWEEN :start AND :end " +
            "GROUP BY p.paymentMethod ORDER BY COUNT(p) DESC")
    List<Object[]> countFailuresByMethod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get failure rate by payment method.
     * Returns: paymentMethod, failedCount, totalCount
     */
    @Query("SELECT p.paymentMethod, " +
            "SUM(CASE WHEN p.status IN ('FAILED', 'DECLINED') THEN 1 ELSE 0 END), " +
            "COUNT(p) " +
            "FROM PaymentAttempt p WHERE p.createdAt BETWEEN :start AND :end " +
            "GROUP BY p.paymentMethod")
    List<Object[]> getFailureRateByMethod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get failure counts by card BIN.
     */
    @Query("SELECT p.cardBin, COUNT(p) FROM PaymentAttempt p " +
            "WHERE p.cardBin IS NOT NULL AND p.status IN ('FAILED', 'DECLINED') " +
            "AND p.createdAt BETWEEN :start AND :end " +
            "GROUP BY p.cardBin ORDER BY COUNT(p) DESC")
    List<Object[]> countFailuresByCardBin(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Amount Queries ====================

    /**
     * Get total failed payment amount.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentAttempt p " +
            "WHERE p.status IN ('FAILED', 'DECLINED') AND p.createdAt BETWEEN :start AND :end")
    java.math.BigDecimal getTotalFailedAmount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get average and max failed payment amount.
     * Returns: avg, max
     */
    @Query("SELECT AVG(p.amount), MAX(p.amount) FROM PaymentAttempt p " +
            "WHERE p.status IN ('FAILED', 'DECLINED') AND p.createdAt BETWEEN :start AND :end")
    Object[] getFailedAmountStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== Spike Detection Queries ====================

    /**
     * Get hourly failure counts for spike detection.
     * Returns: hour, failedCount, totalCount
     */
    @Query("SELECT EXTRACT(HOUR FROM p.createdAt), " +
            "SUM(CASE WHEN p.status IN ('FAILED', 'DECLINED') THEN 1 ELSE 0 END), " +
            "COUNT(p) " +
            "FROM PaymentAttempt p WHERE p.createdAt BETWEEN :start AND :end " +
            "GROUP BY EXTRACT(HOUR FROM p.createdAt) " +
            "ORDER BY EXTRACT(HOUR FROM p.createdAt)")
    List<Object[]> getHourlyFailureCounts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get failure count in short time window for velocity check.
     */
    @Query("SELECT COUNT(p) FROM PaymentAttempt p " +
            "WHERE p.userId = :userId AND p.status IN ('FAILED', 'DECLINED') " +
            "AND p.createdAt BETWEEN :windowStart AND :windowEnd")
    Long countUserFailuresInWindow(@Param("userId") Long userId,
                                   @Param("windowStart") LocalDateTime windowStart,
                                   @Param("windowEnd") LocalDateTime windowEnd);

    // ==================== Card/Token Queries ====================

    /**
     * Count unique cards used.
     */
    @Query("SELECT COUNT(DISTINCT p.cardLastFour) FROM PaymentAttempt p " +
            "WHERE p.cardLastFour IS NOT NULL AND p.createdAt BETWEEN :start AND :end")
    Long countUniqueCards(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get cards with multiple failures.
     * Returns: cardLastFour, cardBin, failedCount, userCount
     */
    @Query("SELECT p.cardLastFour, p.cardBin, COUNT(p), COUNT(DISTINCT p.userId) " +
            "FROM PaymentAttempt p " +
            "WHERE p.cardLastFour IS NOT NULL AND p.status IN ('FAILED', 'DECLINED') " +
            "AND p.createdAt BETWEEN :start AND :end " +
            "GROUP BY p.cardLastFour, p.cardBin " +
            "HAVING COUNT(p) >= :threshold " +
            "ORDER BY COUNT(p) DESC")
    List<Object[]> getCardsWithMultipleFailures(@Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end,
                                                 @Param("threshold") int threshold);

    /**
     * Detect shared payment tokens across users.
     * Returns: tokenHash, userCount, transactionCount
     */
    @Query("SELECT p.paymentTokenHash, COUNT(DISTINCT p.userId), COUNT(p) " +
            "FROM PaymentAttempt p " +
            "WHERE p.paymentTokenHash IS NOT NULL AND p.createdAt BETWEEN :start AND :end " +
            "GROUP BY p.paymentTokenHash " +
            "HAVING COUNT(DISTINCT p.userId) > 1 " +
            "ORDER BY COUNT(DISTINCT p.userId) DESC")
    List<Object[]> detectSharedPaymentTokens(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== User-Specific Queries ====================

    @Query("SELECT COUNT(p) FROM PaymentAttempt p WHERE p.userId = :userId " +
            "AND p.createdAt BETWEEN :start AND :end")
    Long countByUserIdAndCreatedAtBetween(@Param("userId") Long userId,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(p) FROM PaymentAttempt p WHERE p.userId = :userId " +
            "AND p.status IN ('FAILED', 'DECLINED') AND p.createdAt BETWEEN :start AND :end")
    Long countUserFailedPayments(@Param("userId") Long userId,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentAttempt p WHERE p.userId = :userId " +
            "AND p.status IN ('FAILED', 'DECLINED') AND p.createdAt BETWEEN :start AND :end")
    java.math.BigDecimal getUserTotalFailedAmount(@Param("userId") Long userId,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);
}
