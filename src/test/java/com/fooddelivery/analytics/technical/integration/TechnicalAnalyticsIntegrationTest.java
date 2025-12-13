package com.fooddelivery.analytics.technical.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fooddelivery.analytics.technical.dto.*;
import com.fooddelivery.analytics.technical.model.*;
import com.fooddelivery.analytics.technical.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Technical Analytics using Testcontainers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TechnicalAnalyticsIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("fooddelivery_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HttpRequestLogRepository httpRequestLogRepository;

    @Autowired
    private SlowQueryLogRepository slowQueryLogRepository;

    @Autowired
    private WebSocketConnectionLogRepository wsConnectionLogRepository;

    @Autowired
    private WebSocketMessageLogRepository wsMessageLogRepository;

    @Autowired
    private StorageMetadataRepository storageMetadataRepository;

    @Autowired
    private BackupLogRepository backupLogRepository;

    private ObjectMapper objectMapper;
    private LocalDateTime testStart;
    private LocalDateTime testEnd;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testStart = LocalDateTime.now().minusHours(1);
        testEnd = LocalDateTime.now();
    }

    @Test
    @Order(1)
    @DisplayName("Should seed test data and retrieve API performance metrics")
    void shouldRetrieveApiPerformanceMetrics() throws Exception {
        // Seed HTTP request logs
        seedHttpRequestLogs();

        TechnicalMetricsRequest request = TechnicalMetricsRequest.builder()
                .startDate(testStart)
                .endDate(testEnd)
                .topEndpointsLimit(10)
                .includeHourlyDistribution(true)
                .build();

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/v1/analytics/technical/api-performance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ApiPerformanceMetricsDto metrics = objectMapper.readValue(responseBody, ApiPerformanceMetricsDto.class);

        assertThat(metrics.getTotalRequests()).isGreaterThan(0);
    }

    @Test
    @Order(2)
    @DisplayName("Should retrieve backend resource metrics")
    void shouldRetrieveBackendResourceMetrics() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/api/v1/analytics/technical/backend-resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpu").exists())
                .andExpect(jsonPath("$.memory").exists())
                .andExpect(jsonPath("$.jvm").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        BackendResourceMetricsDto metrics = objectMapper.readValue(responseBody, BackendResourceMetricsDto.class);

        assertThat(metrics.getCpu()).isNotNull();
        assertThat(metrics.getMemory()).isNotNull();
        assertThat(metrics.getJvm()).isNotNull();
    }

    @Test
    @Order(3)
    @DisplayName("Should seed and retrieve database performance metrics")
    void shouldRetrieveDatabasePerformanceMetrics() throws Exception {
        // Seed slow query logs and backup logs
        seedSlowQueryLogs();
        seedBackupLogs();

        TechnicalMetricsRequest request = TechnicalMetricsRequest.builder()
                .startDate(testStart)
                .endDate(testEnd)
                .slowQueryThresholdMs(500L)
                .topSlowQueriesLimit(10)
                .includeTableStats(false)
                .build();

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/v1/analytics/technical/database-performance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slowQueriesSummary").exists())
                .andExpect(jsonPath("$.backupSummary").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        DatabasePerformanceMetricsDto metrics = objectMapper.readValue(responseBody, DatabasePerformanceMetricsDto.class);

        assertThat(metrics.getSlowQueriesSummary()).isNotNull();
        assertThat(metrics.getBackupSummary()).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("Should seed and retrieve WebSocket metrics")
    void shouldRetrieveWebSocketMetrics() throws Exception {
        // Seed WebSocket connection and message logs
        seedWebSocketLogs();

        TechnicalMetricsRequest request = TechnicalMetricsRequest.builder()
                .startDate(testStart)
                .endDate(testEnd)
                .includeHourlyDistribution(true)
                .build();

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/v1/analytics/technical/websocket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActiveConnections").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        WebSocketMetricsDto metrics = objectMapper.readValue(responseBody, WebSocketMetricsDto.class);

        assertThat(metrics).isNotNull();
    }

    @Test
    @Order(5)
    @DisplayName("Should seed and retrieve storage metrics")
    void shouldRetrieveStorageMetrics() throws Exception {
        // Seed storage metadata
        seedStorageMetadata();

        TechnicalMetricsRequest request = TechnicalMetricsRequest.builder()
                .startDate(testStart)
                .endDate(testEnd)
                .includeDailyTrend(true)
                .build();

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/v1/analytics/technical/storage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFileCount").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        StorageMetricsDto metrics = objectMapper.readValue(responseBody, StorageMetricsDto.class);

        assertThat(metrics.getTotalFileCount()).isGreaterThan(0);
    }

    @Test
    @Order(6)
    @DisplayName("Should retrieve technical summary")
    void shouldRetrieveTechnicalSummary() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/api/v1/analytics/technical/summary")
                        .param("startDate", testStart.toString())
                        .param("endDate", testEnd.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallHealth").exists())
                .andExpect(jsonPath("$.apiHealthScore").exists())
                .andExpect(jsonPath("$.backendHealthScore").exists())
                .andExpect(jsonPath("$.databaseHealthScore").exists())
                .andExpect(jsonPath("$.websocketHealthScore").exists())
                .andExpect(jsonPath("$.storageHealthScore").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        TechnicalSummaryDto summary = objectMapper.readValue(responseBody, TechnicalSummaryDto.class);

        assertThat(summary.getOverallHealth()).isNotNull();
        assertThat(summary.getApiHealthScore()).isBetween(0, 100);
        assertThat(summary.getBackendHealthScore()).isBetween(0, 100);
    }

    @Test
    @Order(7)
    @DisplayName("Should retrieve health scores")
    void shouldRetrieveHealthScores() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/analytics/technical/health-scores")
                        .param("startDate", testStart.toString())
                        .param("endDate", testEnd.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiHealthScore").isNumber())
                .andExpect(jsonPath("$.backendHealthScore").isNumber())
                .andExpect(jsonPath("$.databaseHealthScore").isNumber())
                .andExpect(jsonPath("$.websocketHealthScore").isNumber())
                .andExpect(jsonPath("$.storageHealthScore").isNumber());
    }

    @Test
    @Order(8)
    @DisplayName("Should refresh metrics cache")
    void shouldRefreshMetricsCache() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/analytics/technical/refresh"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(9)
    @DisplayName("Should retrieve real-time WebSocket metrics")
    void shouldRetrieveRealTimeWebSocketMetrics() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/analytics/technical/websocket/realtime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActiveConnections").exists());
    }

    @Test
    @Order(10)
    @DisplayName("Should retrieve storage summary")
    void shouldRetrieveStorageSummary() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/analytics/technical/storage/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFileCount").exists())
                .andExpect(jsonPath("$.totalStorageSizeBytes").exists());
    }

    // Helper methods to seed test data

    private void seedHttpRequestLogs() {
        for (int i = 0; i < 100; i++) {
            HttpRequestLog log = HttpRequestLog.builder()
                    .requestId(UUID.randomUUID().toString())
                    .method("GET")
                    .endpoint("/api/v1/restaurants")
                    .statusCode(i < 95 ? 200 : (i < 98 ? 400 : 500))
                    .responseTimeMs(50L + (long) (Math.random() * 200))
                    .requestSizeBytes(100L)
                    .responseSizeBytes(5000L)
                    .clientIp("192.168.1." + (i % 256))
                    .isTimeout(false)
                    .timestamp(testStart.plusMinutes(i % 60))
                    .build();
            httpRequestLogRepository.save(log);
        }
    }

    private void seedSlowQueryLogs() {
        String[] queryTypes = {"SELECT", "UPDATE", "INSERT"};
        String[] tables = {"orders", "users", "restaurants"};

        for (int i = 0; i < 25; i++) {
            SlowQueryLog log = SlowQueryLog.builder()
                    .queryHash("hash_" + i)
                    .queryText("SELECT * FROM " + tables[i % 3] + " WHERE id = ?")
                    .queryType(SlowQueryLog.QueryType.valueOf(queryTypes[i % 3]))
                    .durationMs(1000L + (long) (Math.random() * 4000))
                    .rowsAffected((long) (Math.random() * 100))
                    .tableName(tables[i % 3])
                    .isUsingIndex(i % 4 != 0)
                    .timestamp(testStart.plusMinutes(i * 2))
                    .build();
            slowQueryLogRepository.save(log);
        }
    }

    private void seedBackupLogs() {
        for (int i = 0; i < 7; i++) {
            BackupLog log = BackupLog.builder()
                    .backupId(UUID.randomUUID().toString())
                    .databaseName("fooddelivery")
                    .backupType(i % 2 == 0 ? BackupLog.BackupType.FULL : BackupLog.BackupType.INCREMENTAL)
                    .status(i < 6 ? BackupLog.BackupStatus.COMPLETED : BackupLog.BackupStatus.FAILED)
                    .backupSizeBytes(1_000_000_000L + (long) (Math.random() * 500_000_000))
                    .startedAt(testStart.minusDays(6 - i))
                    .completedAt(i < 6 ? testStart.minusDays(6 - i).plusHours(1) : null)
                    .errorMessage(i >= 6 ? "Disk full" : null)
                    .build();
            backupLogRepository.save(log);
        }
    }

    private void seedWebSocketLogs() {
        WebSocketConnectionLog.UserType[] userTypes = WebSocketConnectionLog.UserType.values();

        for (int i = 0; i < 50; i++) {
            WebSocketConnectionLog connLog = WebSocketConnectionLog.builder()
                    .sessionId(UUID.randomUUID().toString())
                    .userId((long) (i + 1))
                    .userType(userTypes[i % userTypes.length])
                    .clientIp("192.168.1." + (i % 256))
                    .connectedAt(testStart.plusMinutes(i))
                    .disconnectedAt(i < 40 ? testStart.plusMinutes(i + 30) : null)
                    .isGracefulDisconnect(i < 45)
                    .messagesSent((long) (Math.random() * 100))
                    .messagesReceived((long) (Math.random() * 50))
                    .build();
            wsConnectionLogRepository.save(connLog);
        }

        for (int i = 0; i < 200; i++) {
            WebSocketMessageLog msgLog = WebSocketMessageLog.builder()
                    .messageId(UUID.randomUUID().toString())
                    .sessionId("session_" + (i % 50))
                    .messageType(i % 3 == 0 ? "LOCATION_UPDATE" : (i % 3 == 1 ? "ORDER_STATUS" : "CHAT"))
                    .payloadSizeBytes(100 + (int) (Math.random() * 900))
                    .publishedAt(testStart.plusMinutes(i % 60))
                    .deliveredAt(i < 195 ? testStart.plusMinutes(i % 60).plusNanos((long) (Math.random() * 100_000_000)) : null)
                    .isDelivered(i < 195)
                    .deliveryAttempts(i < 190 ? 1 : 2)
                    .failureReason(i >= 195 ? "Connection closed" : null)
                    .build();
            wsMessageLogRepository.save(msgLog);
        }
    }

    private void seedStorageMetadata() {
        String[] contentTypes = {"image/jpeg", "image/png", "image/webp", "application/pdf"};
        String[] entityTypes = {"RESTAURANT", "MENU_ITEM", "USER", "ORDER"};

        for (int i = 0; i < 100; i++) {
            StorageMetadata metadata = StorageMetadata.builder()
                    .fileKey("uploads/" + UUID.randomUUID() + ".jpg")
                    .originalFilename("image_" + i + ".jpg")
                    .contentType(contentTypes[i % contentTypes.length])
                    .fileSizeBytes(100_000L + (long) (Math.random() * 5_000_000))
                    .storageType(StorageMetadata.StorageType.S3)
                    .bucketName("food-delivery-assets")
                    .entityType(entityTypes[i % entityTypes.length])
                    .entityId((long) (i + 1))
                    .uploadedBy((long) ((i % 50) + 1))
                    .uploadDurationMs(200L + (long) (Math.random() * 2000))
                    .isUploadSuccessful(i < 97)
                    .errorMessage(i >= 97 ? "Timeout" : null)
                    .uploadedAt(testStart.plusMinutes(i % 60))
                    .build();
            storageMetadataRepository.save(metadata);
        }
    }
}
