package com.fooddelivery.courier.reassignment.scheduler;

import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.courier.reassignment.entity.ReassignmentInitiator;
import com.fooddelivery.courier.reassignment.entity.ReassignmentReason;
import com.fooddelivery.courier.reassignment.service.CourierReassignmentService;
import com.fooddelivery.courier.repository.CourierRepository;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task to detect and handle stuck deliveries.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StuckDeliveryChecker {

    private final OrderRepository orderRepository;
    private final CourierRepository courierRepository;
    private final CourierReassignmentService reassignmentService;

    @Value("${app.delivery.unresponsive-threshold-minutes:10}")
    private int unresponsiveThresholdMinutes;

    @Value("${app.delivery.eta-exceeded-multiplier:2.0}")
    private double etaExceededMultiplier;

    @Value("${app.delivery.stuck-order-threshold-minutes:60}")
    private int stuckOrderThresholdMinutes;

    /**
     * Check for stuck deliveries every 5 minutes.
     */
    @Scheduled(fixedDelayString = "${app.delivery.check-interval-ms:300000}")
    public void checkStuckDeliveries() {
        log.debug("Running stuck delivery check");

        try {
            checkUnresponsiveCouriers();
            checkEtaExceeded();
            checkStuckOrders();
        } catch (Exception e) {
            log.error("Error during stuck delivery check: {}", e.getMessage(), e);
        }
    }

    /**
     * Check for couriers who haven't updated their location recently.
     */
    private void checkUnresponsiveCouriers() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(unresponsiveThresholdMinutes);

        List<Order> ordersWithUnresponsiveCouriers = orderRepository.findOrdersWithInactiveCouriers(
                List.of(OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT),
                threshold
        );

        for (Order order : ordersWithUnresponsiveCouriers) {
            log.warn("Courier {} unresponsive for order {}",
                    order.getCourier().getId(), order.getId());

            try {
                reassignmentService.reassignOrder(
                        order.getId(),
                        ReassignmentReason.COURIER_UNRESPONSIVE,
                        ReassignmentInitiator.SYSTEM,
                        null,
                        "Courier unresponsive for " + unresponsiveThresholdMinutes + " minutes"
                );
            } catch (Exception e) {
                log.error("Failed to reassign order {} due to unresponsive courier: {}",
                        order.getId(), e.getMessage());
            }
        }
    }

    /**
     * Check for orders where ETA significantly exceeds original estimate.
     */
    private void checkEtaExceeded() {
        List<Order> inTransitOrders = orderRepository.findByStatusIn(
                List.of(OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT)
        );

        LocalDateTime now = LocalDateTime.now();

        for (Order order : inTransitOrders) {
            if (order.getEstimatedDeliveryTime() == null || order.getPickedUpAt() == null) {
                continue;
            }

            // Calculate original estimated duration
            Duration originalDuration = Duration.between(order.getPickedUpAt(), order.getEstimatedDeliveryTime());
            long originalMinutes = originalDuration.toMinutes();

            // Calculate actual elapsed time
            Duration elapsed = Duration.between(order.getPickedUpAt(), now);
            long elapsedMinutes = elapsed.toMinutes();

            // Check if elapsed time exceeds threshold
            if (elapsedMinutes > originalMinutes * etaExceededMultiplier) {
                log.warn("Order {} ETA exceeded: original {} min, elapsed {} min",
                        order.getId(), originalMinutes, elapsedMinutes);

                try {
                    reassignmentService.reassignOrder(
                            order.getId(),
                            ReassignmentReason.ETA_EXCEEDED,
                            ReassignmentInitiator.SYSTEM,
                            null,
                            "ETA exceeded by " + (elapsedMinutes - originalMinutes) + " minutes"
                    );
                } catch (Exception e) {
                    log.error("Failed to reassign order {} due to ETA exceeded: {}",
                            order.getId(), e.getMessage());
                }
            }
        }
    }

    /**
     * Check for orders stuck in ready status without courier assignment.
     */
    private void checkStuckOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(stuckOrderThresholdMinutes);

        List<Order> stuckOrders = orderRepository.findByStatusAndReadyAtBefore(
                OrderStatus.READY,
                threshold
        );

        for (Order order : stuckOrders) {
            if (order.getCourier() == null) {
                log.warn("Order {} stuck in READY status without courier for {} minutes",
                        order.getId(), stuckOrderThresholdMinutes);

                // Escalate to operations - could trigger notification or auto-assignment
                escalateStuckOrder(order);
            }
        }
    }

    /**
     * Escalate stuck order to operations team.
     */
    private void escalateStuckOrder(Order order) {
        log.info("Escalating stuck order {} to operations", order.getId());
        // TODO: Integrate with escalation system (Phase 3)
    }
}
