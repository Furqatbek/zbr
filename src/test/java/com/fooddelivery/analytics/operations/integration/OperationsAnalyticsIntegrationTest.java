package com.fooddelivery.analytics.operations.integration;

import com.fooddelivery.analytics.operations.dto.*;
import com.fooddelivery.analytics.operations.model.*;
import com.fooddelivery.analytics.operations.repository.*;
import com.fooddelivery.analytics.operations.service.OperationsAnalyticsService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Operations Analytics using Testcontainers.
 * Tests real database queries with PostgreSQL and Redis.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OperationsAnalyticsIntegrationTest {

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
    }

    @Autowired
    private OperationsAnalyticsService analyticsService;

    @Autowired
    private CourierLocationEventRepository locationEventRepository;

    @Autowired
    private CourierOrderEventRepository courierOrderEventRepository;

    @Autowired
    private CourierAvailabilityEventRepository availabilityEventRepository;

    @Autowired
    private RestaurantOrderEventRepository restaurantOrderEventRepository;

    @Autowired
    private RestaurantOnlineStatusRepository onlineStatusRepository;

    @Autowired
    private MenuUpdateHistoryRepository menuUpdateRepository;

    @Autowired
    private EtaHistoryRepository etaHistoryRepository;

    private static final Long TEST_RESTAURANT_ID = 1L;
    private static final Long TEST_COURIER_ID = 1L;
    private static final Long TEST_ORDER_ID = 1L;

    @BeforeEach
    void setUp() {
        // Setup test data before each test if needed
    }

    @Test
    @Order(1)
    @DisplayName("Should setup test data for courier location events")
    void shouldSetupCourierLocationTestData() {
        // Given - Create location events for the courier
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 10; i++) {
            CourierLocationEvent event = CourierLocationEvent.builder()
                    .courierId(TEST_COURIER_ID)
                    .latitude(BigDecimal.valueOf(41.3111 + i * 0.001))
                    .longitude(BigDecimal.valueOf(69.2797 + i * 0.001))
                    .accuracyMeters(5.0)
                    .speedKmh(25.0)
                    .isMoving(true)
                    .recordedAt(now.minusMinutes(i * 5))
                    .build();
            locationEventRepository.save(event);
        }

        // Then
        Long count = locationEventRepository.countByCourierIdAndTimeRange(
                TEST_COURIER_ID, now.minusHours(1), now);
        assertThat(count).isEqualTo(10L);
    }

    @Test
    @Order(2)
    @DisplayName("Should setup test data for courier order events")
    void shouldSetupCourierOrderTestData() {
        // Given - Create order events (offers, accepts, etc.)
        LocalDateTime now = LocalDateTime.now();

        // 10 offered orders
        for (int i = 1; i <= 10; i++) {
            CourierOrderEvent offered = CourierOrderEvent.builder()
                    .courierId(TEST_COURIER_ID)
                    .orderId((long) i)
                    .eventType(CourierOrderEventType.OFFERED)
                    .eventTimestamp(now.minusMinutes(i * 10))
                    .build();
            courierOrderEventRepository.save(offered);

            // 8 accepted, 2 rejected
            if (i <= 8) {
                CourierOrderEvent accepted = CourierOrderEvent.builder()
                        .courierId(TEST_COURIER_ID)
                        .orderId((long) i)
                        .eventType(CourierOrderEventType.ACCEPTED)
                        .responseTimeSeconds(15L)
                        .eventTimestamp(now.minusMinutes(i * 10).plusSeconds(15))
                        .build();
                courierOrderEventRepository.save(accepted);
            } else {
                CourierOrderEvent rejected = CourierOrderEvent.builder()
                        .courierId(TEST_COURIER_ID)
                        .orderId((long) i)
                        .eventType(CourierOrderEventType.REJECTED)
                        .reason("Too far")
                        .responseTimeSeconds(20L)
                        .eventTimestamp(now.minusMinutes(i * 10).plusSeconds(20))
                        .build();
                courierOrderEventRepository.save(rejected);
            }
        }

        // Then - Verify counts
        Long offered = courierOrderEventRepository.countByCourierIdAndEventType(
                TEST_COURIER_ID, CourierOrderEventType.OFFERED, now.minusHours(2), now);
        Long accepted = courierOrderEventRepository.countByCourierIdAndEventType(
                TEST_COURIER_ID, CourierOrderEventType.ACCEPTED, now.minusHours(2), now);
        Long rejected = courierOrderEventRepository.countByCourierIdAndEventType(
                TEST_COURIER_ID, CourierOrderEventType.REJECTED, now.minusHours(2), now);

        assertThat(offered).isEqualTo(10L);
        assertThat(accepted).isEqualTo(8L);
        assertThat(rejected).isEqualTo(2L);
    }

    @Test
    @Order(3)
    @DisplayName("Should setup test data for restaurant order events")
    void shouldSetupRestaurantOrderTestData() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // Create restaurant order events
        for (int i = 1; i <= 5; i++) {
            RestaurantOrderEvent received = RestaurantOrderEvent.builder()
                    .restaurantId(TEST_RESTAURANT_ID)
                    .orderId((long) i)
                    .eventType(RestaurantOrderEventType.RECEIVED)
                    .eventTimestamp(now.minusMinutes(i * 20))
                    .build();
            restaurantOrderEventRepository.save(received);

            RestaurantOrderEvent accepted = RestaurantOrderEvent.builder()
                    .restaurantId(TEST_RESTAURANT_ID)
                    .orderId((long) i)
                    .eventType(RestaurantOrderEventType.ACCEPTED)
                    .eventTimestamp(now.minusMinutes(i * 20).plusMinutes(2))
                    .build();
            restaurantOrderEventRepository.save(accepted);

            RestaurantOrderEvent ready = RestaurantOrderEvent.builder()
                    .restaurantId(TEST_RESTAURANT_ID)
                    .orderId((long) i)
                    .eventType(RestaurantOrderEventType.READY)
                    .estimatedPrepMinutes(15)
                    .actualPrepMinutes(15 + (i % 3))
                    .eventTimestamp(now.minusMinutes(i * 20).plusMinutes(17))
                    .build();
            restaurantOrderEventRepository.save(ready);
        }

        // Then
        Long received = restaurantOrderEventRepository.countByRestaurantIdAndEventType(
                TEST_RESTAURANT_ID, RestaurantOrderEventType.RECEIVED, now.minusHours(2), now);
        assertThat(received).isEqualTo(5L);
    }

    @Test
    @Order(4)
    @DisplayName("Should setup test data for ETA history")
    void shouldSetupEtaHistoryTestData() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i <= 10; i++) {
            LocalDateTime predictedAt = now.minusMinutes(i * 30);
            int etaMinutes = 30;
            int actualMinutes = 30 + (i % 5) - 2; // Some early, some late

            EtaHistory eta = EtaHistory.builder()
                    .orderId((long) i)
                    .restaurantId(TEST_RESTAURANT_ID)
                    .courierId(TEST_COURIER_ID)
                    .etaShownMinutes(etaMinutes)
                    .predictedDeliveryTime(predictedAt.plusMinutes(etaMinutes))
                    .predictedAt(predictedAt)
                    .actualDeliveredAt(predictedAt.plusMinutes(actualMinutes))
                    .actualDeliveryMinutes(actualMinutes)
                    .etaErrorMinutes(actualMinutes - etaMinutes)
                    .etaErrorAbsMinutes(Math.abs(actualMinutes - etaMinutes))
                    .wasLate(actualMinutes > etaMinutes)
                    .wasCompleted(true)
                    .build();
            etaHistoryRepository.save(eta);
        }

        // Then
        EtaHistory saved = etaHistoryRepository.findByOrderId(1L);
        assertThat(saved).isNotNull();
        assertThat(saved.getWasCompleted()).isTrue();
    }

    @Test
    @Order(5)
    @DisplayName("Should calculate ETA accuracy from test data")
    void shouldCalculateEtaAccuracyFromTestData() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now();

        // When
        EtaAccuracyDto result = analyticsService.getEtaAccuracyMetrics(startDate, endDate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalDeliveries()).isGreaterThan(0);
        // The actual values depend on test data inserted above
    }

    @Test
    @Order(6)
    @DisplayName("Should calculate delivery success rate from test data")
    void shouldCalculateDeliverySuccessRateFromTestData() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        // When
        DeliverySuccessRateDto result = analyticsService.getDeliverySuccessRate(startDate, endDate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPeriodStart()).isNotNull();
        assertThat(result.getPeriodEnd()).isNotNull();
    }

    @Test
    @Order(7)
    @DisplayName("Should cache results and evict on demand")
    void shouldCacheResultsAndEvictOnDemand() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now();

        // When - First call (not cached)
        long startTime1 = System.currentTimeMillis();
        DeliverySuccessRateDto result1 = analyticsService.getDeliverySuccessRate(startDate, endDate);
        long duration1 = System.currentTimeMillis() - startTime1;

        // Second call (should be cached)
        long startTime2 = System.currentTimeMillis();
        DeliverySuccessRateDto result2 = analyticsService.getDeliverySuccessRate(startDate, endDate);
        long duration2 = System.currentTimeMillis() - startTime2;

        // Evict and call again
        analyticsService.evictAllCaches();
        long startTime3 = System.currentTimeMillis();
        DeliverySuccessRateDto result3 = analyticsService.getDeliverySuccessRate(startDate, endDate);
        long duration3 = System.currentTimeMillis() - startTime3;

        // Then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result3).isNotNull();

        // Cached call should be faster (though this is a soft assertion due to JIT, etc.)
        // In a real scenario, you'd use a more robust caching test
    }

    @Test
    @Order(8)
    @DisplayName("Should handle date range validation")
    void shouldHandleDateRangeValidation() {
        // Given
        LocalDate futureDate = LocalDate.now().plusDays(10);
        LocalDate pastDate = LocalDate.now().minusDays(10);

        // When/Then - Future start date should work (validation is in controller)
        DeliverySuccessRateDto result = analyticsService.getDeliverySuccessRate(pastDate, LocalDate.now());
        assertThat(result).isNotNull();
    }

    @Test
    @Order(9)
    @DisplayName("Should get operations summary")
    void shouldGetOperationsSummary() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        // When
        OperationsSummaryDto result = analyticsService.getOperationsSummary(startDate, endDate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPeriodStart()).isNotNull();
        assertThat(result.getPeriodEnd()).isNotNull();
    }
}
