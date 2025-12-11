package com.fooddelivery.analytics.technical.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity tracking HTTP request logs for API performance analysis.
 */
@Entity
@Table(name = "http_request_logs", indexes = {
        @Index(name = "idx_hrl_timestamp", columnList = "timestamp"),
        @Index(name = "idx_hrl_status_code", columnList = "status_code"),
        @Index(name = "idx_hrl_endpoint", columnList = "endpoint"),
        @Index(name = "idx_hrl_method", columnList = "method"),
        @Index(name = "idx_hrl_response_time", columnList = "response_time_ms")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HttpRequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", length = 50)
    private String requestId;

    @Column(name = "method", nullable = false, length = 10)
    private String method;

    @Column(name = "endpoint", nullable = false, length = 500)
    private String endpoint;

    @Column(name = "status_code", nullable = false)
    private Integer statusCode;

    @Column(name = "response_time_ms", nullable = false)
    private Long responseTimeMs;

    @Column(name = "request_size_bytes")
    private Long requestSizeBytes;

    @Column(name = "response_size_bytes")
    private Long responseSizeBytes;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "is_timeout")
    @Builder.Default
    private Boolean isTimeout = false;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Check if this is a client error (4xx).
     */
    public boolean isClientError() {
        return statusCode != null && statusCode >= 400 && statusCode < 500;
    }

    /**
     * Check if this is a server error (5xx).
     */
    public boolean isServerError() {
        return statusCode != null && statusCode >= 500;
    }
}
