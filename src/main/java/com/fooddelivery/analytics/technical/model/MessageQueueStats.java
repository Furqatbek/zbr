package com.fooddelivery.analytics.technical.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity tracking message queue statistics (RabbitMQ/Kafka).
 */
@Entity
@Table(name = "message_queue_stats", indexes = {
        @Index(name = "idx_mqs_queue_name", columnList = "queue_name"),
        @Index(name = "idx_mqs_broker_type", columnList = "broker_type"),
        @Index(name = "idx_mqs_recorded_at", columnList = "recorded_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageQueueStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "queue_name", nullable = false, length = 100)
    private String queueName;

    @Column(name = "broker_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BrokerType brokerType;

    @Column(name = "queue_depth")
    private Long queueDepth;

    @Column(name = "messages_published")
    private Long messagesPublished;

    @Column(name = "messages_consumed")
    private Long messagesConsumed;

    @Column(name = "messages_acknowledged")
    private Long messagesAcknowledged;

    @Column(name = "messages_rejected")
    private Long messagesRejected;

    @Column(name = "consumer_count")
    private Integer consumerCount;

    @Column(name = "publish_rate")
    private Double publishRate;

    @Column(name = "consume_rate")
    private Double consumeRate;

    @Column(name = "avg_processing_time_ms")
    private Long avgProcessingTimeMs;

    /**
     * Kafka-specific: consumer lag
     */
    @Column(name = "consumer_lag")
    private Long consumerLag;

    /**
     * Kafka-specific: partition count
     */
    @Column(name = "partition_count")
    private Integer partitionCount;

    @Column(name = "is_healthy")
    @Builder.Default
    private Boolean isHealthy = true;

    @Column(name = "is_dead_letter_queue")
    @Builder.Default
    private Boolean isDeadLetterQueue = false;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum BrokerType {
        RABBITMQ, KAFKA
    }

    /**
     * Calculate backlog (messages waiting to be processed).
     */
    public Long getBacklog() {
        if (brokerType == BrokerType.KAFKA) {
            return consumerLag != null ? consumerLag : 0L;
        }
        return queueDepth != null ? queueDepth : 0L;
    }
}
