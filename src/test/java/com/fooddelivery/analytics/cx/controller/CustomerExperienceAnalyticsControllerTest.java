package com.fooddelivery.analytics.cx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.analytics.cx.dto.*;
import com.fooddelivery.analytics.cx.service.CustomerExperienceAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for CustomerExperienceAnalyticsController.
 */
@WebMvcTest(CustomerExperienceAnalyticsController.class)
class CustomerExperienceAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerExperienceAnalyticsService cxAnalyticsService;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String startDateStr;
    private String endDateStr;

    @BeforeEach
    void setUp() {
        startDate = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        endDate = LocalDateTime.of(2024, 1, 31, 23, 59, 59);
        startDateStr = startDate.format(DateTimeFormatter.ISO_DATE_TIME);
        endDateStr = endDate.format(DateTimeFormatter.ISO_DATE_TIME);
    }

    @Nested
    @DisplayName("NPS Endpoints Tests")
    class NpsEndpointsTests {

        @Test
        @DisplayName("Should return NPS metrics successfully")
        void shouldReturnNpsMetrics() throws Exception {
            // Given
            NpsMetricsDto npsMetrics = NpsMetricsDto.builder()
                    .npsScore(45.0)
                    .totalResponses(1000L)
                    .promotersCount(600L)
                    .passivesCount(250L)
                    .detractorsCount(150L)
                    .promotersPercentage(60.0)
                    .passivesPercentage(25.0)
                    .detractorsPercentage(15.0)
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(cxAnalyticsService.getNpsMetrics(
                    any(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(npsMetrics);

            // When & Then
            mockMvc.perform(get("/api/v1/analytics/cx/nps")
                            .param("startDate", startDateStr)
                            .param("endDate", endDateStr)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.npsScore").value(45.0))
                    .andExpect(jsonPath("$.totalResponses").value(1000))
                    .andExpect(jsonPath("$.promotersCount").value(600))
                    .andExpect(jsonPath("$.detractorsCount").value(150));
        }

        @Test
        @DisplayName("Should include distribution and trend when requested")
        void shouldIncludeDistributionAndTrend() throws Exception {
            // Given
            Map<Integer, Long> distribution = new HashMap<>();
            for (int i = 0; i <= 10; i++) {
                distribution.put(i, (long) (i * 10));
            }

            NpsMetricsDto npsMetrics = NpsMetricsDto.builder()
                    .npsScore(50.0)
                    .totalResponses(550L)
                    .scoreDistribution(distribution)
                    .build();

            when(cxAnalyticsService.getNpsMetrics(
                    any(), any(), eq(true), eq(true), eq(true)))
                    .thenReturn(npsMetrics);

            // When & Then
            mockMvc.perform(get("/api/v1/analytics/cx/nps")
                            .param("startDate", startDateStr)
                            .param("endDate", endDateStr)
                            .param("includeDistribution", "true")
                            .param("includeTrend", "true")
                            .param("includeSegments", "true")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scoreDistribution").isMap());
        }
    }

    @Nested
    @DisplayName("Restaurant Rating Endpoints Tests")
    class RestaurantRatingEndpointsTests {

        @Test
        @DisplayName("Should return overall restaurant ratings")
        void shouldReturnOverallRestaurantRatings() throws Exception {
            // Given
            RestaurantRatingMetricsDto metrics = RestaurantRatingMetricsDto.builder()
                    .averageRating(4.35)
                    .ratingCount(50000L)
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(cxAnalyticsService.getRestaurantRatingMetrics(
                    isNull(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(metrics);

            // When & Then
            mockMvc.perform(get("/api/v1/analytics/cx/ratings/restaurant")
                            .param("startDate", startDateStr)
                            .param("endDate", endDateStr)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.averageRating").value(4.35))
                    .andExpect(jsonPath("$.ratingCount").value(50000));
        }

        @Test
        @DisplayName("Should return restaurant ratings by ID")
        void shouldReturnRestaurantRatingsById() throws Exception {
            // Given
            RestaurantRatingMetricsDto metrics = RestaurantRatingMetricsDto.builder()
                    .restaurantId(123L)
                    .averageRating(4.5)
                    .ratingCount(1000L)
                    .averageFoodQualityRating(4.6)
                    .averagePortionSizeRating(4.3)
                    .averageValueForMoneyRating(4.4)
                    .build();

            when(cxAnalyticsService.getRestaurantRatingMetrics(
                    eq(123L), any(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(metrics);

            // When & Then
            mockMvc.perform(get("/api/v1/analytics/cx/ratings/restaurant/123")
                            .param("startDate", startDateStr)
                            .param("endDate", endDateStr)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.restaurantId").value(123))
                    .andExpect(jsonPath("$.averageRating").value(4.5))
                    .andExpect(jsonPath("$.averageFoodQualityRating").value(4.6));
        }
    }

    @Nested
    @DisplayName("Courier Rating Endpoints Tests")
    class CourierRatingEndpointsTests {

        @Test
        @DisplayName("Should return overall courier ratings")
        void shouldReturnOverallCourierRatings() throws Exception {
            // Given
            CourierRatingMetricsDto metrics = CourierRatingMetricsDto.builder()
                    .averageRating(4.7)
                    .ratingCount(30000L)
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(cxAnalyticsService.getCourierRatingMetrics(
                    isNull(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(metrics);

            // When & Then
            mockMvc.perform(get("/api/v1/analytics/cx/ratings/courier")
                            .param("startDate", startDateStr)
                            .param("endDate", endDateStr)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.averageRating").value(4.7))
                    .andExpect(jsonPath("$.ratingCount").value(30000));
        }

        @Test
        @DisplayName("Should return courier ratings by ID")
        void shouldReturnCourierRatingsById() throws Exception {
            // Given
            CourierRatingMetricsDto metrics = CourierRatingMetricsDto.builder()
                    .courierId(456L)
                    .averageRating(4.9)
                    .ratingCount(500L)
                    .averageProfessionalismRating(5.0)
                    .averageCommunicationRating(4.8)
                    .averageTimelinessRating(4.9)
                    .build();

            when(cxAnalyticsService.getCourierRatingMetrics(
                    eq(456L), any(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(metrics);

            // When & Then
            mockMvc.perform(get("/api/v1/analytics/cx/ratings/courier/456")
                            .param("startDate", startDateStr)
                            .param("endDate", endDateStr)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.courierId").value(456))
                    .andExpect(jsonPath("$.averageRating").value(4.9))
                    .andExpect(jsonPath("$.averageProfessionalismRating").value(5.0));
        }
    }

    @Nested
    @DisplayName("App Store Rating Endpoints Tests")
    class AppStoreRatingEndpointsTests {

        @Test
        @DisplayName("Should return app store ratings")
        void shouldReturnAppStoreRatings() throws Exception {
            // Given
            AppStoreRatingMetricsDto.PlatformRatingDto iosPlatform =
                    AppStoreRatingMetricsDto.PlatformRatingDto.builder()
                            .platform("IOS")
                            .averageRating(4.6)
                            .ratingCount(25000L)
                            .build();

            AppStoreRatingMetricsDto.PlatformRatingDto androidPlatform =
                    AppStoreRatingMetricsDto.PlatformRatingDto.builder()
                            .platform("ANDROID")
                            .averageRating(4.4)
                            .ratingCount(35000L)
                            .build();

            AppStoreRatingMetricsDto metrics = AppStoreRatingMetricsDto.builder()
                    .overallAverageRating(4.48)
                    .totalReviewCount(60000L)
                    .iosPlatform(iosPlatform)
                    .androidPlatform(androidPlatform)
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(cxAnalyticsService.getAppStoreRatingMetrics(
                    any(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(metrics);

            // When & Then
            mockMvc.perform(get("/api/v1/analytics/cx/ratings/app-store")
                            .param("startDate", startDateStr)
                            .param("endDate", endDateStr)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.overallAverageRating").value(4.48))
                    .andExpect(jsonPath("$.totalReviewCount").value(60000))
                    .andExpect(jsonPath("$.iosPlatform.averageRating").value(4.6))
                    .andExpect(jsonPath("$.androidPlatform.averageRating").value(4.4));
        }
    }

    @Nested
    @DisplayName("Support Ticket Endpoints Tests")
    class SupportTicketEndpointsTests {

        @Test
        @DisplayName("Should return support ticket metrics")
        void shouldReturnSupportTicketMetrics() throws Exception {
            // Given
            Map<String, Long> ticketsByType = new HashMap<>();
            ticketsByType.put("LATE_DELIVERY", 500L);
            ticketsByType.put("MISSING_ITEMS", 300L);

            SupportTicketMetricsDto.SlaMetricsDto slaMetrics =
                    SupportTicketMetricsDto.SlaMetricsDto.builder()
                            .slaHours(24)
                            .slaComplianceRate(95.0)
                            .ticketsBreachedSla(50L)
                            .build();

            SupportTicketMetricsDto metrics = SupportTicketMetricsDto.builder()
                    .totalTickets(1000L)
                    .openTickets(100L)
                    .ticketsByType(ticketsByType)
                    .slaMetrics(slaMetrics)
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(cxAnalyticsService.getSupportTicketMetrics(
                    any(), any(), anyInt(), anyBoolean(), anyBoolean()))
                    .thenReturn(metrics);

            // When & Then
            mockMvc.perform(get("/api/v1/analytics/cx/support-tickets")
                            .param("startDate", startDateStr)
                            .param("endDate", endDateStr)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTickets").value(1000))
                    .andExpect(jsonPath("$.openTickets").value(100))
                    .andExpect(jsonPath("$.slaMetrics.slaComplianceRate").value(95.0));
        }

        @Test
        @DisplayName("Should use custom SLA hours")
        void shouldUseCustomSlaHours() throws Exception {
            // Given
            SupportTicketMetricsDto metrics = SupportTicketMetricsDto.builder()
                    .totalTickets(500L)
                    .slaMetrics(SupportTicketMetricsDto.SlaMetricsDto.builder()
                            .slaHours(12)
                            .build())
                    .build();

            when(cxAnalyticsService.getSupportTicketMetrics(
                    any(), any(), eq(12), anyBoolean(), anyBoolean()))
                    .thenReturn(metrics);

            // When & Then
            mockMvc.perform(get("/api/v1/analytics/cx/support-tickets")
                            .param("startDate", startDateStr)
                            .param("endDate", endDateStr)
                            .param("slaHours", "12")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slaMetrics.slaHours").value(12));

            verify(cxAnalyticsService).getSupportTicketMetrics(
                    any(), any(), eq(12), anyBoolean(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("CX Summary Endpoints Tests")
    class CxSummaryEndpointsTests {

        @Test
        @DisplayName("Should return CX summary")
        void shouldReturnCxSummary() throws Exception {
            // Given
            CxSummaryDto summary = CxSummaryDto.builder()
                    .overallCxScore(82.5)
                    .cxStatus("EXCELLENT")
                    .npsSummary(CxSummaryDto.NpsSummary.builder()
                            .npsScore(50.0)
                            .totalResponses(1000L)
                            .build())
                    .ratingsSummary(CxSummaryDto.RatingsSummary.builder()
                            .overallAverageRating(4.5)
                            .build())
                    .supportSummary(CxSummaryDto.SupportSummary.builder()
                            .totalTickets(500L)
                            .slaComplianceRate(95.0)
                            .build())
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .generatedAt(LocalDateTime.now())
                    .build();

            when(cxAnalyticsService.getCxSummary(any(), any())).thenReturn(summary);

            // When & Then
            mockMvc.perform(get("/api/v1/analytics/cx/summary")
                            .param("startDate", startDateStr)
                            .param("endDate", endDateStr)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.overallCxScore").value(82.5))
                    .andExpect(jsonPath("$.cxStatus").value("EXCELLENT"))
                    .andExpect(jsonPath("$.npsSummary.npsScore").value(50.0))
                    .andExpect(jsonPath("$.ratingsSummary.overallAverageRating").value(4.5))
                    .andExpect(jsonPath("$.supportSummary.slaComplianceRate").value(95.0));
        }
    }

    @Nested
    @DisplayName("Cache Invalidation Endpoints Tests")
    class CacheInvalidationEndpointsTests {

        @Test
        @DisplayName("Should invalidate NPS cache")
        void shouldInvalidateNpsCache() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/analytics/cx/cache/invalidate/nps")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(cxAnalyticsService).invalidateNpsCache();
        }

        @Test
        @DisplayName("Should invalidate ratings cache")
        void shouldInvalidateRatingsCache() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/analytics/cx/cache/invalidate/ratings")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(cxAnalyticsService).invalidateRatingsCache(null, null);
        }

        @Test
        @DisplayName("Should invalidate ratings cache with specific IDs")
        void shouldInvalidateRatingsCacheWithIds() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/analytics/cx/cache/invalidate/ratings")
                            .param("restaurantId", "123")
                            .param("courierId", "456")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(cxAnalyticsService).invalidateRatingsCache(123L, 456L);
        }

        @Test
        @DisplayName("Should invalidate support ticket cache")
        void shouldInvalidateSupportTicketCache() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/analytics/cx/cache/invalidate/support-tickets")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(cxAnalyticsService).invalidateSupportTicketCache();
        }

        @Test
        @DisplayName("Should invalidate all caches")
        void shouldInvalidateAllCaches() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/analytics/cx/cache/invalidate/all")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(cxAnalyticsService).invalidateAllCaches();
        }
    }
}
