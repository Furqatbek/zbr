package com.fooddelivery.escalation.service;

import com.fooddelivery.analytics.cx.model.SupportTicket;
import com.fooddelivery.escalation.entity.EscalationLevel;
import com.fooddelivery.escalation.entity.EscalationRule;
import com.fooddelivery.escalation.entity.EscalationTriggerType;
import com.fooddelivery.escalation.repository.EscalationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Engine for evaluating escalation rules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EscalationRuleEngine {

    private final EscalationRuleRepository ruleRepository;

    // Legal threat keywords
    private static final Set<String> DEFAULT_LEGAL_KEYWORDS = Set.of(
            "lawyer", "attorney", "lawsuit", "legal action", "sue", "court",
            "legal", "litigation", "prosecute", "damages", "compensation lawyer"
    );

    /**
     * Evaluate all active rules against a ticket.
     * Returns the highest target level among matching rules.
     */
    public Optional<EscalationResult> evaluate(SupportTicket ticket, EscalationContext context) {
        List<EscalationRule> activeRules = ruleRepository.findActiveRules();

        EscalationResult bestMatch = null;

        for (EscalationRule rule : activeRules) {
            if (matchesRule(ticket, rule, context)) {
                int currentLevel = ticket.getEscalationLevel();
                int targetLevel = rule.getTargetLevel();

                // Only consider if it would actually escalate
                if (targetLevel > currentLevel) {
                    if (bestMatch == null || targetLevel > bestMatch.targetLevel()) {
                        bestMatch = new EscalationResult(rule.getId(), targetLevel, rule.getName(), rule.getDescription());
                    }
                }
            }
        }

        return Optional.ofNullable(bestMatch);
    }

    /**
     * Check if a rule matches the ticket.
     */
    private boolean matchesRule(SupportTicket ticket, EscalationRule rule, EscalationContext context) {
        Map<String, Object> conditions = rule.getConditionJson();

        return switch (rule.getTriggerType()) {
            case SLA_BREACH -> matchesSlaBreachRule(ticket, conditions);
            case REFUND_AMOUNT -> matchesRefundAmountRule(ticket, conditions);
            case CUSTOMER_VIP -> context.isVipCustomer();
            case REPEAT_COMPLAINT -> matchesRepeatComplaintRule(conditions, context);
            case LEGAL_THREAT -> matchesLegalThreatRule(ticket, conditions);
            case CRITICAL_PRIORITY -> ticket.getPriority() == SupportTicket.TicketPriority.URGENT;
            case TIME_OPEN -> matchesTimeOpenRule(ticket, conditions);
            default -> false;
        };
    }

    private boolean matchesSlaBreachRule(SupportTicket ticket, Map<String, Object> conditions) {
        if (!ticket.getSlaBreach()) {
            return false;
        }

        Integer noResponseMinutes = getIntValue(conditions, "noResponseMinutes");
        if (noResponseMinutes == null) {
            return true;
        }

        // Check if no response since SLA breach
        if (ticket.getSlaBreachedAt() != null && ticket.getFirstResponseAt() == null) {
            Duration sinceBreached = Duration.between(ticket.getSlaBreachedAt(), LocalDateTime.now());
            return sinceBreached.toMinutes() >= noResponseMinutes;
        }

        return false;
    }

    private boolean matchesRefundAmountRule(SupportTicket ticket, Map<String, Object> conditions) {
        Double minAmount = getDoubleValue(conditions, "minAmount");
        if (minAmount == null) {
            return false;
        }

        return ticket.getRefundAmount() != null && ticket.getRefundAmount() >= minAmount;
    }

    private boolean matchesRepeatComplaintRule(Map<String, Object> conditions, EscalationContext context) {
        Integer maxComplaints = getIntValue(conditions, "maxComplaints");
        if (maxComplaints == null) {
            return false;
        }

        return context.getRecentComplaintsCount() >= maxComplaints;
    }

    private boolean matchesLegalThreatRule(SupportTicket ticket, Map<String, Object> conditions) {
        Set<String> keywords = getKeywords(conditions);
        String text = (ticket.getSubject() + " " + ticket.getDescription()).toLowerCase();

        return keywords.stream().anyMatch(text::contains);
    }

    private boolean matchesTimeOpenRule(SupportTicket ticket, Map<String, Object> conditions) {
        Integer hoursOpen = getIntValue(conditions, "hoursOpen");
        if (hoursOpen == null || ticket.getCreatedAt() == null) {
            return false;
        }

        Duration open = Duration.between(ticket.getCreatedAt(), LocalDateTime.now());
        return open.toHours() >= hoursOpen;
    }

    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> getKeywords(Map<String, Object> conditions) {
        Object keywords = conditions.get("keywords");
        if (keywords instanceof List) {
            return new HashSet<>((List<String>) keywords);
        }
        return DEFAULT_LEGAL_KEYWORDS;
    }

    public record EscalationResult(Long ruleId, int targetLevel, String ruleName, String description) {}

    /**
     * Context for escalation evaluation.
     */
    public record EscalationContext(
            boolean isVipCustomer,
            int recentComplaintsCount
    ) {
        public int getRecentComplaintsCount() {
            return recentComplaintsCount;
        }
    }
}
