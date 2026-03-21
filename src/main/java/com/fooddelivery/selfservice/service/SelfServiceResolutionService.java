package com.fooddelivery.selfservice.service;

import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.compensation.service.CreditService;
import com.fooddelivery.order.dto.CancelOrderRequest;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.order.service.OrderService;
import com.fooddelivery.order.service.PaymentService;
import com.fooddelivery.selfservice.entity.SelfServiceResolution;
import com.fooddelivery.selfservice.entity.SelfServiceResolutionType;
import com.fooddelivery.selfservice.repository.SelfServiceResolutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for customer self-service resolutions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SelfServiceResolutionService {

    private final SelfServiceResolutionRepository resolutionRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final CreditService creditService;
    private final OrderService orderService;

    @Value("${app.selfservice.max-auto-refund:10.00}")
    private BigDecimal maxAutoRefund;

    @Value("${app.selfservice.max-daily-resolutions:3}")
    private int maxDailyResolutions;

    /**
     * Request auto-refund for small amounts.
     */
    @Transactional
    public SelfServiceResolution requestAutoRefund(Long userId, Long orderId, String reason) {
        validateDailyLimit(userId, SelfServiceResolutionType.AUTO_REFUND_SMALL);
        validateNotAlreadyResolved(orderId, SelfServiceResolutionType.AUTO_REFUND_SMALL);

        Order order = getOrderForUser(orderId, userId);
        BigDecimal refundAmount = order.getTotal();

        if (refundAmount.compareTo(maxAutoRefund) > 0) {
            throw new BusinessException("Order total exceeds auto-refund limit of $" + maxAutoRefund);
        }

        // Process refund
        paymentService.refundPayment(orderId, refundAmount, "Self-service auto-refund: " + reason);

        // Create resolution record
        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        details.put("refundAmount", refundAmount);

        return createResolution(userId, order, SelfServiceResolutionType.AUTO_REFUND_SMALL, refundAmount, details);
    }

    /**
     * Report missing items and get refund.
     */
    @Transactional
    public SelfServiceResolution reportMissingItems(Long userId, Long orderId, List<Long> missingItemIds) {
        validateDailyLimit(userId, SelfServiceResolutionType.MISSING_ITEMS);
        validateNotAlreadyResolved(orderId, SelfServiceResolutionType.MISSING_ITEMS);

        Order order = getOrderForUser(orderId, userId);

        // Calculate missing items value
        BigDecimal missingValue = order.getItems().stream()
                .filter(item -> missingItemIds.contains(item.getId()))
                .map(item -> item.getTotalPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (missingValue.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("No valid missing items specified");
        }

        // Process refund for missing items
        paymentService.refundPayment(orderId, missingValue, "Self-service missing items refund");

        Map<String, Object> details = new HashMap<>();
        details.put("missingItemIds", missingItemIds);
        details.put("refundAmount", missingValue);

        return createResolution(userId, order, SelfServiceResolutionType.MISSING_ITEMS, missingValue, details);
    }

    /**
     * Cancel order before preparation.
     */
    @Transactional
    public SelfServiceResolution cancelPrePrep(Long userId, Long orderId, String reason) {
        validateDailyLimit(userId, SelfServiceResolutionType.CANCEL_PRE_PREP);

        Order order = getOrderForUser(orderId, userId);

        // Can only cancel before PREPARING status
        if (!order.getStatus().isCancellable()) {
            throw new BusinessException("Order cannot be cancelled at this stage");
        }

        if (order.getStatus() == OrderStatus.PREPARING ||
            order.getStatus() == OrderStatus.READY ||
            order.getStatus() == OrderStatus.PICKED_UP) {
            throw new BusinessException("Order is already being prepared - please contact support");
        }

        // Cancel the order - this will trigger refund
        CancelOrderRequest cancelRequest = CancelOrderRequest.builder()
                .reason(reason)
                .requestRefund(true)
                .build();
        orderService.cancelOrder(orderId, cancelRequest, userId);

        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        details.put("cancelledStatus", order.getStatus().name());

        return createResolution(userId, order, SelfServiceResolutionType.CANCEL_PRE_PREP, order.getTotal(), details);
    }

    /**
     * Check eligibility for self-service resolution.
     */
    @Transactional(readOnly = true)
    public Map<String, Boolean> checkEligibility(Long userId, Long orderId) {
        Map<String, Boolean> eligibility = new HashMap<>();

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || !order.getConsumer().getId().equals(userId)) {
            return eligibility;
        }

        // Auto-refund eligibility
        eligibility.put("autoRefund",
                order.getTotal().compareTo(maxAutoRefund) <= 0 &&
                !resolutionRepository.existsByOrderIdAndResolutionType(orderId, SelfServiceResolutionType.AUTO_REFUND_SMALL));

        // Missing items eligibility (only for delivered orders)
        eligibility.put("missingItems",
                order.getStatus() == OrderStatus.DELIVERED &&
                !resolutionRepository.existsByOrderIdAndResolutionType(orderId, SelfServiceResolutionType.MISSING_ITEMS));

        // Cancel eligibility
        eligibility.put("cancel", order.isCancellable() &&
                order.getStatus() != OrderStatus.PREPARING &&
                order.getStatus() != OrderStatus.READY);

        return eligibility;
    }

    /**
     * Get resolution history for user.
     */
    @Transactional(readOnly = true)
    public List<SelfServiceResolution> getResolutionHistory(Long userId) {
        return resolutionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private Order getOrderForUser(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getConsumer().getId().equals(userId)) {
            throw new BusinessException("Order does not belong to this user");
        }

        return order;
    }

    private void validateDailyLimit(Long userId, SelfServiceResolutionType type) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        Long count = resolutionRepository.countByUserIdAndResolutionTypeSince(userId, type, startOfDay);

        if (count >= maxDailyResolutions) {
            throw new BusinessException("Daily self-service limit reached. Please contact support.");
        }
    }

    private void validateNotAlreadyResolved(Long orderId, SelfServiceResolutionType type) {
        if (resolutionRepository.existsByOrderIdAndResolutionType(orderId, type)) {
            throw new BusinessException("This order has already been resolved via self-service");
        }
    }

    private SelfServiceResolution createResolution(Long userId, Order order,
                                                    SelfServiceResolutionType type,
                                                    BigDecimal amount,
                                                    Map<String, Object> details) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        SelfServiceResolution resolution = SelfServiceResolution.builder()
                .user(user)
                .order(order)
                .resolutionType(type)
                .amount(amount)
                .resolutionDetails(details)
                .status("COMPLETED")
                .build();

        resolution = resolutionRepository.save(resolution);
        log.info("Self-service resolution created: type={}, orderId={}, amount={}",
                type, order.getId(), amount);

        return resolution;
    }
}
