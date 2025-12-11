package com.fooddelivery.order.repository;

import com.fooddelivery.order.entity.Payment;
import com.fooddelivery.order.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Payment entity operations.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    Optional<Payment> findByPaymentIntentId(String paymentIntentId);

    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId AND p.status = :status")
    Optional<Payment> findByOrderIdAndStatus(
            @Param("orderId") Long orderId,
            @Param("status") PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.status = 'CONFIRMED' AND p.createdAt >= :since")
    List<Payment> findConfirmedPaymentsSince(@Param("since") LocalDateTime since);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'CONFIRMED' AND p.createdAt >= :since")
    java.math.BigDecimal sumConfirmedPaymentsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status AND p.createdAt >= :since")
    long countByStatusSince(
            @Param("status") PaymentStatus status,
            @Param("since") LocalDateTime since);
}
