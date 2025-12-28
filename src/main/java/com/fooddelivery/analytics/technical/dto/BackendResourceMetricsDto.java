package com.fooddelivery.analytics.technical.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Backend Resource metrics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackendResourceMetricsDto {

    /**
     * CPU metrics.
     */
    private CpuMetricsDto cpu;

    /**
     * Memory metrics.
     */
    private MemoryMetricsDto memory;

    /**
     * JVM metrics.
     */
    private JvmMetricsDto jvm;

    /**
     * Database connection pool metrics.
     */
    private DatabasePoolMetricsDto databasePool;

    /**
     * Redis metrics.
     */
    private RedisMetricsDto redis;

    /**
     * Message queue metrics.
     */
    private List<MessageQueueMetricsDto> messageQueues;

    /**
     * Single message queue metrics (for backward compatibility).
     */
    private MessageQueueMetricsDto messageQueue;

    /**
     * Overall health status.
     */
    private HealthStatus overallHealth;

    /**
     * Overall status (alias for overallHealth).
     */
    private HealthStatus overallStatus;

    /**
     * Timestamp when metrics were collected.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime collectedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CpuMetricsDto {
        /**
         * System CPU usage (0.0 - 1.0).
         */
        private Double systemCpuUsage;

        /**
         * Process CPU usage (0.0 - 1.0).
         */
        private Double processCpuUsage;

        /**
         * Available processors.
         */
        private Integer availableProcessors;

        /**
         * System load average (1 minute).
         */
        private Double systemLoadAverage;

        /**
         * Load per processor.
         */
        private Double loadPerProcessor;

        /**
         * Health status based on CPU usage.
         */
        private HealthStatus status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemoryMetricsDto {
        /**
         * Total system memory in bytes.
         */
        private Long totalMemoryBytes;

        /**
         * Used system memory in bytes.
         */
        private Long usedMemoryBytes;

        /**
         * Free system memory in bytes.
         */
        private Long freeMemoryBytes;

        /**
         * Memory usage percentage.
         */
        private Double memoryUsagePercent;

        /**
         * Usage percentage (alias).
         */
        private Double usagePercentage;

        /**
         * Health status based on memory usage.
         */
        private HealthStatus status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JvmMetricsDto {
        /**
         * Heap memory used in bytes.
         */
        private Long heapUsedBytes;

        /**
         * Heap memory committed in bytes.
         */
        private Long heapCommittedBytes;

        /**
         * Heap memory max in bytes.
         */
        private Long heapMaxBytes;

        /**
         * Heap usage percentage.
         */
        private Double heapUsagePercent;

        /**
         * Heap usage percentage (alias).
         */
        private Double heapUsagePercentage;

        /**
         * Non-heap memory used in bytes.
         */
        private Long nonHeapUsedBytes;

        /**
         * Non-heap memory max in bytes.
         */
        private Long nonHeapMaxBytes;

        /**
         * Non-heap memory committed in bytes.
         */
        private Long nonHeapCommittedBytes;

        /**
         * Number of live threads.
         */
        private Integer liveThreads;

        /**
         * Thread count.
         */
        private Integer threadCount;

        /**
         * Peak thread count.
         */
        private Integer peakThreads;

        /**
         * Peak thread count (alias).
         */
        private Integer peakThreadCount;

        /**
         * Daemon thread count.
         */
        private Integer daemonThreads;

        /**
         * Total GC pause time in ms.
         */
        private Long gcPauseTimeMs;

        /**
         * GC time in ms (alias).
         */
        private Double gcTimeMs;

        /**
         * GC count.
         */
        private Long gcCount;

        /**
         * JVM uptime in seconds.
         */
        private Long uptimeSeconds;

        /**
         * Health status based on JVM metrics.
         */
        private HealthStatus status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DatabasePoolMetricsDto {
        /**
         * Active connections.
         */
        private Integer activeConnections;

        /**
         * Idle connections.
         */
        private Integer idleConnections;

        /**
         * Total connections.
         */
        private Integer totalConnections;

        /**
         * Max connections allowed.
         */
        private Integer maxConnections;

        /**
         * Max pool size.
         */
        private Integer maxPoolSize;

        /**
         * Pending connection requests.
         */
        private Integer pendingRequests;

        /**
         * Pending connections.
         */
        private Integer pendingConnections;

        /**
         * Connection acquire time (avg) in ms.
         */
        private Double avgAcquireTimeMs;

        /**
         * Connection usage time (avg) in ms.
         */
        private Double avgUsageTimeMs;

        /**
         * Connection pool utilization (0.0 - 1.0).
         */
        private Double poolUtilization;

        /**
         * Usage percentage (0.0 - 1.0).
         */
        private Double usagePercentage;

        /**
         * Health status based on pool metrics.
         */
        private HealthStatus status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RedisMetricsDto {
        /**
         * Redis ping latency in ms.
         */
        private Long pingLatencyMs;

        /**
         * Connected clients.
         */
        private Long connectedClients;

        /**
         * Used memory in bytes.
         */
        private Long usedMemoryBytes;

        /**
         * Max memory in bytes.
         */
        private Long maxMemoryBytes;

        /**
         * Memory usage percentage.
         */
        private Double memoryUsagePercent;

        /**
         * Total keys.
         */
        private Long totalKeys;

        /**
         * Key count.
         */
        private Long keyCount;

        /**
         * Expired keys.
         */
        private Long expiredKeys;

        /**
         * Evicted keys.
         */
        private Long evictedKeys;

        /**
         * Cache hit rate (0.0 - 1.0).
         */
        private Double hitRate;

        /**
         * Operations per second.
         */
        private Double operationsPerSecond;

        /**
         * Is Redis connected.
         */
        private Boolean isConnected;

        /**
         * Health status based on Redis metrics.
         */
        private HealthStatus status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageQueueMetricsDto {
        /**
         * Queue name.
         */
        private String queueName;

        /**
         * Broker type (RABBITMQ, KAFKA).
         */
        private String brokerType;

        /**
         * Queue depth / backlog.
         */
        private Long queueDepth;

        /**
         * Total queue depth across all queues.
         */
        private Long totalQueueDepth;

        /**
         * Consumer lag (Kafka).
         */
        private Long consumerLag;

        /**
         * Total consumer lag across all queues.
         */
        private Long totalConsumerLag;

        /**
         * Dead letter queue message count.
         */
        private Long deadLetterQueueCount;

        /**
         * Queue depths by queue name.
         */
        private java.util.Map<String, Long> queueDepths;

        /**
         * Number of consumers.
         */
        private Integer consumerCount;

        /**
         * Publish rate (messages/sec).
         */
        private Double publishRate;

        /**
         * Consume rate (messages/sec).
         */
        private Double consumeRate;

        /**
         * Is queue healthy.
         */
        private Boolean isHealthy;

        /**
         * Health status.
         */
        private HealthStatus status;
    }

    public enum HealthStatus {
        HEALTHY, DEGRADED, CRITICAL, UNKNOWN
    }
}
