package com.fooddelivery.courier.reassignment.repository;

import com.fooddelivery.courier.reassignment.entity.ReassignmentLog;
import com.fooddelivery.courier.reassignment.entity.ReassignmentReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for reassignment logs.
 */
@Repository
public interface ReassignmentLogRepository extends JpaRepository<ReassignmentLog, Long> {

    /**
     * Find reassignments for an order.
     */
    List<ReassignmentLog> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    /**
     * Find reassignments by courier (as previous courier).
     */
    List<ReassignmentLog> findByPreviousCourierIdOrderByCreatedAtDesc(Long courierId);

    /**
     * Count reassignments for a courier in a time period.
     */
    @Query("SELECT COUNT(r) FROM ReassignmentLog r " +
           "WHERE r.previousCourier.id = :courierId AND r.createdAt >= :since")
    Long countByCourierSince(Long courierId, LocalDateTime since);

    /**
     * Count reassignments by reason in a time period.
     */
    @Query("SELECT r.reason, COUNT(r) FROM ReassignmentLog r " +
           "WHERE r.createdAt >= :since GROUP BY r.reason")
    List<Object[]> countByReasonSince(LocalDateTime since);
}
