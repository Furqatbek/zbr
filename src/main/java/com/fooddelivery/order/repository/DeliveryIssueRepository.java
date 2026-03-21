package com.fooddelivery.order.repository;

import com.fooddelivery.order.entity.DeliveryIssue;
import com.fooddelivery.order.entity.DeliveryIssueType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for DeliveryIssue entity operations.
 */
@Repository
public interface DeliveryIssueRepository extends JpaRepository<DeliveryIssue, Long> {

    List<DeliveryIssue> findByOrderId(Long orderId);

    List<DeliveryIssue> findByCourierId(Long courierId);

    Page<DeliveryIssue> findByResolved(Boolean resolved, Pageable pageable);

    @Query("SELECT di FROM DeliveryIssue di WHERE di.resolved = false ORDER BY di.createdAt DESC")
    Page<DeliveryIssue> findUnresolvedIssues(Pageable pageable);

    @Query("SELECT di FROM DeliveryIssue di WHERE di.issueType = :issueType AND di.resolved = false")
    List<DeliveryIssue> findUnresolvedByType(@Param("issueType") DeliveryIssueType issueType);

    @Query("SELECT COUNT(di) FROM DeliveryIssue di WHERE di.courier.id = :courierId AND di.resolved = false")
    long countUnresolvedByCourier(@Param("courierId") Long courierId);

    @Query("SELECT DISTINCT di.order.id FROM DeliveryIssue di WHERE di.resolved = false")
    List<Long> findOrderIdsWithUnresolvedIssues();
}
