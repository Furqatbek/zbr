package com.fooddelivery.analytics.technical.service;

import com.fooddelivery.analytics.technical.collector.*;
import com.fooddelivery.analytics.technical.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TechnicalAnalyticsService.
 */
@ExtendWith(MockitoExtension.class)
class TechnicalAnalyticsServiceTest {

    @Mock
    private ApiPerformanceCollector apiPerformanceCollector;

    @Mock
    private BackendResourceCollector backendResourceCollector;

    @Mock
    private DatabasePerformanceCollector databasePerformanceCollector;

    @Mock
    private WebSocketCollector webSocketCollector;

    @Mock
    private StorageCollector storageCollector;

    @InjectMocks
    private TechnicalAnalyticsServiceImpl technicalAnalyticsService;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private TechnicalMetricsRequest request;

    @BeforeEach
    void setUp() {
        startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        endDate = LocalDateTime.of(2024, 1, 1, 23, 59);

        request = TechnicalMetricsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .slowQueryThresholdMs(1000L)
                .topSlowQueriesLimit(10)
                .topEndpointsLimit(10)
                .includeHourlyDistribution(true)
                .includeDailyTrend(true)
                .includeTableStats(false)
                .build();
    }

    @Nested
    @DisplayName("API Performance Metrics Tests")
    class ApiPerformanceMetricsTests {

        @Test
        @DisplayName("Should calculate API performance metrics successfully")
        void shouldCalculateApiPerformanceMetrics() {
            // Given
            ApiPerformanceMetricsDto expectedMetrics = ApiPerformanceMetricsDto.builder()
                    .totalRequests(10000L)
                    .requestsPerMinute(166.67)
                    .avgResponseTimeMs(45.5)
                    .maxResponseTimeMs(2500L)
                    .minResponseTimeMs(5L)
                    .responseTimePercentiles(ApiPerformanceMetricsDto.ResponseTimePercentilesDto.builder()
                            .p50(35L).p75(50L).p90(80L).p95(120L).p99(200L)
                            .build())
                    .totalErrors(50L)
                    .errorRate(0.005)
                    .clientErrorRate(0.003)
                    .serverErrorRate(0.002)
                    .timeoutRate(0.001)
                    .status(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(apiPerformanceCollector.collect(any(), any(), anyInt(), anyBoolean()))
                    .thenReturn(expectedMetrics);

            // When
            ApiPerformanceMetricsDto result = technicalAnalyticsService.getApiPerformanceMetrics(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalRequests()).isEqualTo(10000L);
            assertThat(result.getErrorRate()).isEqualTo(0.005);
            assertThat(result.getStatus()).isEqualTo(BackendResourceMetricsDto.HealthStatus.HEALTHY);

            verify(apiPerformanceCollector).collect(startDate, endDate, 10, true);
        }

        @Test
        @DisplayName("Should return real-time API metrics")
        void shouldReturnRealTimeApiMetrics() {
            // Given
            ApiPerformanceMetricsDto realTimeMetrics = ApiPerformanceMetricsDto.builder()
                    .totalRequests(500L)
                    .avgResponseTimeMs(42.0)
                    .build();

            when(apiPerformanceCollector.collectRealTime()).thenReturn(realTimeMetrics);

            // When
            ApiPerformanceMetricsDto result = technicalAnalyticsService.getApiPerformanceRealTime();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalRequests()).isEqualTo(500L);

            verify(apiPerformanceCollector).collectRealTime();
        }
    }

    @Nested
    @DisplayName("Backend Resource Metrics Tests")
    class BackendResourceMetricsTests {

