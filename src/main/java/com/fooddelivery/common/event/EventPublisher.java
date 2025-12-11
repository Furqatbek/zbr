package com.fooddelivery.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event publisher for publishing domain events both locally and to message queues.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Publish event to RabbitMQ exchange.
     *
     * @param exchange   The exchange name
     * @param routingKey The routing key
     * @param event      The domain event
     */
    public void publish(String exchange, String routingKey, DomainEvent event) {
        log.info("Publishing event to {}/{}: {}", exchange, routingKey, event);
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.debug("Event published successfully: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish event {}: {}", event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    /**
     * Publish event asynchronously to RabbitMQ.
     */
    @Async("eventExecutor")
    public void publishAsync(String exchange, String routingKey, DomainEvent event) {
        publish(exchange, routingKey, event);
    }

    /**
     * Publish event locally within the application.
     *
     * @param event The domain event
     */
    public void publishLocal(DomainEvent event) {
        log.debug("Publishing local event: {}", event);
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * Publish event both locally and to message queue.
     */
    public void publishAll(String exchange, String routingKey, DomainEvent event) {
        publishLocal(event);
        publish(exchange, routingKey, event);
    }
}
