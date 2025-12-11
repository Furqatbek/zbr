package com.fooddelivery.analytics.technical.service;

import com.fooddelivery.analytics.technical.collector.*;
import com.fooddelivery.analytics.technical.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Implementation of technical analytics service.
 * Orchestrates collectors and manages caching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TechnicalAnalyticsServiceImpl implements TechnicalAnalyticsService {

    private final ApiPerformanceCollector apiPerformanceCollector;
    private final BackendResourceCollector backendResourceCollector;
    private final DatabasePerformanceCollector databasePerformanceCollector;
    private final WebSocketCollector webSocketCollector;
    private final StorageCollector storageCollector;

    // Cache names with TTLs defined in Redis config
    private static final String CACHE_API_METRICS = "technicalApiMetrics";
    private static final String CACHE_BACKEND_METRICS = "technicalBackendMetrics";
    private static final String CACHE_DB_METRICS = "technicalDbMetrics";
    private static final String CACHE_WS_METRICS = "technicalWsMetrics";
    private static final String CACHE_STORAGE_METRICS = "technicalStorageMetrics";
    private static final String CACHE_SUMMARY = "technicalSummary";

    @Override
    @Cacheable(value = CACHE_SUMMARY, key = "#startDate.toString() + '_' + #endDate.toString()")
    public TechnicalSummaryDto getTechnicalSummary(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating technical summary from {} to {}", startDate, endDate);

        // Build request for detailed metrics
        TechnicalMetricsRequest request = TechnicalMetricsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .topSlowQueriesLimit(5)
                .topEndpointsLimit(5)
                .includeHourlyDistribution(false)
                .includeDailyTrend(false)
                .includeTableStats(false)
                .build();

        // Collect all metrics
        ApiPerformanceMetricsDto apiMetrics = getApiPerformanceMetrics(request);
        BackendResourceMetricsDto backendMetrics = getBackendResourceMetrics();
        DatabasePerformanceMetricsDto dbMetrics = getDatabasePerformanceMetrics(request);
        WebSocketMetricsDto wsMetrics = getWebSocketMetrics(request);
        StorageMetricsDto storageMetrics = getStorageMetrics(request);

        // Calculate health scores
        Integer apiScore = calculateApiHealthScore(apiMetrics);
        Integer backendScore = calculateBackendHealthScore(backendMetrics);
        Integer dbScore = calculateDatabaseHealthScore(dbMetrics);
        Integer wsScore = calculateWebSocketHealthScore(wsMetrics);
        Integer storageScore = calculateStorageHealthScore(storageMetrics);

        // Calculate overall health
        BackendResourceMetricsDto.HealthStatus overallHealth = calculateOverallHealth(
                apiScore, backendScore, dbScore, wsScore, storageScore);

        // Build summary DTOs
        TechnicalSummaryDto.ApiSummaryDto apiSummary = TechnicalSummaryDto.ApiSummaryDto.builder()
                .requestsPerMinute(apiMetrics.getRequestsPerMinute())
                .p99ResponseTimeMs(apiMetrics.getResponseTimePercentiles() != null ?
                        apiMetrics.getResponseTimePercentiles().getP99() : null)
                .errorRate(apiMetrics.getErrorRate())
                .timeoutRate(apiMetrics.getTimeoutRate())
                .status(apiMetrics.getStatus())
                .build();

        TechnicalSummaryDto.BackendSummaryDto backendSummary = TechnicalSummaryDto.BackendSummaryDto.builder()
                .cpuUsage(backendMetrics.getCpu() != null ? backendMetrics.getCpu().getSystemCpuUsage() : null)
                .memoryUsage(backendMetrics.getMemory() != null ? backendMetrics.getMemory().getUsagePercentage() : null)
                .heapUsage(backendMetrics.getJvm() != null ? backendMetrics.getJvm().getHeapUsagePercentage() : null)
                .activeDbConnections(backendMetrics.getDatabasePool() != null ?
                        backendMetrics.getDatabasePool().getActiveConnections() : null)
                .redisLatencyMs(backendMetrics.getRedis() != null ? backendMetrics.getRedis().getPingLatencyMs() : null)
                .messageQueueBacklog(backendMetrics.getMessageQueue() != null ?
                        backendMetrics.getMessageQueue().getTotalQueueDepth() : null)
                .status(backendMetrics.getOverallStatus())
                .build();

        TechnicalSummaryDto.DatabaseSummaryDto dbSummary = TechnicalSummaryDto.DatabaseSummaryDto.builder()
                .slowQueryCount(dbMetrics.getSlowQueriesSummary() != null ?
                        dbMetrics.getSlowQueriesSummary().getTotalSlowQueries() : null)
                .indexHitRate(dbMetrics.getIndexUsage() != null ? dbMetrics.getIndexUsage().getIndexHitRate() : null)
                .backupSuccessRate(dbMetrics.getBackupSummary() != null ?
                        dbMetrics.getBackupSummary().getSuccessRate() : null)
                .storageSizeBytes(dbMetrics.getStorageStats() != null ?
                        dbMetrics.getStorageStats().getTotalDatabaseSizeBytes() : null)
                .storageGrowthPercent(null) // Calculated from historical data
                .status(databasePerformanceCollector.calculateHealthStatus(dbMetrics))
                .build();

        TechnicalSummaryDto.WebSocketSummaryDto wsSummary = TechnicalSummaryDto.WebSocketSummaryDto.builder()
                .activeConnections(wsMetrics.getTotalActiveConnections())
                .connectedCouriers(wsMetrics.getConnectedCouriers())
                .connectedConsumers(wsMetrics.getConnectedConsumers())
                .p99MessageDelayMs(wsMetrics.getMessageDelivery() != null ?
                        wsMetrics.getMessageDelivery().getP99DeliveryDelayMs() : null)
                .droppedConnectionRate(wsMetrics.getDroppedConnections() != null ?
                        wsMetrics.getDroppedConnections().getDroppedRate() : null)
                .status(webSocketCollector.calculateHealthStatus(wsMetrics))
                .build();

        TechnicalSummaryDto.StorageSummaryDto storageSummary = TechnicalSummaryDto.StorageSummaryDto.builder()
                .totalFiles(storageMetrics.getTotalFileCount())
                .totalStorageGb(storageMetrics.getTotalStorageSizeGb())
                .uploadSuccessRate(storageMetrics.getUploadPerformance() != null ?
                        storageMetrics.getUploadPerformance().getSuccessRate() : null)
                .p90UploadLatencyMs(storageMetrics.getUploadPerformance() != null ?
                        storageMetrics.getUploadPerformance().getP90UploadLatencyMs() : null)
                .failedUploads(storageMetrics.getUploadPerformance() != null ?
                        storageMetrics.getUploadPerformance().getFailedUploads() : null)
                .status(storageCollector.calculateHealthStatus(storageMetrics))
                .build();

        return TechnicalSummaryDto.builder()
                .overallHealth(overallHealth)
                .apiHealthScore(apiScore)
                .backendHealthScore(backendScore)
                .databaseHealthScore(dbScore)
                .websocketHealthScore(wsScore)
                .storageHealthScore(storageScore)
                .api(apiSummary)
                .backend(backendSummary)
                .database(dbSummary)
                .websocket(wsSummary)
                .storage(storageSummary)
                .activeAlertsCount(getActiveAlertsCount())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Cacheable(value = CACHE_API_METRICS, key = "#request.startDate.toString() + '_' + #request.endDate.toString()")
    public ApiPerformanceMetricsDto getApiPerformanceMetrics(TechnicalMetricsRequest request) {
        log.debug("Fetching API performance metrics");
        return apiPerformanceCollector.collect(
                request.getStartDate(),
                request.getEndDate(),
                request.getTopEndpointsLimit(),
                request.getIncludeHourlyDistribution()
        );
    }

    @Override
    public ApiPerformanceMetricsDto getApiPerformanceRealTime() {
        log.debug("Fetching real-time API metrics");
        return apiPerformanceCollector.collectRealTime();
    }

    @Override
    @Cacheable(value = CACHE_BACKEND_METRICS, key = "'current'")
    public BackendResourceMetricsDto getBackendResourceMetrics() {
        log.debug("Fetching backend resource metrics");
        return backendResourceCollector.collect();
    }

    @Override
    public BackendResourceMetricsDto getBackendResourceMetricsHistorical(
            LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching historical backend metrics from {} to {}", startDate, endDate);
        return backendResourceCollector.collectHistorical(startDate, endDate);
    }

    @Override
    @Cacheable(value = CACHE_DB_METRICS, key = "#request.startDate.toString() + '_' + #request.endDate.toString()")
    public DatabasePerformanceMetricsDto getDatabasePerformanceMetrics(TechnicalMetricsRequest request) {
        log.debug("Fetching database performance metrics");
        return databasePerformanceCollector.collect(
                request.getStartDate(),
                request.getEndDate(),
                request.getSlowQueryThresholdMs(),
                request.getTopSlowQueriesLimit(),
                request.getIncludeTableStats()
        );
    }

    @Override
    @Cacheable(value = CACHE_WS_METRICS, key = "#request.startDate.toString() + '_' + #request.endDate.toString()")
    public WebSocketMetricsDto getWebSocketMetrics(TechnicalMetricsRequest request) {
        log.debug("Fetching WebSocket metrics");
        return webSocketCollector.collect(
                request.getStartDate(),
                request.getEndDate(),
                request.getIncludeHourlyDistribution()
        );
    }

    @Override
    public WebSocketMetricsDto getWebSocketMetricsRealTime() {
        log.debug("Fetching real-time WebSocket metrics");
        return webSocketCollector.collectRealTime();
    }

    @Override
    @Cacheable(value = CACHE_STORAGE_METRICS, key = "#request.startDate.toString() + '_' + #request.endDate.toString()")
    public StorageMetricsDto getStorageMetrics(TechnicalMetricsRequest request) {
        log.debug("Fetching storage metrics");
        return storageCollector.collect(
                request.getStartDate(),
                request.getEndDate(),
                request.getIncludeDailyTrend()
        );
    }

    @Override
    public StorageMetricsDto getStorageSummary() {
        log.debug("Fetching storage summary");
        return storageCollector.collectSummary();
    }

    @Override
    public Integer calculateApiHealthScore(ApiPerformanceMetricsDto metrics) {
        if (metrics == null) return 0;

        int score = 100;

        // Error rate penalty (max -40 points)
        if (metrics.getErrorRate() != null) {
            double errorPct = metrics.getErrorRate() * 100;
            if (errorPct > 5) score -= 40;
            else if (errorPct > 2) score -= 25;
            else if (errorPct > 1) score -= 15;
            else if (errorPct > 0.5) score -= 5;
        }

        // Response time penalty (max -30 points)
        if (metrics.getResponseTimePercentiles() != null && metrics.getResponseTimePercentiles().getP99() != null) {
            long p99 = metrics.getResponseTimePercentiles().getP99();
            if (p99 > 5000) score -= 30;
            else if (p99 > 2000) score -= 20;
            else if (p99 > 1000) score -= 10;
            else if (p99 > 500) score -= 5;
        }

        // Timeout rate penalty (max -30 points)
        if (metrics.getTimeoutRate() != null) {
            double timeoutPct = metrics.getTimeoutRate() * 100;
            if (timeoutPct > 1) score -= 30;
            else if (timeoutPct > 0.5) score -= 20;
            else if (timeoutPct > 0.1) score -= 10;
        }

        return Math.max(0, score);
    }

    @Override
    public Integer calculateBackendHealthScore(BackendResourceMetricsDto metrics) {
        if (metrics == null) return 0;

        int score = 100;

        // CPU penalty (max -25 points)
        if (metrics.getCpu() != null && metrics.getCpu().getSystemCpuUsage() != null) {
            double cpu = metrics.getCpu().getSystemCpuUsage() * 100;
            if (cpu > 90) score -= 25;
            else if (cpu > 80) score -= 15;
            else if (cpu > 70) score -= 5;
        }

        // Memory penalty (max -25 points)
        if (metrics.getMemory() != null && metrics.getMemory().getUsagePercentage() != null) {
            double mem = metrics.getMemory().getUsagePercentage() * 100;
            if (mem > 90) score -= 25;
            else if (mem > 80) score -= 15;
            else if (mem > 70) score -= 5;
        }

        // Heap penalty (max -20 points)
        if (metrics.getJvm() != null && metrics.getJvm().getHeapUsagePercentage() != null) {
            double heap = metrics.getJvm().getHeapUsagePercentage() * 100;
            if (heap > 95) score -= 20;
            else if (heap > 85) score -= 10;
            else if (heap > 75) score -= 5;
        }

        // DB pool penalty (max -15 points)
        if (metrics.getDatabasePool() != null && metrics.getDatabasePool().getUsagePercentage() != null) {
            double pool = metrics.getDatabasePool().getUsagePercentage() * 100;
            if (pool > 95) score -= 15;
            else if (pool > 85) score -= 10;
            else if (pool > 75) score -= 5;
        }

        // Redis penalty (max -15 points)
        if (metrics.getRedis() != null) {
            if (!Boolean.TRUE.equals(metrics.getRedis().getIsConnected())) {
                score -= 15;
            } else if (metrics.getRedis().getPingLatencyMs() != null) {
                long latency = metrics.getRedis().getPingLatencyMs();
                if (latency > 50) score -= 10;
                else if (latency > 20) score -= 5;
            }
        }

        return Math.max(0, score);
    }

    @Override
    public Integer calculateDatabaseHealthScore(DatabasePerformanceMetricsDto metrics) {
        if (metrics == null) return 0;

        int score = 100;

        // Index hit rate penalty (max -30 points)
        if (metrics.getIndexUsage() != null && metrics.getIndexUsage().getIndexHitRate() != null) {
            double hitRate = metrics.getIndexUsage().getIndexHitRate() * 100;
            if (hitRate < 90) score -= 30;
            else if (hitRate < 95) score -= 15;
            else if (hitRate < 98) score -= 5;
        }

        // Backup success rate penalty (max -30 points)
        if (metrics.getBackupSummary() != null && metrics.getBackupSummary().getSuccessRate() != null) {
            double backupRate = metrics.getBackupSummary().getSuccessRate() * 100;
            if (backupRate < 90) score -= 30;
            else if (backupRate < 95) score -= 15;
            else if (backupRate < 99) score -= 5;
        }

        // Slow queries penalty (max -25 points)
        if (metrics.getSlowQueriesSummary() != null && metrics.getSlowQueriesSummary().getTotalSlowQueries() != null) {
            long slowQueries = metrics.getSlowQueriesSummary().getTotalSlowQueries();
            if (slowQueries > 100) score -= 25;
            else if (slowQueries > 50) score -= 15;
            else if (slowQueries > 10) score -= 5;
        }

        // Dead tuple percentage penalty (max -15 points)
        if (metrics.getStorageStats() != null && metrics.getStorageStats().getDeadTuplePercentage() != null) {
            double deadPct = metrics.getStorageStats().getDeadTuplePercentage() * 100;
            if (deadPct > 20) score -= 15;
            else if (deadPct > 10) score -= 10;
            else if (deadPct > 5) score -= 5;
        }

        return Math.max(0, score);
    }

    @Override
    public Integer calculateWebSocketHealthScore(WebSocketMetricsDto metrics) {
        if (metrics == null) return 0;

        int score = 100;

        // Delivery success rate penalty (max -40 points)
        if (metrics.getMessageDelivery() != null && metrics.getMessageDelivery().getDeliverySuccessRate() != null) {
            double successRate = metrics.getMessageDelivery().getDeliverySuccessRate() * 100;
            if (successRate < 95) score -= 40;
            else if (successRate < 98) score -= 20;
            else if (successRate < 99) score -= 10;
        }

        // P99 delivery delay penalty (max -30 points)
        if (metrics.getMessageDelivery() != null && metrics.getMessageDelivery().getP99DeliveryDelayMs() != null) {
            long p99 = metrics.getMessageDelivery().getP99DeliveryDelayMs();
            if (p99 > 500) score -= 30;
            else if (p99 > 200) score -= 15;
            else if (p99 > 100) score -= 5;
        }

        // Dropped connection rate penalty (max -30 points)
        if (metrics.getDroppedConnections() != null && metrics.getDroppedConnections().getDroppedRate() != null) {
            double droppedRate = metrics.getDroppedConnections().getDroppedRate() * 100;
            if (droppedRate > 5) score -= 30;
            else if (droppedRate > 2) score -= 15;
            else if (droppedRate > 1) score -= 5;
        }

        return Math.max(0, score);
    }

    @Override
    public Integer calculateStorageHealthScore(StorageMetricsDto metrics) {
        if (metrics == null) return 0;

        int score = 100;

        // Upload success rate penalty (max -40 points)
        if (metrics.getUploadPerformance() != null && metrics.getUploadPerformance().getSuccessRate() != null) {
            double successRate = metrics.getUploadPerformance().getSuccessRate() * 100;
            if (successRate < 95) score -= 40;
            else if (successRate < 98) score -= 20;
            else if (successRate < 99) score -= 10;
        }

        // P90 upload latency penalty (max -30 points)
        if (metrics.getUploadPerformance() != null && metrics.getUploadPerformance().getP90UploadLatencyMs() != null) {
            long p90 = metrics.getUploadPerformance().getP90UploadLatencyMs();
            if (p90 > 5000) score -= 30;
            else if (p90 > 2000) score -= 15;
            else if (p90 > 1000) score -= 5;
        }

        // Storage growth rate penalty (max -30 points)
        if (metrics.getStorageGrowth() != null && metrics.getStorageGrowth().getGrowthPercentage() != null) {
            double growth = metrics.getStorageGrowth().getGrowthPercentage();
            if (growth > 25) score -= 30;
            else if (growth > 15) score -= 15;
            else if (growth > 10) score -= 5;
        }

        return Math.max(0, score);
    }

    @Override
    @CacheEvict(value = {CACHE_API_METRICS, CACHE_BACKEND_METRICS, CACHE_DB_METRICS,
            CACHE_WS_METRICS, CACHE_STORAGE_METRICS, CACHE_SUMMARY}, allEntries = true)
    public void refreshAllMetrics() {
        log.info("Refreshing all technical metrics caches");
    }

    @Override
    public Integer getActiveAlertsCount() {
        // In a real implementation, this would check an alerting system
        // For now, count critical/degraded statuses
        int count = 0;

        try {
            BackendResourceMetricsDto backendMetrics = backendResourceCollector.collect();
            if (backendMetrics.getOverallStatus() == BackendResourceMetricsDto.HealthStatus.CRITICAL) {
                count += 2;
            } else if (backendMetrics.getOverallStatus() == BackendResourceMetricsDto.HealthStatus.DEGRADED) {
                count += 1;
            }
        } catch (Exception e) {
            log.warn("Could not check backend metrics for alerts: {}", e.getMessage());
        }

        return count;
    }

    private BackendResourceMetricsDto.HealthStatus calculateOverallHealth(
            Integer apiScore, Integer backendScore, Integer dbScore, Integer wsScore, Integer storageScore) {

        // Calculate weighted average
        int avgScore = (apiScore * 25 + backendScore * 25 + dbScore * 20 + wsScore * 15 + storageScore * 15) / 100;

        if (avgScore < 50) return BackendResourceMetricsDto.HealthStatus.CRITICAL;
        if (avgScore < 75) return BackendResourceMetricsDto.HealthStatus.DEGRADED;
        return BackendResourceMetricsDto.HealthStatus.HEALTHY;
    }
}
