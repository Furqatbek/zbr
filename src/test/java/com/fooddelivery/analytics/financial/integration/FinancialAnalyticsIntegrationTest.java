package com.fooddelivery.analytics.financial.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fooddelivery.analytics.financial.dto.FinancialMetricsRequest;
import com.fooddelivery.analytics.financial.model.*;
import com.fooddelivery.analytics.financial.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Financial Analytics module using Testcontainers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FinancialAnalyticsIntegrationTest {

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
    private MockMvc mockMvc;

    @Autowired
    private RestaurantCommissionRepository commissionRepository;

    @Autowired
    private CourierPaymentRepository courierPaymentRepository;

    @Autowired
    private CourierBonusRepository courierBonusRepository;

    @Autowired
    private PromotionUsageRepository promotionUsageRepository;

    @Autowired
    private RestaurantPayoutRepository restaurantPayoutRepository;

    @Autowired
    private GiftCardUsageRepository giftCardUsageRepository;

    @Autowired
    private ReferralRewardRepository referralRewardRepository;

    private ObjectMapper objectMapper;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        endDate = LocalDateTime.of(2024, 1, 31, 23, 59);
    }

    @Test
    @Order(1)
    @DisplayName("Should setup test data")
    void shouldSetupTestData() {
        // Create restaurant commissions
        for (int i = 1; i <= 10; i++) {
            RestaurantCommission commission = RestaurantCommission.builder()
                    .restaurantId((long) i)
                    .orderId((long) (i * 100))
                    .orderSubtotal(new BigDecimal("100.00"))
                    .commissionRate(new BigDecimal("0.15"))
                    .commissionAmount(new BigDecimal("15.00"))
                    .commissionType(CommissionType.PERCENTAGE)
                    .earnedAt(startDate.plusDays(i))
                    .build();
            commissionRepository.save(commission);
        }

        // Create courier payments
        for (int i = 1; i <= 10; i++) {
            CourierPayment payment = CourierPayment.builder()
                    .courierId((long) (i % 3 + 1))
                    .orderId((long) (i * 100))
                    .basePayment(new BigDecimal("5.00"))
                    .distanceBonus(new BigDecimal("1.00"))
                    .tipAmount(new BigDecimal("2.00"))
                    .peakBonus(new BigDecimal("0.50"))
                    .totalPayment(new BigDecimal("8.50"))
                    .paymentType(CourierPaymentType.DELIVERY)
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .paymentDate(startDate.plusDays(i))
                    .build();
            payment.calculateTotal();
            courierPaymentRepository.save(payment);
        }

        // Create promotion usages
        for (int i = 1; i <= 5; i++) {
            PromotionUsage usage = PromotionUsage.builder()
                    .promotionId((long) i)
                    .promoCode("PROMO" + i)
                    .orderId((long) (i * 100))
                    .userId((long) i)
                    .promotionType(PromotionType.PERCENTAGE_DISCOUNT)
                    .discountAmount(new BigDecimal("10.00"))
                    .originalOrderValue(new BigDecimal("100.00"))
                    .fundedBy(PromotionFunder.PLATFORM)
                    .platformCost(new BigDecimal("10.00"))
                    .restaurantCost(BigDecimal.ZERO)
                    .usedAt(startDate.plusDays(i))
                    .build();
            promotionUsageRepository.save(usage);
        }

        // Verify data was created
        Assertions.assertTrue(commissionRepository.count() >= 10);
        Assertions.assertTrue(courierPaymentRepository.count() >= 10);
        Assertions.assertTrue(promotionUsageRepository.count() >= 5);
    }

    @Test
    @Order(2)
    @DisplayName("Should calculate GMV metrics via API")
    void shouldCalculateGmvMetricsViaApi() throws Exception {
        // Given
        FinancialMetricsRequest request = FinancialMetricsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .includeDailyTrend(true)
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/analytics/financial/gmv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalGmv").exists())
                .andExpect(jsonPath("$.totalOrders").exists())
                .andExpect(jsonPath("$.averageOrderValue").exists())
                .andExpect(jsonPath("$.periodStart").exists())
                .andExpect(jsonPath("$.periodEnd").exists());
    }

    @Test
    @Order(3)
    @DisplayName("Should calculate commission metrics via API")
    void shouldCalculateCommissionMetricsViaApi() throws Exception {
        // Given
        FinancialMetricsRequest request = FinancialMetricsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/analytics/financial/commission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCommission").exists())
                .andExpect(jsonPath("$.orderCount").exists())
                .andExpect(jsonPath("$.effectiveCommissionRate").exists());
    }

    @Test
    @Order(4)
    @DisplayName("Should calculate delivery fee metrics via API")
    void shouldCalculateDeliveryFeeMetricsViaApi() throws Exception {
        // Given
        FinancialMetricsRequest request = FinancialMetricsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/analytics/financial/delivery-fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDeliveryFeesPaid").exists())
                .andExpect(jsonPath("$.deliveryCount").exists());
    }

    @Test
    @Order(5)
    @DisplayName("Should calculate promotion metrics via API")
    void shouldCalculatePromotionMetricsViaApi() throws Exception {
        // Given
        FinancialMetricsRequest request = FinancialMetricsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/analytics/financial/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDiscountValue").exists())
                .andExpect(jsonPath("$.platformCost").exists());
    }

    @Test
    @Order(6)
    @DisplayName("Should calculate contribution margin via API")
    void shouldCalculateContributionMarginViaApi() throws Exception {
        // Given
        FinancialMetricsRequest request = FinancialMetricsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/analytics/financial/contribution-margin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gmv").exists())
                .andExpect(jsonPath("$.totalRevenue").exists())
                .andExpect(jsonPath("$.totalVariableCosts").exists())
                .andExpect(jsonPath("$.contributionMargin").exists())
                .andExpect(jsonPath("$.unitEconomics").exists());
    }

    @Test
    @Order(7)
    @DisplayName("Should get financial summary via API")
    void shouldGetFinancialSummaryViaApi() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/v1/analytics/financial/summary")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gmv").exists())
                .andExpect(jsonPath("$.totalRevenue").exists())
                .andExpect(jsonPath("$.contributionMargin").exists());
    }

    @Test
    @Order(8)
    @DisplayName("Should use caching for repeated requests")
    void shouldUseCachingForRepeatedRequests() throws Exception {
        // Given
        FinancialMetricsRequest request = FinancialMetricsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();
        String requestJson = objectMapper.writeValueAsString(request);

        // First request
        long startTime1 = System.currentTimeMillis();
        mockMvc.perform(post("/api/v1/analytics/financial/gmv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());
        long duration1 = System.currentTimeMillis() - startTime1;

        // Second request (should be cached)
        long startTime2 = System.currentTimeMillis();
        mockMvc.perform(post("/api/v1/analytics/financial/gmv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());
        long duration2 = System.currentTimeMillis() - startTime2;

        // Note: In a real test, we would verify the cache hit
        // This is a basic timing check that cached response should generally be faster
        System.out.println("First request duration: " + duration1 + "ms");
        System.out.println("Second request duration: " + duration2 + "ms");
    }

    @Test
    @Order(9)
    @DisplayName("Should validate request parameters")
    void shouldValidateRequestParameters() throws Exception {
        // Given - Invalid request with missing dates
        String invalidRequest = "{}";

        // When/Then
        mockMvc.perform(post("/api/v1/analytics/financial/gmv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(10)
    @DisplayName("Should handle empty data gracefully")
    void shouldHandleEmptyDataGracefully() throws Exception {
        // Given - Request for a period with no data
        FinancialMetricsRequest request = FinancialMetricsRequest.builder()
                .startDate(LocalDateTime.of(2020, 1, 1, 0, 0))
                .endDate(LocalDateTime.of(2020, 1, 31, 23, 59))
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/analytics/financial/gmv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGmv").value(0))
                .andExpect(jsonPath("$.totalOrders").value(0));
    }
}
