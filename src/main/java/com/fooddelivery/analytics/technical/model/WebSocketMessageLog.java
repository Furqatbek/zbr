package com.fooddelivery.analytics.technical.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity tracking WebSocket message delivery for latency analysis.
 */
@Entity
@Table(name = "websocket_message_logs", indexes = {
        @Index(name = "idx_wsml_session_id", columnList = "session_id"),
        @Index(name = "idx_wsml_message_type", columnList = "message_type"),
        @Index(name = "idx_wsml_published_at", columnList = "published_at"),
        @Index(name = "idx_wsml_delivered_at", columnList = "delivered_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketMessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, length = 50)
    private String messageId;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Column(name = "message_type", nullable = false, length = 50)
    private String messageType;

    @Column(name = "destination", length = 200)
    private String destination;

    @Column(name = "payload_size_bytes")
    private Long payloadSizeBytes;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "is_delivered")
    @Builder.Default
    private Boolean isDelivered = false;

    @Column(name = "delivery_attempts")
    @Builder.Default
    private Integer deliveryAttempts = 0;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Calculate delivery delay in milliseconds.
     */
    public Long getDeliveryDelayMs() {
        if (publishedAt == null || deliveredAt == null) return null;
        return java.time.Duration.between(publishedAt, deliveredAt).toMillis();
    }
}
