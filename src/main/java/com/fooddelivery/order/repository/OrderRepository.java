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

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.consumer LEFT JOIN FETCH o.restaurant LEFT JOIN FETCH o.courier WHERE o.id = :id")
    Optional<Order> findByIdWithLock(@Param("id") Long id);

    Page<Order> findByConsumerId(Long consumerId, Pageable pageable);

    Page<Order> findByRestaurantId(Long restaurantId, Pageable pageable);

    Page<Order> findByCourierId(Long courierId, Pageable pageable);

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

    @Query("SELECT o FROM Order o WHERE o.courier.id = :courierId AND o.status IN :statuses")
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
            "WHERE o.status = com.fooddelivery.order.entity.OrderStatus.READY " +
            "AND o.orderType = com.fooddelivery.order.entity.OrderType.DELIVERY " +
            "AND o.courier IS NULL " +
            "ORDER BY o.readyAt ASC")
    List<Order> findAvailableOrdersForCourier();

    /**
     * Get problematic orders (cancelled, refunded).
     */
    @Query("SELECT o FROM Order o WHERE o.status IN :statuses ORDER BY o.updatedAt DESC")
    Page<Order> findByStatusIn(@Param("statuses") List<OrderStatus> statuses, Pageable pageable);
}
