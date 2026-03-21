package com.fooddelivery.compensation.repository;

import com.fooddelivery.compensation.entity.CompensationLog;
import com.fooddelivery.compensation.entity.CompensationStatus;
import com.fooddelivery.compensation.entity.CompensationTriggerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for compensation logs.
 */
@Repository
public interface CompensationLogRepository extends JpaRepository<CompensationLog, Long> {

    /**
     * Find compensations by user ID.
     */
    Page<CompensationLog> findByUserId(Long userId, Pageable pageable);

    /**
     * Find compensations by order ID.
     */
    List<CompensationLog> findByOrderId(Long orderId);

    /**
     * Find compensations by status.
     */
    List<CompensationLog> findByStatus(CompensationStatus status);

    /**
     * Check if compensation already issued for an order and trigger type.
     */
    boolean existsByOrderIdAndTriggerType(Long orderId, CompensationTriggerType triggerType);

    /**
     * Get total compensation amount for a user in a time period.
     */
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CompensationLog c " +
           "WHERE c.user.id = :userId AND c.status = 'COMPLETED' " +
           "AND c.createdAt >= :since")
    BigDecimal getTotalCompensationForUserSince(Long userId, LocalDateTime since);

    /**
     * Count compensations by trigger type in a time period.
     */
    @Query("SELECT COUNT(c) FROM CompensationLog c " +
           "WHERE c.triggerType = :triggerType AND c.createdAt >= :since")
    Long countByTriggerTypeSince(CompensationTriggerType triggerType, LocalDateTime since);

    /**
     * Get compensation statistics for dashboard.
     */
    @Query("SELECT c.triggerType, COUNT(c), SUM(c.amount) FROM CompensationLog c " +
           "WHERE c.status = 'COMPLETED' AND c.createdAt >= :since " +
           "GROUP BY c.triggerType")
    List<Object[]> getCompensationStatsSince(LocalDateTime since);
}
