package com.fooddelivery.admin.dashboard.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for capturing system health metrics snapshots.
 */
@Entity
@Table(name = "system_health_snapshots", indexes = {
        @Index(name = "idx_health_snapshot_time", columnList = "captured_at"),
        @Index(name = "idx_health_snapshot_component", columnList = "component")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemHealthSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "component", nullable = false, length = 50)
    private String component; // API, DATABASE, REDIS, QUEUE

    @Column(name = "api_latency_p50")
    private Integer apiLatencyP50;

    @Column(name = "api_latency_p90")
    private Integer apiLatencyP90;

    @Column(name = "api_latency_p99")
    private Integer apiLatencyP99;

    @Column(name = "db_active_connections")
    private Integer dbActiveConnections;

    @Column(name = "db_idle_connections")
    private Integer dbIdleConnections;

    @Column(name = "db_waiting_connections")
    private Integer dbWaitingConnections;

    @Column(name = "redis_latency_ms")
    private Integer redisLatencyMs;

    @Column(name = "redis_memory_used_mb")
    private Integer redisMemoryUsedMb;

    @Column(name = "redis_connected_clients")
    private Integer redisConnectedClients;

    @Column(name = "queue_backlog")
    private Long queueBacklog;

    @Column(name = "queue_processing_rate")
    private Double queueProcessingRate;

    @Column(name = "cpu_usage_percent")
    private Double cpuUsagePercent;

    @Column(name = "memory_usage_percent")
    private Double memoryUsagePercent;

    @Column(name = "disk_usage_percent")
    private Double diskUsagePercent;

    @Column(name = "error_rate_percent")
    private Double errorRatePercent;

    @Column(name = "requests_per_second")
    private Double requestsPerSecond;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", length = 20)
    @Builder.Default
    private HealthStatus healthStatus = HealthStatus.HEALTHY;

    @CreationTimestamp
    @Column(name = "captured_at", nullable = false, updatable = false)
    private LocalDateTime capturedAt;

    /**
     * Health status enum.
     */
    public enum HealthStatus {
        HEALTHY,
        DEGRADED,
        CRITICAL,
        DOWN
    }
}
