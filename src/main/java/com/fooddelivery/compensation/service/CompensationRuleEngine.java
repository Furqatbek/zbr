package com.fooddelivery.compensation.service;

import com.fooddelivery.compensation.dto.CompensationContext;
import com.fooddelivery.compensation.dto.CompensationResult;
import com.fooddelivery.compensation.entity.CompensationRule;
import com.fooddelivery.compensation.entity.CompensationType;
import com.fooddelivery.compensation.repository.CompensationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Engine for evaluating compensation rules against order context.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompensationRuleEngine {

    private final CompensationRuleRepository ruleRepository;

    /**
     * Evaluate compensation rules and return the best matching result.
     */
    public Optional<CompensationResult> evaluate(CompensationContext context) {
        List<CompensationRule> rules = ruleRepository.findActiveRulesByTriggerType(context.getTriggerType());

        for (CompensationRule rule : rules) {
            if (matchesCondition(rule, context)) {
                CompensationResult result = calculateCompensation(rule, context);
                log.info("Compensation rule matched: {} for order {} - amount: {}",
                        rule.getName(), context.getOrderId(), result.getCreditAmount());
                return Optional.of(result);
            }
        }

        log.debug("No compensation rule matched for trigger {} on order {}",
                context.getTriggerType(), context.getOrderId());
        return Optional.empty();
    }

    /**
     * Check if a rule's conditions are met.
     */
    private boolean matchesCondition(CompensationRule rule, CompensationContext context) {
        Map<String, Object> conditions = rule.getConditionJson();
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        switch (rule.getTriggerType()) {
            case LATE_DELIVERY:
                return matchesDelayCondition(conditions, context);
            case MISSING_ITEMS:
                return context.getMissingItems() != null && !context.getMissingItems().isEmpty();
            case CANCELLED_AFTER_PREP:
            case RESTAURANT_REJECTION:
            case ORDER_NOT_DELIVERED:
                return true;
            default:
                return true;
        }
    }

    /**
     * Check if delay conditions are met.
     */
    private boolean matchesDelayCondition(Map<String, Object> conditions, CompensationContext context) {
        Integer delayMinutes = context.getDelayMinutes();
        if (delayMinutes == null) {
            return false;
        }

        Integer minDelay = getIntValue(conditions, "minDelayMinutes");
        Integer maxDelay = getIntValue(conditions, "maxDelayMinutes");

        if (minDelay != null && delayMinutes < minDelay) {
            return false;
        }
        if (maxDelay != null && delayMinutes > maxDelay) {
            return false;
        }

        return true;
    }

    /**
     * Calculate compensation amount based on rule and context.
     */
    private CompensationResult calculateCompensation(CompensationRule rule, CompensationContext context) {
        BigDecimal creditAmount = BigDecimal.ZERO;
        BigDecimal refundAmount = BigDecimal.ZERO;
        boolean freeDelivery = false;

        CompensationType type = rule.getCompensationType();

        switch (type) {
            case CREDIT:
                creditAmount = calculateAmount(rule, context);
                break;
            case REFUND:
                refundAmount = calculateRefundAmount(rule, context);
                break;
            case FREE_DELIVERY:
                freeDelivery = true;
                break;
            case CREDIT_AND_FREE_DELIVERY:
                creditAmount = calculateAmount(rule, context);
                freeDelivery = true;
                break;
            case REFUND_AND_CREDIT:
                refundAmount = calculateRefundAmount(rule, context);
                creditAmount = calculateAmount(rule, context);
                break;
        }

        return CompensationResult.builder()
                .eligible(true)
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .compensationType(type)
                .creditAmount(creditAmount)
                .refundAmount(refundAmount)
                .freeDelivery(freeDelivery)
                .description(rule.getDescription())
                .build();
    }

    /**
     * Calculate credit amount based on rule.
     */
    private BigDecimal calculateAmount(CompensationRule rule, CompensationContext context) {
        BigDecimal amount;

        if (rule.getCompensationValue() != null) {
            amount = rule.getCompensationValue();
        } else if (rule.getCompensationPercent() != null && context.getOrderTotal() != null) {
            amount = context.getOrderTotal()
                    .multiply(rule.getCompensationPercent())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            amount = BigDecimal.ZERO;
        }

        // Apply max cap
        if (rule.getMaxCompensation() != null && amount.compareTo(rule.getMaxCompensation()) > 0) {
            amount = rule.getMaxCompensation();
        }

        return amount;
    }

    /**
     * Calculate refund amount for missing items or full order.
     */
    private BigDecimal calculateRefundAmount(CompensationRule rule, CompensationContext context) {
        if (context.getMissingItemsValue() != null) {
            return context.getMissingItemsValue();
        }
        if (context.getOrderTotal() != null) {
            return context.getOrderTotal();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Helper to get integer value from conditions map.
     */
    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
