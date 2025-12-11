package com.fooddelivery.analytics.technical.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity tracking WebSocket connections for real-time metrics.
 */
@Entity
@Table(name = "websocket_connection_logs", indexes = {
        @Index(name = "idx_wscl_user_type", columnList = "user_type"),
        @Index(name = "idx_wscl_connected_at", columnList = "connected_at"),
        @Index(name = "idx_wscl_disconnected_at", columnList = "disconnected_at"),
        @Index(name = "idx_wscl_session_id", columnList = "session_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketConnectionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserType userType;

    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    @Column(name = "disconnected_at")
    private LocalDateTime disconnectedAt;

    @Column(name = "disconnect_reason", length = 100)
    private String disconnectReason;

    @Column(name = "is_graceful_disconnect")
    private Boolean isGracefulDisconnect;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "messages_sent")
    @Builder.Default
    private Long messagesSent = 0L;

    @Column(name = "messages_received")
    @Builder.Default
    private Long messagesReceived = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum UserType {
        COURIER, CONSUMER, RESTAURANT, ADMIN
    }

    /**
     * Check if connection is currently active.
     */
    public boolean isActive() {
        return disconnectedAt == null;
    }

    /**
     * Get connection duration in seconds.
     */
    public Long getDurationSeconds() {
        if (connectedAt == null) return 0L;
        LocalDateTime endTime = disconnectedAt != null ? disconnectedAt : LocalDateTime.now();
        return java.time.Duration.between(connectedAt, endTime).getSeconds();
    }
}
