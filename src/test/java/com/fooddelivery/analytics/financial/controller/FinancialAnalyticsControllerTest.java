package com.fooddelivery.analytics.financial.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fooddelivery.analytics.financial.dto.*;
import com.fooddelivery.analytics.financial.service.FinancialAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for FinancialAnalyticsController.
 */
@WebMvcTest(FinancialAnalyticsController.class)
class FinancialAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FinancialAnalyticsService financialAnalyticsService;

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

    @Nested
    @DisplayName("GMV Metrics Endpoint Tests")
    class GmvMetricsEndpointTests {

        @Test
        @DisplayName("Should return GMV metrics successfully")
        void shouldReturnGmvMetrics() throws Exception {
            // Given
            FinancialMetricsRequest request = FinancialMetricsRequest.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();

            GmvMetricsDto response = GmvMetricsDto.builder()
                    .totalGmv(new BigDecimal("100000.00"))
                    .totalOrders(500L)
                    .averageOrderValue(new BigDecimal("200.00"))
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(financialAnalyticsService.calculateGmvMetrics(any())).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/analytics/financial/gmv")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.totalGmv").value(100000.00))
                    .andExpect(jsonPath("$.totalOrders").value(500))
                    .andExpect(jsonPath("$.averageOrderValue").value(200.00));
        }

        @Test
        @DisplayName("Should return 400 for missing start date")
        void shouldReturn400ForMissingStartDate() throws Exception {
            // Given
            String invalidRequest = "{\"endDate\": \"2024-01-31T23:59:00\"}";

            // When/Then
            mockMvc.perform(post("/api/v1/analytics/financial/gmv")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Commission Metrics Endpoint Tests")
    class CommissionMetricsEndpointTests {

        @Test
        @DisplayName("Should return commission metrics successfully")
        void shouldReturnCommissionMetrics() throws Exception {
            // Given
            FinancialMetricsRequest request = FinancialMetricsRequest.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();

            CommissionMetricsDto response = CommissionMetricsDto.builder()
                    .totalCommission(new BigDecimal("15000.00"))
                    .orderCount(500L)
                    .averageCommissionPerOrder(new BigDecimal("30.00"))
                    .effectiveCommissionRate(new BigDecimal("0.15"))
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(financialAnalyticsService.calculateCommissionMetrics(any())).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/analytics/financial/commission")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCommission").value(15000.00))
                    .andExpect(jsonPath("$.orderCount").value(500))
                    .andExpect(jsonPath("$.effectiveCommissionRate").value(0.15));
        }
    }

    @Nested
    @DisplayName("Delivery Fee Metrics Endpoint Tests")
    class DeliveryFeeMetricsEndpointTests {

        @Test
        @DisplayName("Should return delivery fee metrics successfully")
        void shouldReturnDeliveryFeeMetrics() throws Exception {
            // Given
            FinancialMetricsRequest request = FinancialMetricsRequest.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();

            DeliveryFeeMetricsDto response = DeliveryFeeMetricsDto.builder()
                    .totalDeliveryFeesCollected(new BigDecimal("10000.00"))
                    .totalDeliveryFeesPaid(new BigDecimal("8500.00"))
                    .netDeliveryFeeRevenue(new BigDecimal("1500.00"))
                    .deliveryCount(1000L)
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(financialAnalyticsService.calculateDeliveryFeeMetrics(any())).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/analytics/financial/delivery-fees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDeliveryFeesCollected").value(10000.00))
                    .andExpect(jsonPath("$.netDeliveryFeeRevenue").value(1500.00))
                    .andExpect(jsonPath("$.deliveryCount").value(1000));
        }
    }

    @Nested
    @DisplayName("Promotion Metrics Endpoint Tests")
    class PromotionMetricsEndpointTests {

        @Test
        @DisplayName("Should return promotion metrics successfully")
        void shouldReturnPromotionMetrics() throws Exception {
            // Given
            FinancialMetricsRequest request = FinancialMetricsRequest.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();

            PromotionMetricsDto response = PromotionMetricsDto.builder()
                    .totalDiscountValue(new BigDecimal("8000.00"))
                    .platformCost(new BigDecimal("5000.00"))
                    .restaurantCost(new BigDecimal("3000.00"))
                    .promotedOrderCount(400L)
                    .promotionUsageRate(new BigDecimal("80.00"))
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(financialAnalyticsService.calculatePromotionMetrics(any())).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/analytics/financial/promotions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDiscountValue").value(8000.00))
                    .andExpect(jsonPath("$.platformCost").value(5000.00))
                    .andExpect(jsonPath("$.promotionUsageRate").value(80.00));
        }
    }

    @Nested
    @DisplayName("Contribution Margin Endpoint Tests")
    class ContributionMarginEndpointTests {

        @Test
        @DisplayName("Should return contribution margin successfully")
        void shouldReturnContributionMargin() throws Exception {
            // Given
            FinancialMetricsRequest request = FinancialMetricsRequest.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();

            ContributionMarginDto response = ContributionMarginDto.builder()
                    .gmv(new BigDecimal("100000.00"))
                    .totalRevenue(new BigDecimal("16500.00"))
                    .totalVariableCosts(new BigDecimal("12500.00"))
                    .contributionMargin(new BigDecimal("4000.00"))
                    .contributionMarginPercentage(new BigDecimal("4.00"))
                    .orderCount(500L)
                    .unitEconomics(new BigDecimal("8.00"))
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(financialAnalyticsService.calculateContributionMargin(any())).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/analytics/financial/contribution-margin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.gmv").value(100000.00))
                    .andExpect(jsonPath("$.contributionMargin").value(4000.00))
                    .andExpect(jsonPath("$.unitEconomics").value(8.00));
        }
    }

    @Nested
    @DisplayName("Financial Summary Endpoint Tests")
    class FinancialSummaryEndpointTests {

        @Test
        @DisplayName("Should return financial summary successfully")
        void shouldReturnFinancialSummary() throws Exception {
            // Given
            FinancialSummaryDto response = FinancialSummaryDto.builder()
                    .gmv(new BigDecimal("100000.00"))
                    .totalRevenue(new BigDecimal("16500.00"))
                    .commissionRevenue(new BigDecimal("15000.00"))
                    .contributionMargin(new BigDecimal("4000.00"))
                    .totalOrders(500L)
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(financialAnalyticsService.getFinancialSummary(any(), any())).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/analytics/financial/summary")
                            .param("startDate", "2024-01-01T00:00:00")
                            .param("endDate", "2024-01-31T23:59:00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.gmv").value(100000.00))
                    .andExpect(jsonPath("$.totalRevenue").value(16500.00))
                    .andExpect(jsonPath("$.totalOrders").value(500));
        }
    }

    @Nested
    @DisplayName("Restaurant Payout Details Endpoint Tests")
    class RestaurantPayoutDetailsEndpointTests {

        @Test
        @DisplayName("Should return restaurant payout details successfully")
        void shouldReturnRestaurantPayoutDetails() throws Exception {
            // Given
            RestaurantPayoutMetricsDto response = RestaurantPayoutMetricsDto.builder()
                    .totalGrossSales(new BigDecimal("50000.00"))
                    .netPayoutAmount(new BigDecimal("42500.00"))
                    .restaurantCount(1L)
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(financialAnalyticsService.getRestaurantPayoutDetails(any(), any(), any())).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/analytics/financial/restaurants/1/payouts")
                            .param("startDate", "2024-01-01T00:00:00")
                            .param("endDate", "2024-01-31T23:59:00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalGrossSales").value(50000.00))
                    .andExpect(jsonPath("$.netPayoutAmount").value(42500.00));
        }
    }

    @Nested
    @DisplayName("Courier Payout Details Endpoint Tests")
    class CourierPayoutDetailsEndpointTests {

        @Test
        @DisplayName("Should return courier payout details successfully")
        void shouldReturnCourierPayoutDetails() throws Exception {
            // Given
            CourierPayoutMetricsDto response = CourierPayoutMetricsDto.builder()
                    .totalPayouts(new BigDecimal("5000.00"))
                    .totalDeliveries(100L)
                    .activeCourierCount(1L)
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(financialAnalyticsService.getCourierPayoutDetails(any(), any(), any())).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/analytics/financial/couriers/1/payouts")
                            .param("startDate", "2024-01-01T00:00:00")
                            .param("endDate", "2024-01-31T23:59:00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPayouts").value(5000.00))
                    .andExpect(jsonPath("$.totalDeliveries").value(100));
        }
    }
}
