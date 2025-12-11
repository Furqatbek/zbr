package com.fooddelivery.analytics.technical.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fooddelivery.analytics.technical.dto.*;
import com.fooddelivery.analytics.technical.service.TechnicalAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TechnicalAnalyticsController.
 */
@WebMvcTest(TechnicalAnalyticsController.class)
class TechnicalAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TechnicalAnalyticsService technicalAnalyticsService;

    private ObjectMapper objectMapper;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        endDate = LocalDateTime.of(2024, 1, 1, 23, 59);
    }

    @Test
    @DisplayName("Should return technical summary")
    void shouldReturnTechnicalSummary() throws Exception {
        // Given
        TechnicalSummaryDto summary = TechnicalSummaryDto.builder()
                .overallHealth(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                .apiHealthScore(95)
                .backendHealthScore(90)
                .databaseHealthScore(92)
                .websocketHealthScore(98)
                .storageHealthScore(96)
                .generatedAt(LocalDateTime.now())
                .build();

        when(technicalAnalyticsService.getTechnicalSummary(any(), any())).thenReturn(summary);

        // When & Then
        mockMvc.perform(get("/api/v1/analytics/technical/summary")
                        .param("startDate", "2024-01-01T00:00:00")
                        .param("endDate", "2024-01-01T23:59:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallHealth").value("HEALTHY"))
                .andExpect(jsonPath("$.apiHealthScore").value(95))
                .andExpect(jsonPath("$.backendHealthScore").value(90));

        verify(technicalAnalyticsService).getTechnicalSummary(any(), any());
    }

    @Test
    @DisplayName("Should return API performance metrics")
    void shouldReturnApiPerformanceMetrics() throws Exception {
        // Given
        ApiPerformanceMetricsDto metrics = ApiPerformanceMetricsDto.builder()
                .totalRequests(10000L)
                .requestsPerMinute(166.67)
                .errorRate(0.005)
                .status(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                .build();

        when(technicalAnalyticsService.getApiPerformanceMetrics(any())).thenReturn(metrics);

        TechnicalMetricsRequest request = TechnicalMetricsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/analytics/technical/api-performance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(10000))
                .andExpect(jsonPath("$.errorRate").value(0.005))
                .andExpect(jsonPath("$.status").value("HEALTHY"));

        verify(technicalAnalyticsService).getApiPerformanceMetrics(any());
    }

    @Test
    @DisplayName("Should return real-time API metrics")
    void shouldReturnRealTimeApiMetrics() throws Exception {
        // Given
        ApiPerformanceMetricsDto metrics = ApiPerformanceMetricsDto.builder()
                .totalRequests(500L)
                .avgResponseTimeMs(42.0)
                .build();

        when(technicalAnalyticsService.getApiPerformanceRealTime()).thenReturn(metrics);

        // When & Then
        mockMvc.perform(get("/api/v1/analytics/technical/api-performance/realtime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(500));

        verify(technicalAnalyticsService).getApiPerformanceRealTime();
    }

    @Test
    @DisplayName("Should return backend resource metrics")
    void shouldReturnBackendResourceMetrics() throws Exception {
        // Given
        BackendResourceMetricsDto metrics = BackendResourceMetricsDto.builder()
                .cpu(BackendResourceMetricsDto.CpuMetricsDto.builder()
                        .systemCpuUsage(0.45)
                        .status(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                        .build())
                .memory(BackendResourceMetricsDto.MemoryMetricsDto.builder()
                        .usagePercentage(0.50)
                        .status(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                        .build())
                .overallStatus(BackendResourceMetricsDto.HealthStatus.HEALTHY)
                .build();

        when(technicalAnalyticsService.getBackendResourceMetrics()).thenReturn(metrics);

        // When & Then
        mockMvc.perform(get("/api/v1/analytics/technical/backend-resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpu.systemCpuUsage").value(0.45))
                .andExpect(jsonPath("$.overallStatus").value("HEALTHY"));

        verify(technicalAnalyticsService).getBackendResourceMetrics();
    }

    @Test
    @DisplayName("Should return database performance metrics")
    void shouldReturnDatabasePerformanceMetrics() throws Exception {
        // Given
        DatabasePerformanceMetricsDto metrics = DatabasePerformanceMetricsDto.builder()
                .slowQueriesSummary(DatabasePerformanceMetricsDto.SlowQueriesSummaryDto.builder()
                        .totalSlowQueries(25L)
                        .avgDurationMs(1500.0)
                        .build())
                .indexUsage(DatabasePerformanceMetricsDto.IndexUsageDto.builder()
                        .indexHitRate(0.98)
                        .build())
                .build();

        when(technicalAnalyticsService.getDatabasePerformanceMetrics(any())).thenReturn(metrics);

        TechnicalMetricsRequest request = TechnicalMetricsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/analytics/technical/database-performance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slowQueriesSummary.totalSlowQueries").value(25))
                .andExpect(jsonPath("$.indexUsage.indexHitRate").value(0.98));

        verify(technicalAnalyticsService).getDatabasePerformanceMetrics(any());
    }

    @Test
    @DisplayName("Should return WebSocket metrics")
    void shouldReturnWebSocketMetrics() throws Exception {
        // Given
        WebSocketMetricsDto metrics = WebSocketMetricsDto.builder()
                .totalActiveConnections(730L)
                .connectedCouriers(150L)
                .connectedConsumers(500L)
                .messageDelivery(WebSocketMetricsDto.MessageDeliveryMetricsDto.builder()
                        .deliverySuccessRate(0.999)
                        .build())
                .build();

        when(technicalAnalyticsService.getWebSocketMetrics(any())).thenReturn(metrics);

        TechnicalMetricsRequest request = TechnicalMetricsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/analytics/technical/websocket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActiveConnections").value(730))
                .andExpect(jsonPath("$.connectedCouriers").value(150));

        verify(technicalAnalyticsService).getWebSocketMetrics(any());
    }

    @Test
    @DisplayName("Should return storage metrics")
    void shouldReturnStorageMetrics() throws Exception {
        // Given
        StorageMetricsDto metrics = StorageMetricsDto.builder()
                .totalFileCount(50000L)
                .totalStorageSizeGb(9.31)
                .uploadPerformance(StorageMetricsDto.UploadPerformanceDto.builder()
                        .successRate(0.995)
                        .build())
                .build();

        when(technicalAnalyticsService.getStorageMetrics(any())).thenReturn(metrics);

        TechnicalMetricsRequest request = TechnicalMetricsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/analytics/technical/storage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFileCount").value(50000))
                .andExpect(jsonPath("$.totalStorageSizeGb").value(9.31));

        verify(technicalAnalyticsService).getStorageMetrics(any());
    }

    @Test
    @DisplayName("Should return health scores")
    void shouldReturnHealthScores() throws Exception {
        // Given
        when(technicalAnalyticsService.getApiPerformanceMetrics(any()))
                .thenReturn(ApiPerformanceMetricsDto.builder().build());
        when(technicalAnalyticsService.getBackendResourceMetrics())
                .thenReturn(BackendResourceMetricsDto.builder().build());
        when(technicalAnalyticsService.getDatabasePerformanceMetrics(any()))
                .thenReturn(DatabasePerformanceMetricsDto.builder().build());
        when(technicalAnalyticsService.getWebSocketMetrics(any()))
                .thenReturn(WebSocketMetricsDto.builder().build());
        when(technicalAnalyticsService.getStorageMetrics(any()))
                .thenReturn(StorageMetricsDto.builder().build());

        when(technicalAnalyticsService.calculateApiHealthScore(any())).thenReturn(95);
        when(technicalAnalyticsService.calculateBackendHealthScore(any())).thenReturn(90);
        when(technicalAnalyticsService.calculateDatabaseHealthScore(any())).thenReturn(92);
        when(technicalAnalyticsService.calculateWebSocketHealthScore(any())).thenReturn(98);
        when(technicalAnalyticsService.calculateStorageHealthScore(any())).thenReturn(96);

        // When & Then
        mockMvc.perform(get("/api/v1/analytics/technical/health-scores")
                        .param("startDate", "2024-01-01T00:00:00")
                        .param("endDate", "2024-01-01T23:59:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiHealthScore").value(95))
                .andExpect(jsonPath("$.backendHealthScore").value(90))
                .andExpect(jsonPath("$.databaseHealthScore").value(92))
                .andExpect(jsonPath("$.websocketHealthScore").value(98))
                .andExpect(jsonPath("$.storageHealthScore").value(96));
    }

    @Test
    @DisplayName("Should refresh metrics cache")
    void shouldRefreshMetricsCache() throws Exception {
        // Given
        doNothing().when(technicalAnalyticsService).refreshAllMetrics();

        // When & Then
        mockMvc.perform(post("/api/v1/analytics/technical/refresh"))
                .andExpect(status().isOk());

        verify(technicalAnalyticsService).refreshAllMetrics();
    }

    @Test
    @DisplayName("Should return bad request for invalid date format")
    void shouldReturnBadRequestForInvalidDateFormat() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/analytics/technical/summary")
                        .param("startDate", "invalid-date")
                        .param("endDate", "2024-01-01T23:59:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return bad request for missing required date")
    void shouldReturnBadRequestForMissingDate() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/analytics/technical/summary")
                        .param("startDate", "2024-01-01T00:00:00"))
                .andExpect(status().isBadRequest());
    }
}
