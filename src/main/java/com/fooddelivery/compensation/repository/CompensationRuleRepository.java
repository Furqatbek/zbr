package com.fooddelivery.compensation.repository;

import com.fooddelivery.compensation.entity.CompensationRule;
import com.fooddelivery.compensation.entity.CompensationTriggerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for compensation rules.
 */
@Repository
public interface CompensationRuleRepository extends JpaRepository<CompensationRule, Long> {

    /**
     * Find all active rules for a trigger type, ordered by priority descending.
     */
    @Query("SELECT r FROM CompensationRule r WHERE r.triggerType = :triggerType AND r.active = true ORDER BY r.priority DESC")
    List<CompensationRule> findActiveRulesByTriggerType(CompensationTriggerType triggerType);

    /**
     * Find all active rules.
     */
    List<CompensationRule> findByActiveTrue();

    /**
     * Find rules by trigger type.
     */
    List<CompensationRule> findByTriggerType(CompensationTriggerType triggerType);
}
