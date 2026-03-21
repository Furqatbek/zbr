package com.fooddelivery.compensation.service;

import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.compensation.dto.CompensationContext;
import com.fooddelivery.compensation.dto.CompensationResult;
import com.fooddelivery.compensation.entity.*;
import com.fooddelivery.compensation.repository.CompensationLogRepository;
import com.fooddelivery.compensation.repository.CompensationRuleRepository;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.order.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for processing compensations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompensationService {

    private final CompensationRuleEngine ruleEngine;
    private final CreditService creditService;
    private final CompensationLogRepository logRepository;
    private final CompensationRuleRepository ruleRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    /**
     * Process automatic compensation for an order event.
     */
    @Transactional
    public Optional<CompensationLog> processAutoCompensation(CompensationContext context) {
        // Check if already compensated for this trigger
        if (logRepository.existsByOrderIdAndTriggerType(context.getOrderId(), context.getTriggerType())) {
            log.info("Compensation already issued for order {} trigger {}",
                    context.getOrderId(), context.getTriggerType());
            return Optional.empty();
        }

        // Evaluate rules
        Optional<CompensationResult> result = ruleEngine.evaluate(context);
        if (result.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(applyCompensation(context, result.get()));
    }

    /**
     * Process late delivery compensation.
     */
    @Transactional
    public Optional<CompensationLog> processLateDelivery(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getEstimatedDeliveryTime() == null || order.getDeliveredAt() == null) {
            return Optional.empty();
        }

        long delayMinutes = Duration.between(
                order.getEstimatedDeliveryTime(),
                order.getDeliveredAt()
        ).toMinutes();

        if (delayMinutes <= 0) {
            return Optional.empty();
        }

        CompensationContext context = CompensationContext.builder()
                .orderId(orderId)
                .userId(order.getConsumer().getId())
                .triggerType(CompensationTriggerType.LATE_DELIVERY)
                .orderTotal(order.getTotalAmount())
                .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                .actualDeliveryTime(order.getDeliveredAt())
                .delayMinutes((int) delayMinutes)
                .description("Late delivery: " + delayMinutes + " minutes")
                .build();

        return processAutoCompensation(context);
    }

    /**
     * Process missing items compensation.
     */
    @Transactional
    public Optional<CompensationLog> processMissingItems(Long orderId, BigDecimal missingValue, String description) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        CompensationContext context = CompensationContext.builder()
                .orderId(orderId)
                .userId(order.getConsumer().getId())
                .triggerType(CompensationTriggerType.MISSING_ITEMS)
                .orderTotal(order.getTotalAmount())
                .missingItemsValue(missingValue)
                .description(description)
                .build();

        return processAutoCompensation(context);
    }

    /**
     * Issue manual compensation.
     */
    @Transactional
    public CompensationLog issueManualCompensation(
            Long userId,
            Long orderId,
            CompensationType type,
            BigDecimal amount,
            String description) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Order order = orderId != null ? orderRepository.findById(orderId).orElse(null) : null;

        CompensationResult result = CompensationResult.builder()
                .eligible(true)
                .compensationType(type)
                .creditAmount(type == CompensationType.CREDIT ? amount : BigDecimal.ZERO)
                .refundAmount(type == CompensationType.REFUND ? amount : BigDecimal.ZERO)
                .description(description)
                .build();

        CompensationContext context = CompensationContext.builder()
                .userId(userId)
                .orderId(orderId)
                .triggerType(CompensationTriggerType.MANUAL)
                .description(description)
                .build();

        return applyCompensation(context, result);
    }

    /**
     * Apply compensation based on result.
     */
    private CompensationLog applyCompensation(CompensationContext context, CompensationResult result) {
        User user = userRepository.findById(context.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Order order = context.getOrderId() != null
                ? orderRepository.findById(context.getOrderId()).orElse(null)
                : null;

        CompensationRule rule = result.getRuleId() != null
                ? ruleRepository.findById(result.getRuleId()).orElse(null)
                : null;

        BigDecimal totalAmount = result.getCreditAmount().add(result.getRefundAmount());

        // Create log entry
        CompensationLog logEntry = CompensationLog.builder()
                .user(user)
                .order(order)
                .triggerType(context.getTriggerType())
                .compensationType(result.getCompensationType())
                .amount(totalAmount)
                .description(result.getDescription() != null ? result.getDescription() : context.getDescription())
                .rule(rule)
                .status(CompensationStatus.PENDING)
                .build();

        logEntry = logRepository.save(logEntry);

        try {
            // Apply credit
            if (result.getCreditAmount().compareTo(BigDecimal.ZERO) > 0) {
                creditService.addCredit(context.getUserId(), result.getCreditAmount());
            }

            // Process refund
            if (result.getRefundAmount().compareTo(BigDecimal.ZERO) > 0 && order != null) {
                paymentService.refundPayment(order.getId(), result.getRefundAmount(),
                        "Compensation: " + context.getTriggerType());
            }

            logEntry.markCompleted();
            log.info("Compensation applied: {} {} to user {} for order {}",
                    result.getCompensationType(), totalAmount, context.getUserId(), context.getOrderId());

        } catch (Exception e) {
            log.error("Failed to apply compensation for order {}: {}", context.getOrderId(), e.getMessage());
            logEntry.markFailed();
        }

        return logRepository.save(logEntry);
    }

    /**
     * Get compensation history for a user.
     */
    @Transactional(readOnly = true)
    public Page<CompensationLog> getCompensationHistory(Long userId, Pageable pageable) {
        return logRepository.findByUserId(userId, pageable);
    }

    /**
     * Get compensations for an order.
     */
    @Transactional(readOnly = true)
    public List<CompensationLog> getCompensationsForOrder(Long orderId) {
        return logRepository.findByOrderId(orderId);
    }

    /**
     * Get all compensation rules.
     */
    @Transactional(readOnly = true)
    public List<CompensationRule> getAllRules() {
        return ruleRepository.findAll();
    }

    /**
     * Get active compensation rules.
     */
    @Transactional(readOnly = true)
    public List<CompensationRule> getActiveRules() {
        return ruleRepository.findByActiveTrue();
    }

    /**
     * Toggle rule active status.
     */
    @Transactional
    public CompensationRule toggleRuleStatus(Long ruleId) {
        CompensationRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));
        rule.setActive(!rule.getActive());
        return ruleRepository.save(rule);
    }
}
