package com.fooddelivery.admin.dashboard.repository;

import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for dashboard order queries.
 * Provides optimized queries for order metrics and analytics.
 */
@Repository
public interface DashboardOrderRepository extends JpaRepository<Order, Long> {

    // ==================== Count Queries ====================

    /**
     * Count orders by status within date range.
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    Long countByStatusAndDateRange(@Param("status") OrderStatus status,
                                    @Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Count all orders within date range.
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    Long countByDateRange(@Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);

    /**
     * Count active orders (not in terminal states).
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN :statuses")
    Long countByStatusIn(@Param("statuses") List<OrderStatus> statuses);

    /**
     * Count orders by restaurant within date range.
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.restaurant.id = :restaurantId " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    Long countByRestaurantAndDateRange(@Param("restaurantId") Long restaurantId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Count orders by courier within date range.
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.courier.id = :courierId " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    Long countByCourierAndDateRange(@Param("courierId") Long courierId,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    // ==================== Revenue Queries ====================

    /**
     * Calculate total revenue (GMV) within date range.
     */
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o " +
            "WHERE o.status NOT IN ('CANCELLED', 'REFUNDED') " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumRevenueByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate total tips within date range.
     */
    @Query("SELECT COALESCE(SUM(o.tipAmount), 0) FROM Order o " +
            "WHERE o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumTipsByDateRange(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate total discounts within date range.
     */
    @Query("SELECT COALESCE(SUM(o.discount), 0) FROM Order o " +
            "WHERE o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumDiscountsByDateRange(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate average order value within date range.
     */
    @Query("SELECT COALESCE(AVG(o.total), 0) FROM Order o " +
            "WHERE o.status NOT IN ('CANCELLED', 'REFUNDED') " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal avgOrderValueByDateRange(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * Revenue by restaurant within date range.
     */
    @Query("SELECT o.restaurant.id, o.restaurant.name, COALESCE(SUM(o.total), 0), COUNT(o) " +
            "FROM Order o WHERE o.status NOT IN ('CANCELLED', 'REFUNDED') " +
            "AND o.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY o.restaurant.id, o.restaurant.name " +
            "ORDER BY SUM(o.total) DESC")
    List<Object[]> revenueByRestaurant(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        Pageable pageable);

    // ==================== Performance Queries ====================

    /**
     * Calculate average delivery time (in minutes) for delivered orders.
     */
    @Query("SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, o.createdAt, o.deliveredAt)), 0) " +
            "FROM Order o WHERE o.status = 'DELIVERED' " +
            "AND o.deliveredAt IS NOT NULL " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    Double avgDeliveryTimeByDateRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate average prep time (from accepted to ready).
     */
    @Query("SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, o.acceptedAt, o.readyAt)), 0) " +
            "FROM Order o WHERE o.readyAt IS NOT NULL AND o.acceptedAt IS NOT NULL " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    Double avgPrepTimeByDateRange(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate average acceptance time (from created to accepted).
     */
    @Query("SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, o.createdAt, o.acceptedAt)), 0) " +
            "FROM Order o WHERE o.acceptedAt IS NOT NULL " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    Double avgAcceptanceTimeByDateRange(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    // ==================== Active Orders Queries ====================

    /**
     * Find all active orders (not in terminal states).
     */
    @Query("SELECT o FROM Order o WHERE o.status IN :statuses ORDER BY o.createdAt DESC")
    Page<Order> findActiveOrders(@Param("statuses") List<OrderStatus> statuses, Pageable pageable);

    /**
     * Find active orders with filters.
     */
    @Query("SELECT o FROM Order o WHERE o.status IN :statuses " +
            "AND (:restaurantId IS NULL OR o.restaurant.id = :restaurantId) " +
            "AND (:courierId IS NULL OR o.courier.id = :courierId) " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findActiveOrdersWithFilters(@Param("statuses") List<OrderStatus> statuses,
                                             @Param("restaurantId") Long restaurantId,
                                             @Param("courierId") Long courierId,
                                             Pageable pageable);

    // ==================== Stuck Orders Queries ====================

    /**
     * Find orders stuck in CREATED status (pending acceptance).
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'CREATED' " +
            "AND TIMESTAMPDIFF(MINUTE, o.createdAt, :now) > :thresholdMinutes " +
            "ORDER BY o.createdAt ASC")
    List<Order> findStuckPendingOrders(@Param("now") LocalDateTime now,
                                        @Param("thresholdMinutes") int thresholdMinutes);

    /**
     * Find orders stuck in ACCEPTED status.
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'ACCEPTED' " +
            "AND TIMESTAMPDIFF(MINUTE, o.acceptedAt, :now) > :thresholdMinutes " +
            "ORDER BY o.acceptedAt ASC")
    List<Order> findStuckAcceptedOrders(@Param("now") LocalDateTime now,
                                         @Param("thresholdMinutes") int thresholdMinutes);

    /**
     * Find orders stuck in PREPARING status.
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PREPARING' " +
            "AND TIMESTAMPDIFF(MINUTE, o.preparingAt, :now) > :thresholdMinutes " +
            "ORDER BY o.preparingAt ASC")
    List<Order> findStuckPreparingOrders(@Param("now") LocalDateTime now,
                                          @Param("thresholdMinutes") int thresholdMinutes);

    /**
     * Find orders stuck in READY status (waiting for pickup).
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'READY' " +
            "AND TIMESTAMPDIFF(MINUTE, o.readyAt, :now) > :thresholdMinutes " +
            "ORDER BY o.readyAt ASC")
    List<Order> findStuckReadyOrders(@Param("now") LocalDateTime now,
                                      @Param("thresholdMinutes") int thresholdMinutes);

    /**
     * Find orders stuck in IN_TRANSIT status.
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'IN_TRANSIT' " +
            "AND TIMESTAMPDIFF(MINUTE, o.pickedUpAt, :now) > :thresholdMinutes " +
            "ORDER BY o.pickedUpAt ASC")
    List<Order> findStuckInTransitOrders(@Param("now") LocalDateTime now,
                                          @Param("thresholdMinutes") int thresholdMinutes);

    // ==================== Cancelled Orders Queries ====================

    /**
     * Find cancelled orders within date range.
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'CANCELLED' " +
            "AND o.cancelledAt BETWEEN :startDate AND :endDate " +
            "ORDER BY o.cancelledAt DESC")
    Page<Order> findCancelledOrders(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate,
                                     Pageable pageable);

    /**
     * Count cancelled orders by reason.
     */
    @Query("SELECT o.cancellationReason, COUNT(o) FROM Order o " +
            "WHERE o.status = 'CANCELLED' " +
            "AND o.cancelledAt BETWEEN :startDate AND :endDate " +
            "GROUP BY o.cancellationReason")
    List<Object[]> countCancelledByReason(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Sum value of cancelled orders.
     */
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o " +
            "WHERE o.status = 'CANCELLED' " +
            "AND o.cancelledAt BETWEEN :startDate AND :endDate")
    BigDecimal sumCancelledOrderValue(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    // ==================== Hourly Distribution Queries ====================

    /**
     * Order count by hour.
     */
    @Query("SELECT HOUR(o.createdAt), COUNT(o) FROM Order o " +
            "WHERE o.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY HOUR(o.createdAt) " +
            "ORDER BY HOUR(o.createdAt)")
    List<Object[]> orderCountByHour(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Cancelled orders by hour.
     */
    @Query("SELECT HOUR(o.cancelledAt), COUNT(o) FROM Order o " +
            "WHERE o.status = 'CANCELLED' " +
            "AND o.cancelledAt BETWEEN :startDate AND :endDate " +
            "GROUP BY HOUR(o.cancelledAt)")
    List<Object[]> cancelledOrdersByHour(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    // ==================== Restaurant-specific Queries ====================

    /**
     * Restaurant performance metrics.
     */
    @Query("SELECT o.restaurant.id, " +
            "COUNT(o), " +
            "SUM(CASE WHEN o.status = 'DELIVERED' OR o.status = 'COMPLETED' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN o.status = 'CANCELLED' THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(o.total), 0), " +
            "COALESCE(AVG(TIMESTAMPDIFF(MINUTE, o.acceptedAt, o.readyAt)), 0), " +
            "COALESCE(AVG(TIMESTAMPDIFF(SECOND, o.createdAt, o.acceptedAt)), 0) " +
            "FROM Order o WHERE o.restaurant.id = :restaurantId " +
            "AND o.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY o.restaurant.id")
    List<Object[]> getRestaurantMetrics(@Param("restaurantId") Long restaurantId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    // ==================== Courier-specific Queries ====================

    /**
     * Courier performance metrics.
     */
    @Query("SELECT o.courier.id, " +
            "COUNT(o), " +
            "SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN o.status = 'CANCELLED' AND o.courier IS NOT NULL THEN 1 ELSE 0 END), " +
            "COALESCE(AVG(TIMESTAMPDIFF(MINUTE, o.pickedUpAt, o.deliveredAt)), 0), " +
            "COALESCE(SUM(o.tipAmount), 0) " +
            "FROM Order o WHERE o.courier.id = :courierId " +
            "AND o.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY o.courier.id")
    List<Object[]> getCourierMetrics(@Param("courierId") Long courierId,
                                      @Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Count deliveries by courier today.
     */
    @Query("SELECT o.courier.id, COUNT(o) FROM Order o " +
            "WHERE o.status = 'DELIVERED' " +
            "AND o.deliveredAt BETWEEN :startDate AND :endDate " +
            "GROUP BY o.courier.id")
    List<Object[]> deliveriesPerCourier(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    // ==================== Finance Collector Methods ====================

    /**
     * Calculate GMV (Gross Merchandise Value) within date range.
     */
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o " +
            "WHERE o.status IN ('DELIVERED', 'COMPLETED') " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateGMV(@Param("startDate") LocalDateTime startDate,
                            @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate delivery fee revenue within date range.
     */
    @Query("SELECT COALESCE(SUM(o.deliveryFee), 0) FROM Order o " +
            "WHERE o.status IN ('DELIVERED', 'COMPLETED') " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateDeliveryFeeRevenue(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate courier payouts within date range.
     */
    @Query("SELECT COALESCE(SUM(o.courierEarnings), 0) FROM Order o " +
            "WHERE o.status = 'DELIVERED' " +
            "AND o.deliveredAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateCourierPayouts(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate total discounts used within date range.
     */
    @Query("SELECT COALESCE(SUM(o.discount), 0) FROM Order o " +
            "WHERE o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateDiscountsUsed(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate unsettled restaurant payouts.
     */
    @Query("SELECT COALESCE(SUM(o.restaurantEarnings), 0) FROM Order o " +
            "WHERE o.status = 'DELIVERED' AND o.restaurantPaid = false " +
            "AND o.deliveredAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateUnsettledRestaurantPayouts(@Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate unsettled courier payouts.
     */
    @Query("SELECT COALESCE(SUM(o.courierEarnings), 0) FROM Order o " +
            "WHERE o.status = 'DELIVERED' AND o.courierPaid = false " +
            "AND o.deliveredAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateUnsettledCourierPayouts(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Count orders within date range (alias for countByDateRange).
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    Long countOrdersToday(@Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);

    /**
     * Count pending restaurant payouts.
     */
    @Query("SELECT COUNT(DISTINCT o.restaurant.id) FROM Order o " +
            "WHERE o.status = 'DELIVERED' AND o.restaurantPaid = false " +
            "AND o.deliveredAt BETWEEN :startDate AND :endDate")
    Long countPendingRestaurantPayouts(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Count pending courier payouts.
     */
    @Query("SELECT COUNT(DISTINCT o.courier.id) FROM Order o " +
            "WHERE o.status = 'DELIVERED' AND o.courierPaid = false " +
            "AND o.deliveredAt BETWEEN :startDate AND :endDate")
    Long countPendingCourierPayouts(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Count completed payouts.
     */
    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.status = 'DELIVERED' AND o.restaurantPaid = true AND o.courierPaid = true " +
            "AND o.deliveredAt BETWEEN :startDate AND :endDate")
    Long countCompletedPayouts(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    /**
     * Average payout settlement time in minutes.
     */
    @Query("SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, o.deliveredAt, o.payoutProcessedAt)), 0) FROM Order o " +
            "WHERE o.status = 'DELIVERED' AND o.payoutProcessedAt IS NOT NULL " +
            "AND o.deliveredAt BETWEEN :startDate AND :endDate")
    Double avgPayoutSettlementTimeMinutes(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Count orders with discount.
     */
    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.discount IS NOT NULL AND o.discount > 0 " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    Long countOrdersWithDiscount(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Get discounts breakdown by type.
     */
    @Query("SELECT o.discountType, COALESCE(SUM(o.discount), 0) FROM Order o " +
            "WHERE o.discount IS NOT NULL AND o.discount > 0 " +
            "AND o.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY o.discountType")
    List<Object[]> getDiscountsByType(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Get top promo codes by usage.
     */
    @Query(value = "SELECT promo_code, COUNT(*), COALESCE(SUM(discount), 0) FROM orders " +
            "WHERE promo_code IS NOT NULL " +
            "AND created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY promo_code " +
            "ORDER BY COUNT(*) DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> getTopPromoCodes(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate,
                                     @Param("limit") int limit);

    /**
     * Get daily revenue breakdown.
     */
    @Query(value = "SELECT DATE(created_at), COALESCE(SUM(total), 0), COUNT(*), " +
            "COALESCE(SUM(delivery_fee), 0), COALESCE(SUM(discount), 0) FROM orders " +
            "WHERE status IN ('DELIVERED', 'COMPLETED') " +
            "AND created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY DATE(created_at)", nativeQuery = true)
    List<Object[]> getDailyRevenue(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Get revenue breakdown by payment method.
     */
    @Query("SELECT o.paymentMethod, COALESCE(SUM(o.total), 0) FROM Order o " +
            "WHERE o.status IN ('DELIVERED', 'COMPLETED') " +
            "AND o.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY o.paymentMethod")
    List<Object[]> getRevenueByPaymentMethod(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * Count rejected orders within date range.
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'REJECTED' " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    Long countRejectedOrders(@Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate);
}
