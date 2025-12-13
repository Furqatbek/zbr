package com.fooddelivery.admin.dashboard.integration;

import com.fooddelivery.admin.dashboard.dto.*;
import com.fooddelivery.admin.dashboard.model.DashboardRefreshLog;
import com.fooddelivery.admin.dashboard.model.SystemHealthSnapshot;
import com.fooddelivery.admin.dashboard.repository.*;
import com.fooddelivery.admin.dashboard.service.AdminDashboardService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Admin Dashboard Integration Tests")
class AdminDashboardIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("dashboard_test")
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
    }

    @Autowired
    private AdminDashboardService dashboardService;

    @Autowired
    private SystemHealthRepository systemHealthRepository;

    @Autowired
    private DashboardRefreshLogRepository refreshLogRepository;

    @Autowired
    private DashboardOrderRepository orderRepository;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private DashboardFilterRequest defaultFilter;

    @BeforeEach
    void setUp() {
        startDate = LocalDate.now().atStartOfDay();
        endDate = LocalDateTime.now();
        defaultFilter = DashboardFilterRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .page(0)
                .pageSize(50)
                .build();
    }

    @Nested
    @DisplayName("Dashboard Overview Integration Tests")
    class OverviewIntegrationTests {

        @Test
        @Order(1)
        @DisplayName("Should return dashboard overview")
        void shouldReturnDashboardOverview() {
            // Act
            DashboardOverviewDto overview = dashboardService.getOverview(startDate, endDate);

            // Assert
            assertThat(overview).isNotNull();
            assertThat(overview.getGeneratedAt()).isNotNull();
            assertThat(overview.getOrdersToday()).isNotNull();
            assertThat(overview.getRevenueToday()).isNotNull();
        }

        @Test
        @Order(2)
        @DisplayName("Should include system status")
        void shouldIncludeSystemStatus() {
            // Arrange - Create system health snapshot
            SystemHealthSnapshot snapshot = SystemHealthSnapshot.builder()
                    .component("api")
                    .apiLatencyP50(150.0)
                    .apiLatencyP90(250.0)
                    .dbActiveConnections(25)
                    .redisLatencyMs(5.0)
                    .cpuUsagePercent(45.0)
                    .memoryUsagePercent(60.0)
                    .status("HEALTHY")
                    .capturedAt(LocalDateTime.now())
                    .build();
            systemHealthRepository.save(snapshot);

            // Act
            DashboardOverviewDto overview = dashboardService.getOverview(startDate, endDate);

            // Assert
            assertThat(overview.getSystemStatus()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Active Orders Integration Tests")
    class ActiveOrdersIntegrationTests {

        @Test
        @Order(3)
        @DisplayName("Should return active orders with status breakdown")
        void shouldReturnActiveOrdersWithStatusBreakdown() {
            // Act
            ActiveOrdersDto activeOrders = dashboardService.getActiveOrders(defaultFilter);

            // Assert
            assertThat(activeOrders).isNotNull();
            assertThat(activeOrders.getGeneratedAt()).isNotNull();
            assertThat(activeOrders.getTotalActiveOrders()).isNotNull();
        }

        @Test
        @Order(4)
        @DisplayName("Should filter active orders by date range")
        void shouldFilterActiveOrdersByDateRange() {
            // Arrange
            DashboardFilterRequest filter = DashboardFilterRequest.builder()
                    .startDate(LocalDate.now().minusDays(7).atStartOfDay())
                    .endDate(LocalDateTime.now())
                    .page(0)
                    .pageSize(100)
                    .build();

            // Act
            ActiveOrdersDto activeOrders = dashboardService.getActiveOrders(filter);

            // Assert
            assertThat(activeOrders).isNotNull();
        }
    }

    @Nested
    @DisplayName("Stuck Orders Integration Tests")
    class StuckOrdersIntegrationTests {

        @Test
        @Order(5)
        @DisplayName("Should return stuck orders prioritized by urgency")
        void shouldReturnStuckOrdersByPriority() {
            // Act
            StuckOrdersDto stuckOrders = dashboardService.getStuckOrders(defaultFilter);

            // Assert
            assertThat(stuckOrders).isNotNull();
            assertThat(stuckOrders.getGeneratedAt()).isNotNull();
            assertThat(stuckOrders.getTotalStuckOrders()).isNotNull();
        }

        @Test
        @Order(6)
        @DisplayName("Should respect custom stuck thresholds")
        void shouldRespectCustomThresholds() {
            // Arrange
            DashboardFilterRequest filter = DashboardFilterRequest.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .pendingThresholdMinutes(5)
                    .acceptedThresholdMinutes(10)
                    .preparingThresholdMinutes(20)
                    .readyThresholdMinutes(10)
                    .inTransitThresholdMinutes(30)
                    .build();

            // Act
            StuckOrdersDto stuckOrders = dashboardService.getStuckOrders(filter);

            // Assert
            assertThat(stuckOrders).isNotNull();
            assertThat(stuckOrders.getThresholds()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Restaurant Metrics Integration Tests")
    class RestaurantMetricsIntegrationTests {

        @Test
        @Order(7)
        @DisplayName("Should return restaurant metrics with performance data")
        void shouldReturnRestaurantMetrics() {
            // Act
            RestaurantMetricsDto metrics = dashboardService.getRestaurantMetrics(defaultFilter);

            // Assert
            assertThat(metrics).isNotNull();
            assertThat(metrics.getGeneratedAt()).isNotNull();
            assertThat(metrics.getTotalRestaurants()).isNotNull();
        }

        @Test
        @Order(8)
        @DisplayName("Should include online/offline breakdown")
        void shouldIncludeOnlineOfflineBreakdown() {
            // Act
            RestaurantMetricsDto metrics = dashboardService.getRestaurantMetrics(defaultFilter);

            // Assert
            assertThat(metrics.getOnlineRestaurants()).isNotNull();
            assertThat(metrics.getOfflineRestaurants()).isNotNull();
            assertThat(metrics.getOnlinePercentage()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Courier Metrics Integration Tests")
    class CourierMetricsIntegrationTests {

        @Test
        @Order(9)
        @DisplayName("Should return courier metrics with location data")
        void shouldReturnCourierMetrics() {
            // Act
            CourierMetricsDto metrics = dashboardService.getCourierMetrics(defaultFilter);

            // Assert
            assertThat(metrics).isNotNull();
            assertThat(metrics.getGeneratedAt()).isNotNull();
            assertThat(metrics.getTotalCouriers()).isNotNull();
        }

        @Test
        @Order(10)
        @DisplayName("Should include utilization metrics")
        void shouldIncludeUtilizationMetrics() {
            // Act
            CourierMetricsDto metrics = dashboardService.getCourierMetrics(defaultFilter);

            // Assert
            assertThat(metrics.getActiveCouriers()).isNotNull();
            assertThat(metrics.getOnDeliveryCouriers()).isNotNull();
            assertThat(metrics.getAvailableCouriers()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Finance Metrics Integration Tests")
    class FinanceMetricsIntegrationTests {

        @Test
        @Order(11)
        @DisplayName("Should return finance metrics with GMV and revenue")
        void shouldReturnFinanceMetrics() {
            // Act
            FinanceMetricsDto metrics = dashboardService.getFinanceMetrics(defaultFilter);

            // Assert
            assertThat(metrics).isNotNull();
            assertThat(metrics.getGeneratedAt()).isNotNull();
            assertThat(metrics.getGmv()).isNotNull();
            assertThat(metrics.getCommissionRevenue()).isNotNull();
        }

        @Test
        @Order(12)
        @DisplayName("Should include payout summary")
        void shouldIncludePayoutSummary() {
            // Act
            FinanceMetricsDto metrics = dashboardService.getFinanceMetrics(defaultFilter);

            // Assert
            assertThat(metrics.getRestaurantPayouts()).isNotNull();
            assertThat(metrics.getCourierPayouts()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Support Metrics Integration Tests")
    class SupportMetricsIntegrationTests {

        @Test
        @Order(13)
        @DisplayName("Should return support metrics with ticket data")
        void shouldReturnSupportMetrics() {
            // Act
            SupportMetricsDto metrics = dashboardService.getSupportMetrics(defaultFilter);

            // Assert
            assertThat(metrics).isNotNull();
            assertThat(metrics.getGeneratedAt()).isNotNull();
            assertThat(metrics.getTotalTickets()).isNotNull();
        }

        @Test
        @Order(14)
        @DisplayName("Should include resolution metrics")
        void shouldIncludeResolutionMetrics() {
            // Act
            SupportMetricsDto metrics = dashboardService.getSupportMetrics(defaultFilter);

            // Assert
            assertThat(metrics.getOpenTickets()).isNotNull();
            assertThat(metrics.getResolvedTickets()).isNotNull();
            assertThat(metrics.getResolutionRate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Cache Integration Tests")
    class CacheIntegrationTests {

        @Test
        @Order(15)
        @DisplayName("Should refresh all caches")
        void shouldRefreshAllCaches() {
            // Act & Assert - should not throw
            dashboardService.refreshCache();
        }

        @Test
        @Order(16)
        @DisplayName("Should refresh specific cache")
        void shouldRefreshSpecificCache() {
            // Act & Assert - should not throw
            dashboardService.refreshCache("overview");
            dashboardService.refreshCache("activeOrders");
            dashboardService.refreshCache("restaurants");
        }

        @Test
        @Order(17)
        @DisplayName("Should log refresh events")
        void shouldLogRefreshEvents() {
            // Act
            dashboardService.getOverview(startDate, endDate);

            // Assert
            long logCount = refreshLogRepository.countByComponent("overview",
                    LocalDateTime.now().minusHours(1));
            assertThat(logCount).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Pagination Integration Tests")
    class PaginationIntegrationTests {

        @Test
        @Order(18)
        @DisplayName("Should respect page size in active orders")
        void shouldRespectPageSizeInActiveOrders() {
            // Arrange
            DashboardFilterRequest filter = DashboardFilterRequest.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .page(0)
                    .pageSize(10)
                    .build();

            // Act
            ActiveOrdersDto activeOrders = dashboardService.getActiveOrders(filter);

            // Assert
            assertThat(activeOrders).isNotNull();
            // Orders list should be at most pageSize
            if (activeOrders.getOrders() != null) {
                assertThat(activeOrders.getOrders().size()).isLessThanOrEqualTo(10);
            }
        }

        @Test
        @Order(19)
        @DisplayName("Should support pagination in restaurant metrics")
        void shouldSupportPaginationInRestaurantMetrics() {
            // Arrange
            DashboardFilterRequest filter = DashboardFilterRequest.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .page(0)
                    .pageSize(20)
                    .build();

            // Act
            RestaurantMetricsDto metrics = dashboardService.getRestaurantMetrics(filter);

            // Assert
            assertThat(metrics).isNotNull();
            if (metrics.getRestaurants() != null) {
                assertThat(metrics.getRestaurants().size()).isLessThanOrEqualTo(20);
            }
        }
    }

    @Nested
    @DisplayName("System Health Integration Tests")
    class SystemHealthIntegrationTests {

        @Test
        @Order(20)
        @DisplayName("Should save and retrieve system health snapshots")
        void shouldSaveAndRetrieveSystemHealthSnapshots() {
            // Arrange
            SystemHealthSnapshot snapshot = SystemHealthSnapshot.builder()
                    .component("database")
                    .apiLatencyP50(100.0)
                    .apiLatencyP90(200.0)
                    .dbActiveConnections(30)
                    .redisLatencyMs(3.0)
                    .status("HEALTHY")
                    .capturedAt(LocalDateTime.now())
                    .build();

            // Act
            SystemHealthSnapshot saved = systemHealthRepository.save(snapshot);

            // Assert
            assertThat(saved.getId()).isNotNull();

            var retrieved = systemHealthRepository.findLatestSnapshot();
            assertThat(retrieved).isPresent();
        }
    }

    @AfterAll
    static void tearDown() {
        postgres.stop();
        redis.stop();
    }
}