        @Test
        @DisplayName("Should collect backend resource metrics successfully")
        void shouldCollectBackendResourceMetrics() {
            // Given
            BackendResourceMetricsDto expectedMetrics = BackendResourceMetricsDto.builder()
                    .cpu(BackendResourceMetricsDto.CpuMetricsDto.builder()
                            .systemCpuUsage(0.45)
                            .processCpuUsage(0.30)
                            .availableProcessors(8)
                            .status(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                            .build())
                    .memory(BackendResourceMetricsDto.MemoryMetricsDto.builder()
                            .totalMemoryBytes(8_000_000_000L)
                            .usedMemoryBytes(4_000_000_000L)
                            .usagePercentage(0.50)
                            .status(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                            .build())
                    .jvm(BackendResourceMetricsDto.JvmMetricsDto.builder()
                            .heapUsedBytes(2_000_000_000L)
                            .heapMaxBytes(4_000_000_000L)
                            .heapUsagePercentage(0.50)
                            .threadCount(150)
                            .status(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                            .build())
                    .databasePool(BackendResourceMetricsDto.DatabasePoolMetricsDto.builder()
                            .activeConnections(10)
                            .maxPoolSize(50)
                            .usagePercentage(0.20)
                            .status(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                            .build())
                    .redis(BackendResourceMetricsDto.RedisMetricsDto.builder()
                            .isConnected(true)
                            .pingLatencyMs(2L)
                            .status(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                            .build())
                    .messageQueue(BackendResourceMetricsDto.MessageQueueMetricsDto.builder()
                            .totalQueueDepth(100L)
                            .status(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                            .build())
                    .overallStatus(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                    .collectedAt(LocalDateTime.now())
                    .build();

            when(backendResourceCollector.collect()).thenReturn(expectedMetrics);

            // When
            BackendResourceMetricsDto result = technicalAnalyticsService.getBackendResourceMetrics();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCpu().getSystemCpuUsage()).isEqualTo(0.45);
            assertThat(result.getMemory().getUsagePercentage()).isEqualTo(0.50);
            assertThat(result.getDatabasePool().getActiveConnections()).isEqualTo(10);
            assertThat(result.getRedis().getIsConnected()).isTrue();
            assertThat(result.getOverallStatus()).isEqualTo(BackendResourceMetricsDto.HealthStatus.HEALTHY);

            verify(backendResourceCollector).collect();
        }

        @Test
        @DisplayName("Should handle degraded backend status")
        void shouldHandleDegradedBackendStatus() {
            // Given
            BackendResourceMetricsDto degradedMetrics = BackendResourceMetricsDto.builder()
                    .cpu(BackendResourceMetricsDto.CpuMetricsDto.builder()
                            .systemCpuUsage(0.85)
                            .status(BackendResourceMetricsDto.HealthStatus.DEGRADED)
                            .build())
                    .memory(BackendResourceMetricsDto.MemoryMetricsDto.builder()
                            .usagePercentage(0.50)
                            .status(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                            .build())
                    .jvm(BackendResourceMetricsDto.JvmMetricsDto.builder()
                            .heapUsagePercentage(0.50)
                            .status(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                            .build())
                    .databasePool(BackendResourceMetricsDto.DatabasePoolMetricsDto.builder()
                            .status(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                            .build())
                    .redis(BackendResourceMetricsDto.RedisMetricsDto.builder()
                            .isConnected(true)
                            .status(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                            .build())
                    .messageQueue(BackendResourceMetricsDto.MessageQueueMetricsDto.builder()
                            .status(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                            .build())
                    .overallStatus(BackendResourceMetricsDto.HealthStatus.DEGRADED)
                    .build();

            when(backendResourceCollector.collect()).thenReturn(degradedMetrics);

            // When
            BackendResourceMetricsDto result = technicalAnalyticsService.getBackendResourceMetrics();

            // Then
            assertThat(result.getCpu().getStatus()).isEqualTo(BackendResourceMetricsDto.HealthStatus.DEGRADED);
            assertThat(result.getOverallStatus()).isEqualTo(BackendResourceMetricsDto.HealthStatus.DEGRADED);
        }
    }

    @Nested
    @DisplayName("Database Performance Metrics Tests")
    class DatabasePerformanceMetricsTests {

        @Test
        @DisplayName("Should collect database performance metrics successfully")
        void shouldCollectDatabasePerformanceMetrics() {
            // Given
            DatabasePerformanceMetricsDto expectedMetrics = DatabasePerformanceMetricsDto.builder()
                    .slowQueriesSummary(DatabasePerformanceMetricsDto.SlowQueriesSummaryDto.builder()
                            .totalSlowQueries(25L)
                            .avgDurationMs(1500.0)
                            .maxDurationMs(5000L)
                            .slowQueryThresholdMs(1000L)
                            .build())
                    .indexUsage(DatabasePerformanceMetricsDto.IndexUsageDto.builder()
                            .indexHitRate(0.98)
                            .totalIndexScans(100000L)
                            .totalSeqScans(500L)
                            .unusedIndexesCount(3)
                            .build())
                    .storageStats(DatabasePerformanceMetricsDto.StorageStatsDto.builder()
                            .totalDatabaseSizeBytes(50_000_000_000L)
                            .deadTuplePercentage(0.02)
                            .build())
                    .backupSummary(DatabasePerformanceMetricsDto.BackupSummaryDto.builder()
                            .totalBackups(7L)
                            .successfulBackups(7L)
                            .failedBackups(0L)
                            .successRate(1.0)
                            .build())
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(databasePerformanceCollector.collect(any(), any(), anyLong(), anyInt(), anyBoolean()))
                    .thenReturn(expectedMetrics);

            // When
            DatabasePerformanceMetricsDto result = technicalAnalyticsService.getDatabasePerformanceMetrics(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSlowQueriesSummary().getTotalSlowQueries()).isEqualTo(25L);
            assertThat(result.getIndexUsage().getIndexHitRate()).isEqualTo(0.98);
            assertThat(result.getBackupSummary().getSuccessRate()).isEqualTo(1.0);

            verify(databasePerformanceCollector).collect(startDate, endDate, 1000L, 10, false);
        }
    }

    @Nested
    @DisplayName("WebSocket Metrics Tests")
    class WebSocketMetricsTests {

        @Test
        @DisplayName("Should collect WebSocket metrics successfully")
        void shouldCollectWebSocketMetrics() {
            // Given
            Map<String, Long> connectionsByType = new HashMap<>();
            connectionsByType.put("COURIER", 150L);
            connectionsByType.put("CONSUMER", 500L);
            connectionsByType.put("RESTAURANT", 80L);

            WebSocketMetricsDto expectedMetrics = WebSocketMetricsDto.builder()
                    .totalActiveConnections(730L)
                    .connectedCouriers(150L)
                    .connectedConsumers(500L)
                    .connectedRestaurants(80L)
                    .connectionsByUserType(connectionsByType)
                    .messageDelivery(WebSocketMetricsDto.MessageDeliveryMetricsDto.builder()
                            .totalMessagesSent(50000L)
                            .totalMessagesDelivered(49950L)
                            .deliverySuccessRate(0.999)
                            .avgDeliveryDelayMs(15.5)
                            .p99DeliveryDelayMs(85L)
                            .build())
                    .droppedConnections(WebSocketMetricsDto.DroppedConnectionsDto.builder()
                            .totalDropped(20L)
                            .droppedRate(0.003)
                            .build())
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(webSocketCollector.collect(any(), any(), anyBoolean())).thenReturn(expectedMetrics);

            // When
            WebSocketMetricsDto result = technicalAnalyticsService.getWebSocketMetrics(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalActiveConnections()).isEqualTo(730L);
            assertThat(result.getConnectedCouriers()).isEqualTo(150L);
            assertThat(result.getMessageDelivery().getDeliverySuccessRate()).isEqualTo(0.999);

            verify(webSocketCollector).collect(startDate, endDate, true);
        }

        @Test
        @DisplayName("Should return real-time WebSocket metrics")
        void shouldReturnRealTimeWebSocketMetrics() {
            // Given
            WebSocketMetricsDto realTimeMetrics = WebSocketMetricsDto.builder()
                    .totalActiveConnections(800L)
                    .connectedCouriers(180L)
                    .connectedConsumers(550L)
                    .build();

            when(webSocketCollector.collectRealTime()).thenReturn(realTimeMetrics);

            // When
            WebSocketMetricsDto result = technicalAnalyticsService.getWebSocketMetricsRealTime();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalActiveConnections()).isEqualTo(800L);

            verify(webSocketCollector).collectRealTime();
        }
    }

    @Nested
    @DisplayName("Storage Metrics Tests")
    class StorageMetricsTests {

        @Test
        @DisplayName("Should collect storage metrics successfully")
        void shouldCollectStorageMetrics() {
            // Given
            StorageMetricsDto expectedMetrics = StorageMetricsDto.builder()
                    .totalFileCount(50000L)
                    .totalStorageSizeBytes(10_000_000_000L)
                    .totalStorageSizeMb(9536.74)
                    .totalStorageSizeGb(9.31)
                    .imageMetrics(StorageMetricsDto.ImageMetricsDto.builder()
                            .totalImageCount(45000L)
                            .totalImageSizeBytes(8_000_000_000L)
                            .avgImageSizeBytes(177777.78)
                            .imageStoragePercentage(80.0)
                            .build())
                    .uploadPerformance(StorageMetricsDto.UploadPerformanceDto.builder()
                            .totalUploads(1000L)
                            .successfulUploads(995L)
                            .failedUploads(5L)
                            .successRate(0.995)
                            .avgUploadLatencyMs(350.0)
                            .p90UploadLatencyMs(800L)
                            .build())
                    .storageGrowth(StorageMetricsDto.StorageGrowthDto.builder()
                            .growthBytes(500_000_000L)
                            .growthPercentage(5.0)
                            .build())
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(storageCollector.collect(any(), any(), anyBoolean())).thenReturn(expectedMetrics);

            // When
            StorageMetricsDto result = technicalAnalyticsService.getStorageMetrics(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalFileCount()).isEqualTo(50000L);
            assertThat(result.getUploadPerformance().getSuccessRate()).isEqualTo(0.995);

            verify(storageCollector).collect(startDate, endDate, true);
        }
    }

    @Nested
    @DisplayName("Health Score Calculation Tests")
    class HealthScoreCalculationTests {

        @Test
        @DisplayName("Should calculate API health score - healthy")
        void shouldCalculateApiHealthScoreHealthy() {
            // Given
            ApiPerformanceMetricsDto metrics = ApiPerformanceMetricsDto.builder()
                    .errorRate(0.001)
                    .timeoutRate(0.0001)
                    .responseTimePercentiles(ApiPerformanceMetricsDto.ResponseTimePercentilesDto.builder()
                            .p99(200L)
                            .build())
                    .build();

            // When
            Integer score = technicalAnalyticsService.calculateApiHealthScore(metrics);

            // Then
            assertThat(score).isGreaterThanOrEqualTo(90);
        }

        @Test
        @DisplayName("Should calculate API health score - degraded with high error rate")
        void shouldCalculateApiHealthScoreDegraded() {
            // Given
            ApiPerformanceMetricsDto metrics = ApiPerformanceMetricsDto.builder()
                    .errorRate(0.03) // 3% error rate
                    .timeoutRate(0.001)
                    .responseTimePercentiles(ApiPerformanceMetricsDto.ResponseTimePercentilesDto.builder()
                            .p99(1500L) // 1.5s p99
                            .build())
                    .build();

            // When
            Integer score = technicalAnalyticsService.calculateApiHealthScore(metrics);

            // Then
            assertThat(score).isLessThan(75);
        }

        @Test
        @DisplayName("Should calculate API health score - critical")
        void shouldCalculateApiHealthScoreCritical() {
            // Given
            ApiPerformanceMetricsDto metrics = ApiPerformanceMetricsDto.builder()
                    .errorRate(0.08) // 8% error rate
                    .timeoutRate(0.02) // 2% timeout
                    .responseTimePercentiles(ApiPerformanceMetricsDto.ResponseTimePercentilesDto.builder()
                            .p99(6000L) // 6s p99
                            .build())
                    .build();

            // When
            Integer score = technicalAnalyticsService.calculateApiHealthScore(metrics);

            // Then
            assertThat(score).isLessThan(50);
        }

        @Test
        @DisplayName("Should calculate backend health score - healthy")
        void shouldCalculateBackendHealthScoreHealthy() {
            // Given
            BackendResourceMetricsDto metrics = BackendResourceMetricsDto.builder()
                    .cpu(BackendResourceMetricsDto.CpuMetricsDto.builder()
                            .systemCpuUsage(0.40)
                            .build())
                    .memory(BackendResourceMetricsDto.MemoryMetricsDto.builder()
                            .usagePercentage(0.50)
                            .build())
                    .jvm(BackendResourceMetricsDto.JvmMetricsDto.builder()
                            .heapUsagePercentage(0.60)
                            .build())
                    .databasePool(BackendResourceMetricsDto.DatabasePoolMetricsDto.builder()
                            .usagePercentage(0.30)
                            .build())
                    .redis(BackendResourceMetricsDto.RedisMetricsDto.builder()
                            .isConnected(true)
                            .pingLatencyMs(5L)
                            .build())
                    .build();

            // When
            Integer score = technicalAnalyticsService.calculateBackendHealthScore(metrics);

            // Then
            assertThat(score).isGreaterThanOrEqualTo(90);
        }

        @Test
        @DisplayName("Should calculate database health score")
        void shouldCalculateDatabaseHealthScore() {
            // Given
            DatabasePerformanceMetricsDto metrics = DatabasePerformanceMetricsDto.builder()
                    .indexUsage(DatabasePerformanceMetricsDto.IndexUsageDto.builder()
                            .indexHitRate(0.98)
                            .build())
                    .backupSummary(DatabasePerformanceMetricsDto.BackupSummaryDto.builder()
                            .successRate(1.0)
                            .build())
                    .slowQueriesSummary(DatabasePerformanceMetricsDto.SlowQueriesSummaryDto.builder()
                            .totalSlowQueries(5L)
                            .build())
                    .storageStats(DatabasePerformanceMetricsDto.StorageStatsDto.builder()
                            .deadTuplePercentage(0.02)
                            .build())
                    .build();

            // When
            Integer score = technicalAnalyticsService.calculateDatabaseHealthScore(metrics);

            // Then
            assertThat(score).isGreaterThanOrEqualTo(90);
        }

        @Test
        @DisplayName("Should calculate WebSocket health score")
        void shouldCalculateWebSocketHealthScore() {
            // Given
            WebSocketMetricsDto metrics = WebSocketMetricsDto.builder()
                    .messageDelivery(WebSocketMetricsDto.MessageDeliveryMetricsDto.builder()
                            .deliverySuccessRate(0.999)
                            .p99DeliveryDelayMs(50L)
                            .build())
                    .droppedConnections(WebSocketMetricsDto.DroppedConnectionsDto.builder()
                            .droppedRate(0.002)
                            .build())
                    .build();

            // When
            Integer score = technicalAnalyticsService.calculateWebSocketHealthScore(metrics);

            // Then
            assertThat(score).isGreaterThanOrEqualTo(90);
        }

        @Test
        @DisplayName("Should calculate storage health score")
        void shouldCalculateStorageHealthScore() {
            // Given
            StorageMetricsDto metrics = StorageMetricsDto.builder()
                    .uploadPerformance(StorageMetricsDto.UploadPerformanceDto.builder()
                            .successRate(0.998)
                            .p90UploadLatencyMs(500L)
                            .build())
                    .storageGrowth(StorageMetricsDto.StorageGrowthDto.builder()
                            .growthPercentage(3.0)
                            .build())
                    .build();

            // When
            Integer score = technicalAnalyticsService.calculateStorageHealthScore(metrics);

            // Then
            assertThat(score).isGreaterThanOrEqualTo(90);
        }
    }

    @Nested
    @DisplayName("Technical Summary Tests")
    class TechnicalSummaryTests {

        @Test
        @DisplayName("Should generate technical summary successfully")
        void shouldGenerateTechnicalSummary() {
            // Given
            setupAllCollectorMocks();

            // When
            TechnicalSummaryDto result = technicalAnalyticsService.getTechnicalSummary(startDate, endDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOverallHealth()).isNotNull();
            assertThat(result.getApiHealthScore()).isNotNull();
            assertThat(result.getBackendHealthScore()).isNotNull();
            assertThat(result.getDatabaseHealthScore()).isNotNull();
            assertThat(result.getWebsocketHealthScore()).isNotNull();
            assertThat(result.getStorageHealthScore()).isNotNull();
            assertThat(result.getGeneratedAt()).isNotNull();
        }

        private void setupAllCollectorMocks() {
            // API metrics
            when(apiPerformanceCollector.collect(any(), any(), anyInt(), anyBoolean()))
                    .thenReturn(ApiPerformanceMetricsDto.builder()
                            .requestsPerMinute(100.0)
                            .errorRate(0.01)
                            .timeoutRate(0.001)
                            .responseTimePercentiles(ApiPerformanceMetricsDto.ResponseTimePercentilesDto.builder()
                                    .p99(200L).build())
                            .status(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                            .build());

            // Backend metrics
            when(backendResourceCollector.collect())
                    .thenReturn(BackendResourceMetricsDto.builder()
                            .cpu(BackendResourceMetricsDto.CpuMetricsDto.builder()
                                    .systemCpuUsage(0.5).build())
                            .memory(BackendResourceMetricsDto.MemoryMetricsDto.builder()
                                    .usagePercentage(0.5).build())
                            .jvm(BackendResourceMetricsDto.JvmMetricsDto.builder()
                                    .heapUsagePercentage(0.5).build())
                            .databasePool(BackendResourceMetricsDto.DatabasePoolMetricsDto.builder()
                                    .activeConnections(10).build())
                            .redis(BackendResourceMetricsDto.RedisMetricsDto.builder()
                                    .isConnected(true).pingLatencyMs(5L).build())
                            .messageQueue(BackendResourceMetricsDto.MessageQueueMetricsDto.builder()
                                    .totalQueueDepth(100L).build())
                            .overallStatus(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                            .build());

            // Database metrics
            when(databasePerformanceCollector.collect(any(), any(), anyLong(), anyInt(), anyBoolean()))
                    .thenReturn(DatabasePerformanceMetricsDto.builder()
                            .slowQueriesSummary(DatabasePerformanceMetricsDto.SlowQueriesSummaryDto.builder()
                                    .totalSlowQueries(10L).build())
                            .indexUsage(DatabasePerformanceMetricsDto.IndexUsageDto.builder()
                                    .indexHitRate(0.98).build())
                            .storageStats(DatabasePerformanceMetricsDto.StorageStatsDto.builder()
                                    .totalDatabaseSizeBytes(1000000L).build())
                            .backupSummary(DatabasePerformanceMetricsDto.BackupSummaryDto.builder()
                                    .successRate(1.0).build())
                            .build());

            when(databasePerformanceCollector.calculateHealthStatus(any()))
                    .thenReturn(BackendResourceMetricsDto.HealthStatus.HEALTHY);

            // WebSocket metrics
            when(webSocketCollector.collect(any(), any(), anyBoolean()))
                    .thenReturn(WebSocketMetricsDto.builder()
                            .totalActiveConnections(500L)
                            .connectedCouriers(100L)
                            .connectedConsumers(350L)
                            .messageDelivery(WebSocketMetricsDto.MessageDeliveryMetricsDto.builder()
                                    .deliverySuccessRate(0.999)
                                    .p99DeliveryDelayMs(50L).build())
                            .droppedConnections(WebSocketMetricsDto.DroppedConnectionsDto.builder()
                                    .droppedRate(0.001).build())
                            .build());

            when(webSocketCollector.calculateHealthStatus(any()))
                    .thenReturn(BackendResourceMetricsDto.HealthStatus.HEALTHY);

            // Storage metrics
            when(storageCollector.collect(any(), any(), anyBoolean()))
                    .thenReturn(StorageMetricsDto.builder()
                            .totalFileCount(10000L)
                            .totalStorageSizeGb(5.0)
                            .uploadPerformance(StorageMetricsDto.UploadPerformanceDto.builder()
                                    .successRate(0.998)
                                    .failedUploads(2L)
                                    .p90UploadLatencyMs(500L).build())
                            .storageGrowth(StorageMetricsDto.StorageGrowthDto.builder()
                                    .growthPercentage(2.0).build())
                            .build());

            when(storageCollector.calculateHealthStatus(any()))
                    .thenReturn(BackendResourceMetricsDto.HealthStatus.HEALTHY);
        }
    }

    @Nested
    @DisplayName("Null Handling Tests")
    class NullHandlingTests {

        @Test
        @DisplayName("Should handle null API metrics gracefully")
        void shouldHandleNullApiMetrics() {
            // When
            Integer score = technicalAnalyticsService.calculateApiHealthScore(null);

            // Then
            assertThat(score).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle null backend metrics gracefully")
        void shouldHandleNullBackendMetrics() {
            // When
            Integer score = technicalAnalyticsService.calculateBackendHealthScore(null);

            // Then
            assertThat(score).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle null database metrics gracefully")
        void shouldHandleNullDatabaseMetrics() {
            // When
            Integer score = technicalAnalyticsService.calculateDatabaseHealthScore(null);

            // Then
            assertThat(score).isEqualTo(0);
        }
    }
}
