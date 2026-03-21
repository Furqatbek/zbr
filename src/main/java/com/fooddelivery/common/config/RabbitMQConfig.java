package com.fooddelivery.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * RabbitMQ configuration for asynchronous messaging.
 * Defines exchanges, queues, and bindings for domain events.
 */
@Configuration
public class RabbitMQConfig {

    // Exchange names
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String COURIER_EXCHANGE = "courier.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String KITCHEN_EXCHANGE = "kitchen.exchange";

    // Queue names
    public static final String ORDER_CREATED_QUEUE = "order.created.queue";
    public static final String ORDER_STATUS_CHANGED_QUEUE = "order.status.changed.queue";
    public static final String COMPENSATION_ORDER_STATUS_QUEUE = "compensation.order.status.queue";
    public static final String PAYMENT_CONFIRMED_QUEUE = "payment.confirmed.queue";
    public static final String PAYMENT_FAILED_QUEUE = "payment.failed.queue";
    public static final String COURIER_ASSIGNED_QUEUE = "courier.assigned.queue";
    public static final String COURIER_LOCATION_QUEUE = "courier.location.queue";
    public static final String NOTIFICATION_EMAIL_QUEUE = "notification.email.queue";
    public static final String NOTIFICATION_SMS_QUEUE = "notification.sms.queue";
    public static final String NOTIFICATION_PUSH_QUEUE = "notification.push.queue";
    public static final String KITCHEN_TICKET_QUEUE = "kitchen.ticket.queue";

    // Routing keys
    public static final String ORDER_CREATED_KEY = "order.created";
    public static final String ORDER_STATUS_CHANGED_KEY = "order.status.changed";
    public static final String PAYMENT_CONFIRMED_KEY = "payment.confirmed";
    public static final String PAYMENT_FAILED_KEY = "payment.failed";
    public static final String COURIER_ASSIGNED_KEY = "courier.assigned";
    public static final String COURIER_LOCATION_KEY = "courier.location";
    public static final String NOTIFICATION_EMAIL_KEY = "notification.email";
    public static final String NOTIFICATION_SMS_KEY = "notification.sms";
    public static final String NOTIFICATION_PUSH_KEY = "notification.push";
    public static final String KITCHEN_TICKET_KEY = "kitchen.ticket";

    // Dead letter
    public static final String DLX_EXCHANGE = "dlx.exchange";
    public static final String DLQ_QUEUE = "dlq.queue";

    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);

        // Configure type mapper to trust all fooddelivery packages for deserialization
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages(
                "com.fooddelivery.sms.dto",
                "com.fooddelivery.notification.dto",
                "com.fooddelivery.order.dto",
                "com.fooddelivery.payment.dto",
                "com.fooddelivery.courier.dto",
                "com.fooddelivery.kitchen.dto",
                "com.fooddelivery.common.dto",
                // Event packages for domain events deserialization
                "com.fooddelivery.order.event",
                "com.fooddelivery.notification.event",
                "com.fooddelivery.kitchen.event",
                "com.fooddelivery.common.event",
                // Order issue resolution modules
                "com.fooddelivery.compensation.dto",
                "com.fooddelivery.compensation.event",
                "com.fooddelivery.escalation.dto",
                "com.fooddelivery.escalation.event",
                "com.fooddelivery.courier.reassignment.dto",
                "com.fooddelivery.courier.reassignment.event",
                "com.fooddelivery.selfservice.dto",
                "com.fooddelivery.delivery.proof.dto"
        );
        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    @Primary
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setPrefetchCount(10);
        return factory;
    }

    // Dead Letter Exchange
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlqQueue()).to(dlxExchange()).with("dlq");
    }

    // Order Exchange and Queues
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue orderStatusChangedQueue() {
        return QueueBuilder.durable(ORDER_STATUS_CHANGED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue())
                .to(orderExchange())
                .with(ORDER_CREATED_KEY);
    }

    @Bean
    public Binding orderStatusChangedBinding() {
        return BindingBuilder.bind(orderStatusChangedQueue())
                .to(orderExchange())
                .with(ORDER_STATUS_CHANGED_KEY);
    }

    @Bean
    public Queue compensationOrderStatusQueue() {
        return QueueBuilder.durable(COMPENSATION_ORDER_STATUS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding compensationOrderStatusBinding() {
        return BindingBuilder.bind(compensationOrderStatusQueue())
                .to(orderExchange())
                .with(ORDER_STATUS_CHANGED_KEY);
    }

    // Payment Exchange and Queues
    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Queue paymentConfirmedQueue() {
        return QueueBuilder.durable(PAYMENT_CONFIRMED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(PAYMENT_FAILED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding paymentConfirmedBinding() {
        return BindingBuilder.bind(paymentConfirmedQueue())
                .to(paymentExchange())
                .with(PAYMENT_CONFIRMED_KEY);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue())
                .to(paymentExchange())
                .with(PAYMENT_FAILED_KEY);
    }

    // Courier Exchange and Queues
    @Bean
    public TopicExchange courierExchange() {
        return new TopicExchange(COURIER_EXCHANGE);
    }

    @Bean
    public Queue courierAssignedQueue() {
        return QueueBuilder.durable(COURIER_ASSIGNED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue courierLocationQueue() {
        return QueueBuilder.durable(COURIER_LOCATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding courierAssignedBinding() {
        return BindingBuilder.bind(courierAssignedQueue())
                .to(courierExchange())
                .with(COURIER_ASSIGNED_KEY);
    }

    @Bean
    public Binding courierLocationBinding() {
        return BindingBuilder.bind(courierLocationQueue())
                .to(courierExchange())
                .with(COURIER_LOCATION_KEY);
    }

    // Notification Exchange and Queues
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue notificationEmailQueue() {
        return QueueBuilder.durable(NOTIFICATION_EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue notificationSmsQueue() {
        return QueueBuilder.durable(NOTIFICATION_SMS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue notificationPushQueue() {
        return QueueBuilder.durable(NOTIFICATION_PUSH_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding notificationEmailBinding() {
        return BindingBuilder.bind(notificationEmailQueue())
                .to(notificationExchange())
                .with(NOTIFICATION_EMAIL_KEY);
    }

    @Bean
    public Binding notificationSmsBinding() {
        return BindingBuilder.bind(notificationSmsQueue())
                .to(notificationExchange())
                .with(NOTIFICATION_SMS_KEY);
    }

    @Bean
    public Binding notificationPushBinding() {
        return BindingBuilder.bind(notificationPushQueue())
                .to(notificationExchange())
                .with(NOTIFICATION_PUSH_KEY);
    }

    // Kitchen Exchange and Queues
    @Bean
    public TopicExchange kitchenExchange() {
        return new TopicExchange(KITCHEN_EXCHANGE);
    }

    @Bean
    public Queue kitchenTicketQueue() {
        return QueueBuilder.durable(KITCHEN_TICKET_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Binding kitchenTicketBinding() {
        return BindingBuilder.bind(kitchenTicketQueue())
                .to(kitchenExchange())
                .with(KITCHEN_TICKET_KEY);
    }
}
