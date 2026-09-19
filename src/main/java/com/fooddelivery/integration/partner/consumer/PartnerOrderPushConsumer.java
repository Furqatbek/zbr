package com.fooddelivery.integration.partner.consumer;

import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.integration.partner.service.PartnerOrderPushService;
import com.fooddelivery.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Sends a newly created order to the partner whose till the venue cooks from.
 *
 * <p>Off the request path deliberately. A partner being slow or down must cost
 * the customer nothing at checkout: food arriving late is recoverable, an order
 * that was never taken is not.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PartnerOrderPushConsumer {

    private final PartnerOrderPushService pushService;

    @RabbitListener(
            queues = RabbitMQConfig.PARTNER_ORDER_PUSH_QUEUE,
            id = "partnerOrderPushListener",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        boolean settled = pushService.push(event.getOrderId());

        if (!settled) {
            // Rethrown so the container's retry policy takes over and, once it
            // is exhausted, the message lands in the DLQ rather than
            // disappearing. An order silently dropped here is one nobody cooks
            // and nobody knows about.
            throw new PartnerPushRetryException(
                    "Order " + event.getExternalOrderNo() + " has not reached the partner's kitchen yet");
        }
    }

    /**
     * Carries the "try again" signal to the AMQP container. Deliberately its
     * own type: it means a partner's system was unreachable, which is an
     * expected condition, and a reader of the DLQ should be able to tell it
     * apart from a bug in our own code.
     */
    public static class PartnerPushRetryException extends RuntimeException {
        public PartnerPushRetryException(String message) {
            super(message);
        }
    }
}
