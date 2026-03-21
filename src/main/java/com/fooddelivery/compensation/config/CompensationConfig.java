package com.fooddelivery.compensation.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for compensation module.
 */
@Configuration
public class CompensationConfig {

    public static final String COMPENSATION_ORDER_STATUS_QUEUE = "compensation.order.status.queue";
    private static final String ORDER_EXCHANGE = "order.exchange";
    private static final String ORDER_STATUS_CHANGED_KEY = "order.status.changed";
    private static final String DLX_EXCHANGE = "dlx.exchange";

    /**
     * Queue for compensation module to receive order status changes.
     * Separate from main order processing queue.
     */
    @Bean
    public Queue compensationOrderStatusQueue() {
        return QueueBuilder.durable(COMPENSATION_ORDER_STATUS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .build();
    }

    /**
     * Bind compensation queue to order exchange.
     */
    @Bean
    public Binding compensationOrderStatusBinding(Queue compensationOrderStatusQueue) {
        return BindingBuilder.bind(compensationOrderStatusQueue)
                .to(new TopicExchange(ORDER_EXCHANGE))
                .with(ORDER_STATUS_CHANGED_KEY);
    }
}
