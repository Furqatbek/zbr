package com.fooddelivery.escalation.repository;

import com.fooddelivery.escalation.entity.EscalationRule;
import com.fooddelivery.escalation.entity.EscalationTriggerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for escalation rules.
 */
@Repository
public interface EscalationRuleRepository extends JpaRepository<EscalationRule, Long> {

    /**
     * Find all active rules ordered by priority.
     */
    @Query("SELECT r FROM EscalationRule r WHERE r.active = true ORDER BY r.priority DESC")
    List<EscalationRule> findActiveRules();

    /**
     * Find active rules by trigger type.
     */
    @Query("SELECT r FROM EscalationRule r WHERE r.triggerType = :triggerType AND r.active = true ORDER BY r.priority DESC")
    List<EscalationRule> findActiveRulesByTriggerType(EscalationTriggerType triggerType);

    /**
     * Find all rules by trigger type.
     */
    List<EscalationRule> findByTriggerType(EscalationTriggerType triggerType);
}
