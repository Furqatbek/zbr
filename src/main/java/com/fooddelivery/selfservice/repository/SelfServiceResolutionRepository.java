package com.fooddelivery.selfservice.repository;

import com.fooddelivery.selfservice.entity.SelfServiceResolution;
import com.fooddelivery.selfservice.entity.SelfServiceResolutionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for self-service resolutions.
 */
@Repository
public interface SelfServiceResolutionRepository extends JpaRepository<SelfServiceResolution, Long> {

    /**
     * Find resolutions by user.
     */
    List<SelfServiceResolution> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find resolutions by order.
     */
    List<SelfServiceResolution> findByOrderId(Long orderId);

    /**
     * Count user resolutions of a type today.
     */
    @Query("SELECT COUNT(r) FROM SelfServiceResolution r " +
           "WHERE r.user.id = :userId " +
           "AND r.resolutionType = :type " +
           "AND r.createdAt >= :since")
    Long countByUserIdAndResolutionTypeSince(@Param("userId") Long userId, @Param("type") SelfServiceResolutionType type, @Param("since") LocalDateTime since);

    /**
     * Check if order already has a resolution of this type.
     */
    boolean existsByOrderIdAndResolutionType(Long orderId, SelfServiceResolutionType type);
}
