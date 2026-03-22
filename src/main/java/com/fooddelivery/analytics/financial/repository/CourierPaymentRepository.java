package com.fooddelivery.analytics.financial.repository;

import com.fooddelivery.analytics.financial.model.CourierPayment;
import com.fooddelivery.analytics.financial.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for courier payment data with optimized analytical queries.
 */
@Repository
public interface CourierPaymentRepository extends JpaRepository<CourierPayment, Long> {

    /**
     * Get total courier payments aggregated.
     */
    @Query("SELECT COALESCE(SUM(cp.totalPayment), 0), " +
           "COALESCE(SUM(cp.basePayment), 0), " +
           "COALESCE(SUM(cp.distanceBonus), 0), " +
           "COALESCE(SUM(cp.tipAmount), 0), " +
           "COALESCE(SUM(cp.peakBonus), 0), " +
           "COUNT(cp), " +
           "COUNT(DISTINCT cp.courierId) " +
           "FROM CourierPayment cp " +
           "WHERE cp.paymentDate BETWEEN :startDate AND :endDate")
    List<Object[]> getCourierPaymentMetrics(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Get payments by status.
     */
    @Query("SELECT cp.paymentStatus, SUM(cp.totalPayment), COUNT(cp) " +
           "FROM CourierPayment cp " +
           "WHERE cp.paymentDate BETWEEN :startDate AND :endDate " +
           "GROUP BY cp.paymentStatus")
    List<Object[]> getPaymentsByStatus(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Get payments by type.
     */
    @Query("SELECT cp.paymentType, SUM(cp.totalPayment), COUNT(cp) " +
           "FROM CourierPayment cp " +
           "WHERE cp.paymentDate BETWEEN :startDate AND :endDate " +
           "GROUP BY cp.paymentType")
    List<Object[]> getPaymentsByType(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Get top couriers by earnings.
     */
    @Query("SELECT cp.courierId, SUM(cp.totalPayment), SUM(cp.basePayment), " +
           "SUM(cp.distanceBonus + cp.peakBonus), SUM(cp.tipAmount), COUNT(cp) " +
           "FROM CourierPayment cp " +
           "WHERE cp.paymentDate BETWEEN :startDate AND :endDate " +
           "GROUP BY cp.courierId " +
           "ORDER BY SUM(cp.totalPayment) DESC")
    List<Object[]> getTopCouriersByEarnings(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Get daily courier payment trend.
     */
    @Query(value = "SELECT DATE(payment_date) as date, " +
                   "SUM(total_payment), SUM(base_payment), " +
                   "SUM(distance_bonus + peak_bonus), SUM(tip_amount), " +
                   "COUNT(DISTINCT courier_id), COUNT(*) " +
                   "FROM courier_payments " +
                   "WHERE payment_date BETWEEN :startDate AND :endDate " +
                   "GROUP BY DATE(payment_date) " +
                   "ORDER BY date", nativeQuery = true)
    List<Object[]> getDailyCourierPaymentTrend(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * Get tip statistics.
     */
    @Query("SELECT COUNT(cp), " +
           "SUM(CASE WHEN cp.tipAmount > 0 THEN 1 ELSE 0 END), " +
           "AVG(CASE WHEN cp.tipAmount > 0 THEN cp.tipAmount ELSE NULL END), " +
           "SUM(cp.tipAmount) " +
           "FROM CourierPayment cp " +
           "WHERE cp.paymentDate BETWEEN :startDate AND :endDate")
    Object[] getTipStatistics(@Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate);

    /**
     * Get pending payouts total.
     */
    @Query("SELECT COALESCE(SUM(cp.totalPayment), 0) FROM CourierPayment cp " +
           "WHERE cp.paymentStatus = :status " +
           "AND cp.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalByStatus(@Param("status") PaymentStatus status,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    /**
     * Get average distance bonus (proxy for delivery distance).
     */
    @Query("SELECT COALESCE(AVG(cp.distanceBonus), 0) FROM CourierPayment cp " +
           "WHERE cp.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal getAverageDistanceBonus(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    List<CourierPayment> findByCourierIdAndPaymentDateBetween(
            Long courierId, LocalDateTime startDate, LocalDateTime endDate);
}
