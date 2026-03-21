package com.fooddelivery.compensation.event;

import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.compensation.service.CompensationService;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Event consumer for automatic compensation processing.
 * Listens to order status changes and triggers compensation when applicable.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CompensationEventConsumer {

    private final CompensationService compensationService;

    /**
     * Process order status changes for potential compensation.
     * Note: Uses a separate queue to avoid interfering with main order processing.
     */
    @RabbitListener(
            queues = "#{@compensationOrderStatusQueue.name}",
            id = "compensationOrderStatusListener",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.debug("Processing order status change for compensation: orderId={}, {} -> {}",
                event.getOrderId(), event.getPreviousStatus(), event.getNewStatus());

        try {
            // Check for late delivery compensation
            if (event.getNewStatus() == OrderStatus.DELIVERED) {
                processDeliveredOrder(event);
            }

            // Check for cancelled orders that may need compensation
            if (event.getNewStatus() == OrderStatus.CANCELLED) {
                processCancelledOrder(event);
            }

        } catch (Exception e) {
            log.error("Failed to process compensation for order {}: {}",
                    event.getOrderId(), e.getMessage(), e);
            // Don't re-throw - compensation failures shouldn't block order processing
        }
    }

    /**
     * Process delivered orders for late delivery compensation.
     */
    private void processDeliveredOrder(OrderStatusChangedEvent event) {
        try {
            compensationService.processLateDelivery(event.getOrderId())
                    .ifPresent(entry -> log.info("Late delivery compensation issued for order {}: {}",
                            event.getOrderId(), entry.getAmount()));
        } catch (Exception e) {
            log.error("Failed to process late delivery compensation for order {}: {}",
                    event.getOrderId(), e.getMessage());
        }
    }

    /**
     * Process cancelled orders for potential compensation.
     */
    private void processCancelledOrder(OrderStatusChangedEvent event) {
        // Only compensate if cancelled after preparation started
        OrderStatus prevStatus = event.getPreviousStatus();
        if (prevStatus == OrderStatus.PREPARING ||
            prevStatus == OrderStatus.READY ||
            prevStatus == OrderStatus.PICKED_UP ||
            prevStatus == OrderStatus.IN_TRANSIT) {

            log.info("Order {} cancelled after preparation - compensation may apply",
                    event.getOrderId());
            // Actual compensation for cancelled orders is handled in OrderService.cancelOrder()
            // to ensure proper refund processing
        }
    }
}
