package com.fooddelivery.order.repository;

import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Order entity operations.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByExternalOrderNo(String externalOrderNo);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.consumer LEFT JOIN FETCH o.restaurant LEFT JOIN FETCH o.courier WHERE o.id = :id")
    Optional<Order> findByIdWithLock(@Param("id") Long id);

    /**
     * Find order by ID with pessimistic write lock for courier assignment.
     * Prevents race condition where multiple couriers try to accept the same order.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.consumer LEFT JOIN FETCH o.restaurant LEFT JOIN FETCH o.courier WHERE o.id = :id")
    Optional<Order> findByIdForCourierAssignment(@Param("id") Long id);

    @Query(value = "SELECT o FROM Order o LEFT JOIN FETCH o.consumer LEFT JOIN FETCH o.restaurant LEFT JOIN FETCH o.courier WHERE o.consumer.id = :consumerId",
           countQuery = "SELECT COUNT(o) FROM Order o WHERE o.consumer.id = :consumerId")
    Page<Order> findByConsumerId(@Param("consumerId") Long consumerId, Pageable pageable);

    Page<Order> findByRestaurantId(Long restaurantId, Pageable pageable);

    Page<Order> findByCourierId(Long courierId, Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.courier.id = :courierId")
    long countByCourierId(@Param("courierId") Long courierId);

    @Query("SELECT o FROM Order o WHERE o.consumer.id = :consumerId AND o.status = :status")
    Page<Order> findByConsumerIdAndStatus(
            @Param("consumerId") Long consumerId,
            @Param("status") OrderStatus status,
            Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.restaurant.id = :restaurantId AND o.status = :status")
    Page<Order> findByRestaurantIdAndStatus(
            @Param("restaurantId") Long restaurantId,
            @Param("status") OrderStatus status,
            Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.restaurant.id = :restaurantId AND o.status IN :statuses ORDER BY o.createdAt DESC")
    List<Order> findActiveOrdersByRestaurant(
            @Param("restaurantId") Long restaurantId,
            @Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.consumer LEFT JOIN FETCH o.restaurant LEFT JOIN FETCH o.courier WHERE o.courier.id = :courierId AND o.status IN :statuses")
    List<Order> findActiveOrdersByCourier(
            @Param("courierId") Long courierId,
            @Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.paymentStatus = :paymentStatus " +
            "AND o.createdAt < :before")
    List<Order> findUnpaidOrdersOlderThan(
            @Param("status") OrderStatus status,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("before") LocalDateTime before);

    @Query("SELECT o FROM Order o WHERE o.status = com.fooddelivery.order.entity.OrderStatus.READY " +
            "AND o.orderType = com.fooddelivery.order.entity.OrderType.DELIVERY AND o.courier IS NULL")
    List<Order> findOrdersAwaitingCourierAssignment();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.restaurant.id = :restaurantId AND o.createdAt >= :since")
    long countOrdersByRestaurantSince(
            @Param("restaurantId") Long restaurantId,
            @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.createdAt >= :since")
    long countByStatusSince(
            @Param("status") OrderStatus status,
            @Param("since") LocalDateTime since);

    @Query("SELECT SUM(o.total) FROM Order o WHERE o.status = com.fooddelivery.order.entity.OrderStatus.DELIVERED " +
            "AND o.createdAt >= :since")
    java.math.BigDecimal sumCompletedOrdersGMVSince(@Param("since") LocalDateTime since);

    @Query("SELECT AVG(o.total) FROM Order o WHERE o.status = com.fooddelivery.order.entity.OrderStatus.DELIVERED " +
            "AND o.createdAt >= :since")
    java.math.BigDecimal avgOrderValueSince(@Param("since") LocalDateTime since);

    @Modifying
    @Query("UPDATE Order o SET o.status = :status, o.cancelledAt = :now, o.cancellationReason = :reason WHERE o.id = :id")
    int cancelOrder(
            @Param("id") Long id,
            @Param("status") OrderStatus status,
            @Param("now") LocalDateTime now,
            @Param("reason") String reason);

    // ===== Courier-specific queries =====

    /**
     * Get completed orders for a courier within a date range (for earnings).
     */
    @Query("SELECT o FROM Order o WHERE o.courier.id = :courierId " +
            "AND o.status = com.fooddelivery.order.entity.OrderStatus.DELIVERED " +
            "AND o.deliveredAt >= :since ORDER BY o.deliveredAt DESC")
    List<Order> findDeliveredOrdersByCourierSince(
            @Param("courierId") Long courierId,
            @Param("since") LocalDateTime since);

    /**
     * Count deliveries by courier since a date.
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.courier.id = :courierId " +
            "AND o.status = com.fooddelivery.order.entity.OrderStatus.DELIVERED " +
            "AND o.deliveredAt >= :since")
    long countDeliveriesByCourierSince(
            @Param("courierId") Long courierId,
            @Param("since") LocalDateTime since);

    /**
     * Sum delivery fees and tips for a courier since a date.
     */
    @Query("SELECT COALESCE(SUM(o.deliveryFee), 0) + COALESCE(SUM(o.tipAmount), 0) FROM Order o " +
            "WHERE o.courier.id = :courierId " +
            "AND o.status = com.fooddelivery.order.entity.OrderStatus.DELIVERED " +
            "AND o.deliveredAt >= :since")
    java.math.BigDecimal sumCourierEarningsSince(
            @Param("courierId") Long courierId,
            @Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(o.deliveryFee), 0) + COALESCE(SUM(o.tipAmount), 0) FROM Order o " +
            "JOIN com.fooddelivery.order.entity.Payment p ON p.order.id = o.id " +
            "WHERE o.courier.id = :courierId " +
            "AND o.status = com.fooddelivery.order.entity.OrderStatus.DELIVERED " +
            "AND o.deliveredAt >= :since " +
            "AND LOWER(p.paymentMethod) = :paymentMethod")
    java.math.BigDecimal sumCourierEarningsByPaymentMethodSince(
            @Param("courierId") Long courierId,
            @Param("since") LocalDateTime since,
            @Param("paymentMethod") String paymentMethod);

    /**
     * Get order history for a courier (completed/cancelled orders).
     */
    @Query("SELECT o FROM Order o WHERE o.courier.id = :courierId " +
            "AND o.status IN (com.fooddelivery.order.entity.OrderStatus.DELIVERED, " +
            "com.fooddelivery.order.entity.OrderStatus.CANCELLED) " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findOrderHistoryByCourier(@Param("courierId") Long courierId, Pageable pageable);

    /**
     * Get available orders for pickup (near courier location).
     */
    @Query("SELECT o FROM Order o JOIN FETCH o.restaurant " +
            "WHERE o.status IN (com.fooddelivery.order.entity.OrderStatus.ACCEPTED, " +
            "com.fooddelivery.order.entity.OrderStatus.PREPARING, " +
            "com.fooddelivery.order.entity.OrderStatus.READY) " +
            "AND o.orderType = com.fooddelivery.order.entity.OrderType.DELIVERY " +
            "AND o.courier IS NULL " +
            "ORDER BY o.createdAt ASC")
    List<Order> findAvailableOrdersForCourier();

    /**
     * Get problematic orders (cancelled, refunded).
     */
    @Query("SELECT o FROM Order o WHERE o.status IN :statuses ORDER BY o.updatedAt DESC")
    Page<Order> findByStatusIn(@Param("statuses") List<OrderStatus> statuses, Pageable pageable);

    /**
     * Find orders that have open disputes.
     */
    @Query("SELECT DISTINCT o FROM Order o WHERE o.id IN " +
           "(SELECT pd.orderId FROM com.fooddelivery.analytics.financial.model.PayoutDispute pd " +
           "WHERE pd.orderId IS NOT NULL AND pd.status IN (com.fooddelivery.analytics.financial.model.DisputeStatus.OPEN, com.fooddelivery.analytics.financial.model.DisputeStatus.INVESTIGATING, com.fooddelivery.analytics.financial.model.DisputeStatus.ESCALATED, com.fooddelivery.analytics.financial.model.DisputeStatus.ACCIDENT)) " +
           "ORDER BY o.updatedAt DESC")
    Page<Order> findOrdersWithOpenDisputes(Pageable pageable);

    /**
     * Find orders that have unresolved delivery issues.
     */
    @Query("SELECT DISTINCT o FROM Order o WHERE o.id IN " +
           "(SELECT di.order.id FROM com.fooddelivery.order.entity.DeliveryIssue di WHERE di.resolved = false) " +
           "ORDER BY o.updatedAt DESC")
    Page<Order> findOrdersWithUnresolvedDeliveryIssues(Pageable pageable);

    /**
     * Find orders that are cancelled/refunded, have open disputes, or have unresolved delivery issues.
     */
    @Query("SELECT DISTINCT o FROM Order o WHERE o.status IN :statuses " +
           "OR o.id IN (SELECT pd.orderId FROM com.fooddelivery.analytics.financial.model.PayoutDispute pd " +
           "WHERE pd.orderId IS NOT NULL AND pd.status IN (com.fooddelivery.analytics.financial.model.DisputeStatus.OPEN, com.fooddelivery.analytics.financial.model.DisputeStatus.INVESTIGATING, com.fooddelivery.analytics.financial.model.DisputeStatus.ESCALATED, com.fooddelivery.analytics.financial.model.DisputeStatus.ACCIDENT)) " +
           "OR o.id IN (SELECT di.order.id FROM com.fooddelivery.order.entity.DeliveryIssue di WHERE di.resolved = false) " +
           "ORDER BY o.updatedAt DESC")
    Page<Order> findProblematicOrders(@Param("statuses") List<OrderStatus> statuses, Pageable pageable);

    /**
     * Find orders by status (list version).
     */
    @Query("SELECT o FROM Order o JOIN FETCH o.courier WHERE o.status IN :statuses")
    List<Order> findByStatusIn(@Param("statuses") List<OrderStatus> statuses);

    /**
     * Find orders with inactive couriers (courier hasn't updated location recently).
     */
    @Query("SELECT o FROM Order o JOIN FETCH o.courier c " +
           "WHERE o.status IN :statuses " +
           "AND c.locationUpdatedAt < :threshold")
    List<Order> findOrdersWithInactiveCouriers(
            @Param("statuses") List<OrderStatus> statuses,
            @Param("threshold") LocalDateTime threshold);

    /**
     * Find orders stuck in a status since before a threshold time.
     */
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.readyAt < :threshold")
    List<Order> findByStatusAndReadyAtBefore(
            @Param("status") OrderStatus status,
            @Param("threshold") LocalDateTime threshold);

    /**
     * Delivered orders whose deliveredAt is older than the threshold — used to auto-complete.
     */
    @Query("SELECT o FROM Order o WHERE o.status = com.fooddelivery.order.entity.OrderStatus.DELIVERED " +
            "AND o.deliveredAt < :threshold")
    List<Order> findDeliveredBefore(@Param("threshold") LocalDateTime threshold);

    /**
     * Delivery orders stuck READY with no courier assigned past the threshold — used to time out.
     */
    @Query("SELECT o FROM Order o WHERE o.status = com.fooddelivery.order.entity.OrderStatus.READY " +
            "AND o.courier IS NULL " +
            "AND o.orderType = com.fooddelivery.order.entity.OrderType.DELIVERY " +
            "AND o.readyAt < :threshold")
    List<Order> findReadyWithoutCourierBefore(@Param("threshold") LocalDateTime threshold);
}
