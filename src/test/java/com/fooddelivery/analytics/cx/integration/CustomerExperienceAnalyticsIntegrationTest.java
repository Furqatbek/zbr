package com.fooddelivery.analytics.cx.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fooddelivery.analytics.cx.dto.*;
import com.fooddelivery.analytics.cx.model.*;
import com.fooddelivery.analytics.cx.repository.*;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Customer Experience Analytics using Testcontainers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomerExperienceAnalyticsIntegrationTest {

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
    private NpsSurveyRepository npsSurveyRepository;

    @Autowired
    private RestaurantRatingRepository restaurantRatingRepository;

    @Autowired
    private CourierRatingRepository courierRatingRepository;

    @Autowired
    private AppStoreRatingRepository appStoreRatingRepository;

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    private ObjectMapper objectMapper;
    private LocalDateTime testStart;
    private LocalDateTime testEnd;
    private static final Random random = new Random(42);

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testStart = LocalDateTime.now().minusDays(30);
        testEnd = LocalDateTime.now();
    }

    @Test
    @Order(1)
    @DisplayName("Should seed test data and retrieve NPS metrics")
    void shouldRetrieveNpsMetrics() throws Exception {
        // Seed NPS survey data
        seedNpsSurveys();

        String startDateStr = testStart.format(DateTimeFormatter.ISO_DATE_TIME);
        String endDateStr = testEnd.format(DateTimeFormatter.ISO_DATE_TIME);

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/analytics/cx/nps")
                        .param("startDate", startDateStr)
                        .param("endDate", endDateStr)
                        .param("includeDistribution", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        NpsMetricsDto metrics = objectMapper.readValue(
                result.getResponse().getContentAsString(), NpsMetricsDto.class);

        assertThat(metrics).isNotNull();
        assertThat(metrics.getTotalResponses()).isGreaterThan(0);
        assertThat(metrics.getNpsScore()).isBetween(-100.0, 100.0);
        assertThat(metrics.getPromotersCount()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.getPassivesCount()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.getDetractorsCount()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.getScoreDistribution()).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("Should retrieve restaurant rating metrics")
    void shouldRetrieveRestaurantRatingMetrics() throws Exception {
        // Seed restaurant rating data
        seedRestaurantRatings();

        String startDateStr = testStart.format(DateTimeFormatter.ISO_DATE_TIME);
        String endDateStr = testEnd.format(DateTimeFormatter.ISO_DATE_TIME);

        // When - overall
        MvcResult overallResult = mockMvc.perform(get("/api/v1/analytics/cx/ratings/restaurant")
                        .param("startDate", startDateStr)
                        .param("endDate", endDateStr)
                        .param("includeDistribution", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        RestaurantRatingMetricsDto overallMetrics = objectMapper.readValue(
                overallResult.getResponse().getContentAsString(), RestaurantRatingMetricsDto.class);

        assertThat(overallMetrics).isNotNull();
        assertThat(overallMetrics.getAverageRating()).isBetween(1.0, 5.0);
        assertThat(overallMetrics.getRatingCount()).isGreaterThan(0);

        // When - specific restaurant
        MvcResult specificResult = mockMvc.perform(get("/api/v1/analytics/cx/ratings/restaurant/1")
                        .param("startDate", startDateStr)
                        .param("endDate", endDateStr)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        RestaurantRatingMetricsDto specificMetrics = objectMapper.readValue(
                specificResult.getResponse().getContentAsString(), RestaurantRatingMetricsDto.class);

        assertThat(specificMetrics).isNotNull();
        assertThat(specificMetrics.getRestaurantId()).isEqualTo(1L);
    }

    @Test
    @Order(3)
    @DisplayName("Should retrieve courier rating metrics")
    void shouldRetrieveCourierRatingMetrics() throws Exception {
        // Seed courier rating data
        seedCourierRatings();

        String startDateStr = testStart.format(DateTimeFormatter.ISO_DATE_TIME);
        String endDateStr = testEnd.format(DateTimeFormatter.ISO_DATE_TIME);

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/analytics/cx/ratings/courier")
                        .param("startDate", startDateStr)
                        .param("endDate", endDateStr)
                        .param("includeDistribution", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        CourierRatingMetricsDto metrics = objectMapper.readValue(
                result.getResponse().getContentAsString(), CourierRatingMetricsDto.class);

        assertThat(metrics).isNotNull();
        assertThat(metrics.getAverageRating()).isBetween(1.0, 5.0);
        assertThat(metrics.getRatingCount()).isGreaterThan(0);
    }

    @Test
    @Order(4)
    @DisplayName("Should retrieve app store rating metrics")
    void shouldRetrieveAppStoreRatingMetrics() throws Exception {
        // Seed app store rating data
        seedAppStoreRatings();

        String startDateStr = testStart.format(DateTimeFormatter.ISO_DATE_TIME);
        String endDateStr = testEnd.format(DateTimeFormatter.ISO_DATE_TIME);

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/analytics/cx/ratings/app-store")
                        .param("startDate", startDateStr)
                        .param("endDate", endDateStr)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        AppStoreRatingMetricsDto metrics = objectMapper.readValue(
                result.getResponse().getContentAsString(), AppStoreRatingMetricsDto.class);

        assertThat(metrics).isNotNull();
        assertThat(metrics.getOverallAverageRating()).isBetween(1.0, 5.0);
        assertThat(metrics.getTotalReviewCount()).isGreaterThan(0);
    }

    @Test
    @Order(5)
    @DisplayName("Should retrieve support ticket metrics")
    void shouldRetrieveSupportTicketMetrics() throws Exception {
        // Seed support ticket data
        seedSupportTickets();

        String startDateStr = testStart.format(DateTimeFormatter.ISO_DATE_TIME);
        String endDateStr = testEnd.format(DateTimeFormatter.ISO_DATE_TIME);

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/analytics/cx/support-tickets")
                        .param("startDate", startDateStr)
                        .param("endDate", endDateStr)
                        .param("slaHours", "24")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        SupportTicketMetricsDto metrics = objectMapper.readValue(
                result.getResponse().getContentAsString(), SupportTicketMetricsDto.class);

        assertThat(metrics).isNotNull();
        assertThat(metrics.getTotalTickets()).isGreaterThan(0);
        assertThat(metrics.getTicketsByType()).isNotEmpty();
        assertThat(metrics.getSlaMetrics()).isNotNull();
        assertThat(metrics.getSlaMetrics().getSlaComplianceRate()).isBetween(0.0, 100.0);
    }

    @Test
    @Order(6)
    @DisplayName("Should retrieve CX summary")
    void shouldRetrieveCxSummary() throws Exception {
        String startDateStr = testStart.format(DateTimeFormatter.ISO_DATE_TIME);
        String endDateStr = testEnd.format(DateTimeFormatter.ISO_DATE_TIME);

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/analytics/cx/summary")
                        .param("startDate", startDateStr)
                        .param("endDate", endDateStr)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        CxSummaryDto summary = objectMapper.readValue(
                result.getResponse().getContentAsString(), CxSummaryDto.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getOverallCxScore()).isNotNull();
        assertThat(summary.getCxStatus()).isNotNull();
        assertThat(summary.getNpsSummary()).isNotNull();
        assertThat(summary.getRatingsSummary()).isNotNull();
        assertThat(summary.getSupportSummary()).isNotNull();
    }

    @Test
    @Order(7)
    @DisplayName("Should invalidate caches successfully")
    void shouldInvalidateCaches() throws Exception {
        // Invalidate NPS cache
        mockMvc.perform(post("/api/v1/analytics/cx/cache/invalidate/nps")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Invalidate ratings cache
        mockMvc.perform(post("/api/v1/analytics/cx/cache/invalidate/ratings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Invalidate support ticket cache
        mockMvc.perform(post("/api/v1/analytics/cx/cache/invalidate/support-tickets")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Invalidate all caches
        mockMvc.perform(post("/api/v1/analytics/cx/cache/invalidate/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(8)
    @DisplayName("Should handle NPS formula correctly")
    void shouldHandleNpsFormulaCorrectly() throws Exception {
        // Clear existing data and seed controlled data
        npsSurveyRepository.deleteAll();

        // Seed exactly 100 surveys: 60 promoters (9-10), 25 passives (7-8), 15 detractors (0-6)
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 60; i++) {
            npsSurveyRepository.save(NpsSurvey.builder()
                    .userId((long) i)
                    .score(random.nextBoolean() ? 9 : 10) // Promoters
                    .surveyChannel(NpsSurvey.SurveyChannel.IN_APP)
                    .createdAt(now.minusDays(random.nextInt(30)))
                    .build());
        }
        for (int i = 0; i < 25; i++) {
            npsSurveyRepository.save(NpsSurvey.builder()
                    .userId((long) (60 + i))
                    .score(random.nextBoolean() ? 7 : 8) // Passives
                    .surveyChannel(NpsSurvey.SurveyChannel.EMAIL)
                    .createdAt(now.minusDays(random.nextInt(30)))
                    .build());
        }
        for (int i = 0; i < 15; i++) {
            npsSurveyRepository.save(NpsSurvey.builder()
                    .userId((long) (85 + i))
                    .score(random.nextInt(7)) // Detractors (0-6)
                    .surveyChannel(NpsSurvey.SurveyChannel.SMS)
                    .createdAt(now.minusDays(random.nextInt(30)))
                    .build());
        }

        String startDateStr = now.minusDays(31).format(DateTimeFormatter.ISO_DATE_TIME);
        String endDateStr = now.plusDays(1).format(DateTimeFormatter.ISO_DATE_TIME);

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/analytics/cx/nps")
                        .param("startDate", startDateStr)
                        .param("endDate", endDateStr)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Then - NPS = ((60 - 15) / 100) * 100 = 45
        NpsMetricsDto metrics = objectMapper.readValue(
                result.getResponse().getContentAsString(), NpsMetricsDto.class);

        assertThat(metrics.getTotalResponses()).isEqualTo(100L);
        assertThat(metrics.getPromotersCount()).isEqualTo(60L);
        assertThat(metrics.getPassivesCount()).isEqualTo(25L);
        assertThat(metrics.getDetractorsCount()).isEqualTo(15L);
        assertThat(metrics.getNpsScore()).isEqualTo(45.0);
    }

    // ==================== Seed Methods ====================

    private void seedNpsSurveys() {
        if (npsSurveyRepository.count() > 0) return;

        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 200; i++) {
            npsSurveyRepository.save(NpsSurvey.builder()
                    .userId((long) random.nextInt(1000))
                    .score(random.nextInt(11)) // 0-10
                    .comment(i % 3 == 0 ? "Sample comment " + i : null)
                    .surveyChannel(NpsSurvey.SurveyChannel.values()[random.nextInt(5)])
                    .userSegment(i % 2 == 0 ? "PREMIUM" : "REGULAR")
                    .createdAt(now.minusDays(random.nextInt(30)))
                    .build());
        }
    }

    private void seedRestaurantRatings() {
        if (restaurantRatingRepository.count() > 0) return;

        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 300; i++) {
            restaurantRatingRepository.save(RestaurantRating.builder()
                    .restaurantId((long) (random.nextInt(10) + 1))
                    .userId((long) random.nextInt(1000))
                    .orderId((long) i)
                    .score(random.nextInt(5) + 1) // 1-5
                    .foodQualityScore(random.nextInt(5) + 1)
                    .portionSizeScore(random.nextInt(5) + 1)
                    .valueForMoneyScore(random.nextInt(5) + 1)
                    .comment(i % 4 == 0 ? "Restaurant review " + i : null)
                    .createdAt(now.minusDays(random.nextInt(30)))
                    .build());
        }
    }

    private void seedCourierRatings() {
        if (courierRatingRepository.count() > 0) return;

        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 250; i++) {
            courierRatingRepository.save(CourierRating.builder()
                    .courierId((long) (random.nextInt(20) + 1))
                    .userId((long) random.nextInt(1000))
                    .orderId((long) i)
                    .deliveryId((long) i)
                    .score(random.nextInt(5) + 1) // 1-5
                    .professionalismScore(random.nextInt(5) + 1)
                    .communicationScore(random.nextInt(5) + 1)
                    .timelinessScore(random.nextInt(5) + 1)
                    .tipAmount(random.nextDouble() < 0.6 ? random.nextDouble() * 10 : 0.0)
                    .createdAt(now.minusDays(random.nextInt(30)))
                    .build());
        }
    }

    private void seedAppStoreRatings() {
        if (appStoreRatingRepository.count() > 0) return;

        LocalDateTime now = LocalDateTime.now();
        String[] versions = {"1.0.0", "1.1.0", "1.2.0", "2.0.0"};
        String[] countries = {"US", "UK", "DE", "FR", "JP"};

        for (int i = 0; i < 150; i++) {
            appStoreRatingRepository.save(AppStoreRating.builder()
                    .platform(i % 2 == 0 ? AppStoreRating.Platform.IOS : AppStoreRating.Platform.ANDROID)
                    .score(random.nextInt(5) + 1) // 1-5
                    .title("Review title " + i)
                    .comment(i % 3 == 0 ? "App store review " + i : null)
                    .reviewId("review-" + i)
                    .country(countries[random.nextInt(countries.length)])
                    .appVersion(versions[random.nextInt(versions.length)])
                    .sentimentScore(random.nextDouble() * 2 - 1) // -1 to 1
                    .createdAt(now.minusDays(random.nextInt(30)))
                    .build());
        }
    }

    private void seedSupportTickets() {
        if (supportTicketRepository.count() > 0) return;

        LocalDateTime now = LocalDateTime.now();
        SupportTicket.TicketType[] types = SupportTicket.TicketType.values();
        SupportTicket.TicketStatus[] statuses = SupportTicket.TicketStatus.values();
        SupportTicket.TicketPriority[] priorities = SupportTicket.TicketPriority.values();
        SupportTicket.TicketChannel[] channels = SupportTicket.TicketChannel.values();

        for (int i = 0; i < 100; i++) {
            LocalDateTime createdAt = now.minusDays(random.nextInt(30));
            SupportTicket.TicketStatus status = statuses[random.nextInt(statuses.length)];
            LocalDateTime resolvedAt = null;
            LocalDateTime closedAt = null;

            if (status == SupportTicket.TicketStatus.RESOLVED) {
                resolvedAt = createdAt.plusHours(random.nextInt(48));
            } else if (status == SupportTicket.TicketStatus.CLOSED) {
                resolvedAt = createdAt.plusHours(random.nextInt(24));
                closedAt = resolvedAt.plusHours(random.nextInt(24));
            }

            boolean slaBreached = random.nextDouble() < 0.1; // 10% breach rate

            supportTicketRepository.save(SupportTicket.builder()
                    .ticketNumber("TKT-" + String.format("%06d", i))
                    .userId((long) random.nextInt(1000))
                    .orderId((long) i)
                    .ticketType(types[random.nextInt(types.length)])
                    .status(status)
                    .priority(priorities[random.nextInt(priorities.length)])
                    .channel(channels[random.nextInt(channels.length)])
                    .subject("Support request " + i)
                    .description("Description for ticket " + i)
                    .assignedTo((long) (random.nextInt(10) + 1))
                    .firstResponseAt(createdAt.plusMinutes(random.nextInt(60)))
                    .resolvedAt(resolvedAt)
                    .closedAt(closedAt)
                    .slaBreach(slaBreached)
                    .escalated(random.nextDouble() < 0.05)
                    .customerSatisfactionScore(status == SupportTicket.TicketStatus.CLOSED ? random.nextInt(5) + 1 : null)
                    .isRefunded(random.nextDouble() < 0.2)
                    .refundAmount(random.nextDouble() < 0.2 ? random.nextDouble() * 50 : null)
                    .createdAt(createdAt)
                    .build());
        }
    }
}
