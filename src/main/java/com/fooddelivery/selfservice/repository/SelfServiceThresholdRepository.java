package com.fooddelivery.selfservice.repository;

import com.fooddelivery.selfservice.entity.SelfServiceResolutionType;
import com.fooddelivery.selfservice.entity.SelfServiceThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for self-service thresholds configuration.
 */
@Repository
public interface SelfServiceThresholdRepository extends JpaRepository<SelfServiceThreshold, Long> {

    /**
     * Find threshold by resolution type.
     */
    Optional<SelfServiceThreshold> findByResolutionType(SelfServiceResolutionType resolutionType);

    /**
     * Find all active thresholds.
     */
    List<SelfServiceThreshold> findByActiveTrue();

    /**
     * Check if a resolution type exists.
     */
    boolean existsByResolutionType(SelfServiceResolutionType resolutionType);
}
