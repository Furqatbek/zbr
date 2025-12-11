package com.fooddelivery.analytics.technical.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Summary DTO providing high-level technical health overview.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicalSummaryDto {

    /**
     * Overall system health status.
     */
    private BackendResourceMetricsDto.HealthStatus overallHealth;

    /**
     * API health score (0-100).
     */
    private Integer apiHealthScore;

    /**
     * Backend health score (0-100).
     */
    private Integer backendHealthScore;

    /**
     * Database health score (0-100).
     */
    private Integer databaseHealthScore;

    /**
     * WebSocket health score (0-100).
     */
    private Integer websocketHealthScore;

    /**
     * Storage health score (0-100).
     */
    private Integer storageHealthScore;

    /**
     * Key API metrics.
     */
    private ApiSummaryDto api;

    /**
     * Key backend metrics.
     */
    private BackendSummaryDto backend;

    /**
     * Key database metrics.
     */
    private DatabaseSummaryDto database;

    /**
     * Key WebSocket metrics.
     */
    private WebSocketSummaryDto websocket;

    /**
     * Key storage metrics.
     */
    private StorageSummaryDto storage;

    /**
     * Active alerts count.
     */
    private Integer activeAlertsCount;

    /**
     * Timestamp when summary was generated.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime generatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiSummaryDto {
        private Double requestsPerMinute;
        private Long p99ResponseTimeMs;
        private Double errorRate;
        private Double timeoutRate;
        private BackendResourceMetricsDto.HealthStatus status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BackendSummaryDto {
        private Double cpuUsage;
        private Double memoryUsage;
        private Double heapUsage;
        private Integer activeDbConnections;
        private Long redisLatencyMs;
        private Long messageQueueBacklog;
        private BackendResourceMetricsDto.HealthStatus status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DatabaseSummaryDto {
        private Long slowQueryCount;
        private Double indexHitRate;
        private Double backupSuccessRate;
        private Long storageSizeBytes;
        private Double storageGrowthPercent;
        private BackendResourceMetricsDto.HealthStatus status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WebSocketSummaryDto {
        private Long activeConnections;
        private Long connectedCouriers;
        private Long connectedConsumers;
        private Long p99MessageDelayMs;
        private Double droppedConnectionRate;
        private BackendResourceMetricsDto.HealthStatus status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageSummaryDto {
        private Long totalFiles;
        private Double totalStorageGb;
        private Double uploadSuccessRate;
        private Long p90UploadLatencyMs;
        private Long failedUploads;
        private BackendResourceMetricsDto.HealthStatus status;
    }
}
