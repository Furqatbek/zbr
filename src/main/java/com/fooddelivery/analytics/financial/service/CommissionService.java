package com.fooddelivery.analytics.financial.service;

import com.fooddelivery.analytics.financial.model.CommissionType;
import com.fooddelivery.analytics.financial.model.RestaurantCommission;
import com.fooddelivery.analytics.financial.repository.RestaurantCommissionRepository;
import com.fooddelivery.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records platform commission earned from each completed order.
 * Called when an order reaches DELIVERED (delivery) or COMPLETED (pickup/dine-in).
 * Idempotent — a given order produces at most one commission record.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommissionService {

    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("15.00");

    private final RestaurantCommissionRepository commissionRepository;

    @Transactional
    public void recordCommission(Order order) {
        try {
            if (order == null || order.getId() == null) {
                return;
            }
            // Idempotency: skip if a commission already exists for this order
            if (commissionRepository.existsByOrderId(order.getId())) {
                return;
            }

            BigDecimal subtotal = order.getSubtotal() != null ? order.getSubtotal() : BigDecimal.ZERO;
            BigDecimal rate = order.getRestaurant() != null && order.getRestaurant().getCommissionRate() != null
                    ? order.getRestaurant().getCommissionRate()
                    : DEFAULT_COMMISSION_RATE;

            RestaurantCommission commission = RestaurantCommission.builder()
                    .restaurantId(order.getRestaurant().getId())
                    .orderId(order.getId())
                    .orderSubtotal(subtotal)
                    .commissionRate(rate)
                    .commissionType(CommissionType.PERCENTAGE)
                    .earnedAt(LocalDateTime.now())
                    .build();
            commission.calculateCommission();

            commissionRepository.save(commission);
            log.info("Commission recorded for order {}: {} ({}% of {})",
                    order.getId(), commission.getCommissionAmount(), rate, subtotal);
        } catch (Exception e) {
            // Never block order completion because of commission accounting
            log.warn("Failed to record commission for order {}: {}",
                    order != null ? order.getId() : null, e.getMessage());
        }
    }
}
