package com.fooddelivery.analytics.cx.service;

import com.fooddelivery.analytics.cx.collector.*;
import com.fooddelivery.analytics.cx.dto.*;
import com.fooddelivery.analytics.cx.mapper.CxMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomerExperienceAnalyticsService.
 */
@ExtendWith(MockitoExtension.class)
class CustomerExperienceAnalyticsServiceTest {

    @Mock
    private NpsCollector npsCollector;

    @Mock
    private RatingCollector ratingCollector;

    @Mock
    private SupportTicketCollector supportTicketCollector;

    @Mock
    private CxMapper cxMapper;

    @InjectMocks
    private CustomerExperienceAnalyticsServiceImpl cxAnalyticsService;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        endDate = LocalDateTime.of(2024, 1, 31, 23, 59);
    }

    @Nested
    @DisplayName("NPS Metrics Tests")
    class NpsMetricsTests {

        @Test
        @DisplayName("Should calculate NPS metrics successfully")
        void shouldCalculateNpsMetrics() {
            // Given
            NpsMetricsDto expectedMetrics = NpsMetricsDto.builder()
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

            when(npsCollector.collect(any(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(expectedMetrics);

            // When
            NpsMetricsDto result = cxAnalyticsService.getNpsMetrics(
                    startDate, endDate, true, false, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getNpsScore()).isEqualTo(45.0);
            assertThat(result.getTotalResponses()).isEqualTo(1000L);
            assertThat(result.getPromotersCount()).isEqualTo(600L);
            assertThat(result.getDetractorsCount()).isEqualTo(150L);

            verify(npsCollector).collect(startDate, endDate, true, false, false);
        }

        @Test
        @DisplayName("Should include NPS distribution when requested")
        void shouldIncludeNpsDistribution() {
            // Given
            Map<Integer, Long> distribution = new HashMap<>();
            for (int i = 0; i <= 10; i++) {
                distribution.put(i, (long) (i * 10));
            }

            NpsMetricsDto expectedMetrics = NpsMetricsDto.builder()
                    .npsScore(30.0)
                    .totalResponses(500L)
                    .scoreDistribution(distribution)
                    .build();

            when(npsCollector.collect(any(), any(), eq(true), anyBoolean(), anyBoolean()))
                    .thenReturn(expectedMetrics);

            // When
            NpsMetricsDto result = cxAnalyticsService.getNpsMetrics(
                    startDate, endDate, true, false, false);

            // Then
            assertThat(result.getScoreDistribution()).isNotNull();
            assertThat(result.getScoreDistribution()).hasSize(11);
        }

        @Test
        @DisplayName("Should handle negative NPS score")
        void shouldHandleNegativeNpsScore() {
            // Given
            NpsMetricsDto expectedMetrics = NpsMetricsDto.builder()
                    .npsScore(-20.0)
                    .totalResponses(100L)
                    .promotersCount(20L)
                    .passivesCount(40L)
                    .detractorsCount(40L)
                    .build();

            when(npsCollector.collect(any(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(expectedMetrics);

            // When
            NpsMetricsDto result = cxAnalyticsService.getNpsMetrics(
                    startDate, endDate, false, false, false);

            // Then
            assertThat(result.getNpsScore()).isEqualTo(-20.0);
        }
    }

    @Nested
    @DisplayName("Restaurant Rating Metrics Tests")
    class RestaurantRatingMetricsTests {

        @Test
        @DisplayName("Should calculate restaurant rating metrics successfully")
        void shouldCalculateRestaurantRatingMetrics() {
            // Given
            Map<Integer, Long> distribution = new HashMap<>();
            distribution.put(1, 10L);
            distribution.put(2, 20L);
            distribution.put(3, 100L);
            distribution.put(4, 300L);
            distribution.put(5, 570L);

            RestaurantRatingMetricsDto expectedMetrics = RestaurantRatingMetricsDto.builder()
                    .restaurantId(123L)
                    .averageRating(4.35)
                    .ratingCount(1000L)
                    .distribution(distribution)
                    .averageFoodQualityRating(4.5)
                    .averagePortionSizeRating(4.2)
                    .averageValueForMoneyRating(4.1)
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(ratingCollector.collectRestaurantRatings(
                    any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(expectedMetrics);

            // When
            RestaurantRatingMetricsDto result = cxAnalyticsService.getRestaurantRatingMetrics(
                    123L, startDate, endDate, true, false, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRestaurantId()).isEqualTo(123L);
            assertThat(result.getAverageRating()).isEqualTo(4.35);
            assertThat(result.getRatingCount()).isEqualTo(1000L);
            assertThat(result.getDistribution()).hasSize(5);

            verify(ratingCollector).collectRestaurantRatings(
                    123L, startDate, endDate, true, false, false);
        }

        @Test
        @DisplayName("Should calculate overall restaurant ratings when no ID provided")
        void shouldCalculateOverallRestaurantRatings() {
            // Given
            RestaurantRatingMetricsDto expectedMetrics = RestaurantRatingMetricsDto.builder()
                    .restaurantId(null)
                    .averageRating(4.2)
                    .ratingCount(50000L)
                    .build();

            when(ratingCollector.collectRestaurantRatings(
                    isNull(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(expectedMetrics);

            // When
            RestaurantRatingMetricsDto result = cxAnalyticsService.getRestaurantRatingMetrics(
                    null, startDate, endDate, false, false, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRestaurantId()).isNull();
            assertThat(result.getRatingCount()).isEqualTo(50000L);
        }
    }

    @Nested
    @DisplayName("Courier Rating Metrics Tests")
    class CourierRatingMetricsTests {

        @Test
        @DisplayName("Should calculate courier rating metrics successfully")
        void shouldCalculateCourierRatingMetrics() {
            // Given
            CourierRatingMetricsDto expectedMetrics = CourierRatingMetricsDto.builder()
                    .courierId(456L)
                    .averageRating(4.8)
                    .ratingCount(500L)
                    .averageProfessionalismRating(4.9)
                    .averageCommunicationRating(4.7)
                    .averageTimelinessRating(4.8)
                    .averageTipAmount(3.50)
                    .tipRate(0.65)
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(ratingCollector.collectCourierRatings(
                    any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(expectedMetrics);

            // When
            CourierRatingMetricsDto result = cxAnalyticsService.getCourierRatingMetrics(
                    456L, startDate, endDate, true, false, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCourierId()).isEqualTo(456L);
            assertThat(result.getAverageRating()).isEqualTo(4.8);
            assertThat(result.getTipRate()).isEqualTo(0.65);

            verify(ratingCollector).collectCourierRatings(
                    456L, startDate, endDate, true, false, false);
        }

        @Test
        @DisplayName("Should include top/bottom couriers when requested")
        void shouldIncludeTopBottomCouriers() {
            // Given
            CourierRatingMetricsDto expectedMetrics = CourierRatingMetricsDto.builder()
                    .averageRating(4.5)
                    .ratingCount(100000L)
                    .build();

            when(ratingCollector.collectCourierRatings(
                    isNull(), any(), any(), anyBoolean(), anyBoolean(), eq(true)))
                    .thenReturn(expectedMetrics);

            // When
            CourierRatingMetricsDto result = cxAnalyticsService.getCourierRatingMetrics(
                    null, startDate, endDate, false, false, true);

            // Then
            assertThat(result).isNotNull();

            verify(ratingCollector).collectCourierRatings(
                    null, startDate, endDate, false, false, true);
        }
    }

    @Nested
    @DisplayName("App Store Rating Metrics Tests")
    class AppStoreRatingMetricsTests {

        @Test
        @DisplayName("Should calculate app store rating metrics successfully")
        void shouldCalculateAppStoreRatingMetrics() {
            // Given
            AppStoreRatingMetricsDto.PlatformRatingDto iosPlatform =
                    AppStoreRatingMetricsDto.PlatformRatingDto.builder()
                            .platform("IOS")
                            .averageRating(4.6)
                            .reviewCount(25000L)
                            .build();

            AppStoreRatingMetricsDto.PlatformRatingDto androidPlatform =
                    AppStoreRatingMetricsDto.PlatformRatingDto.builder()
                            .platform("ANDROID")
                            .averageRating(4.4)
                            .reviewCount(35000L)
                            .build();

            AppStoreRatingMetricsDto expectedMetrics = AppStoreRatingMetricsDto.builder()
                    .overallAverageRating(4.48)
                    .totalReviewCount(60000L)
                    .iosPlatform(iosPlatform)
                    .androidPlatform(androidPlatform)
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(ratingCollector.collectAppStoreRatings(
                    any(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(expectedMetrics);

            // When
            AppStoreRatingMetricsDto result = cxAnalyticsService.getAppStoreRatingMetrics(
                    startDate, endDate, false, false, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOverallAverageRating()).isEqualTo(4.48);
            assertThat(result.getTotalReviewCount()).isEqualTo(60000L);
            assertThat(result.getIosPlatform().getAverageRating()).isEqualTo(4.6);
            assertThat(result.getAndroidPlatform().getAverageRating()).isEqualTo(4.4);

            verify(ratingCollector).collectAppStoreRatings(
                    startDate, endDate, false, false, false);
        }
    }

    @Nested
    @DisplayName("Support Ticket Metrics Tests")
    class SupportTicketMetricsTests {

        @Test
        @DisplayName("Should calculate support ticket metrics successfully")
        void shouldCalculateSupportTicketMetrics() {
            // Given
            Map<String, Long> ticketsByType = new HashMap<>();
            ticketsByType.put("LATE_DELIVERY", 500L);
            ticketsByType.put("MISSING_ITEMS", 300L);
            ticketsByType.put("REFUND_REQUEST", 200L);

            SupportTicketMetricsDto.PerformanceMetricsDto performance =
                    SupportTicketMetricsDto.PerformanceMetricsDto.builder()
                            .avgResolutionTimeHours(4.5)
                            .medianResolutionTimeHours(3.0)
                            .avgFirstResponseTimeMinutes(15.0)
                            .resolutionRate(92.0)
                            .build();

            SupportTicketMetricsDto.SlaMetricsDto slaMetrics =
                    SupportTicketMetricsDto.SlaMetricsDto.builder()
                            .slaHours(24)
                            .slaComplianceRate(95.0)
                            .ticketsBreachedSla(50L)
                            .currentlyBreached(5L)
                            .build();

            SupportTicketMetricsDto expectedMetrics = SupportTicketMetricsDto.builder()
                    .totalTickets(1000L)
                    .openTickets(100L)
                    .inProgressTickets(50L)
                    .resolvedTickets(800L)
                    .closedTickets(50L)
                    .ticketsByType(ticketsByType)
                    .performance(performance)
                    .slaMetrics(slaMetrics)
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();

            when(supportTicketCollector.collect(any(), any(), anyInt(), anyBoolean(), anyBoolean()))
                    .thenReturn(expectedMetrics);

            // When
            SupportTicketMetricsDto result = cxAnalyticsService.getSupportTicketMetrics(
                    startDate, endDate, 24, false, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalTickets()).isEqualTo(1000L);
            assertThat(result.getOpenTickets()).isEqualTo(100L);
            assertThat(result.getTicketsByType()).containsKey("LATE_DELIVERY");
            assertThat(result.getPerformance().getAvgResolutionTimeHours()).isEqualTo(4.5);
            assertThat(result.getSlaMetrics().getSlaComplianceRate()).isEqualTo(95.0);

            verify(supportTicketCollector).collect(startDate, endDate, 24, false, false);
        }

        @Test
        @DisplayName("Should use custom SLA hours")
        void shouldUseCustomSlaHours() {
            // Given
            SupportTicketMetricsDto expectedMetrics = SupportTicketMetricsDto.builder()
                    .totalTickets(500L)
                    .slaMetrics(SupportTicketMetricsDto.SlaMetricsDto.builder()
                            .slaHours(12)
                            .slaComplianceRate(85.0)
                            .build())
                    .build();

            when(supportTicketCollector.collect(any(), any(), eq(12), anyBoolean(), anyBoolean()))
                    .thenReturn(expectedMetrics);

            // When
            SupportTicketMetricsDto result = cxAnalyticsService.getSupportTicketMetrics(
                    startDate, endDate, 12, false, false);

            // Then
            assertThat(result.getSlaMetrics().getSlaHours()).isEqualTo(12);

            verify(supportTicketCollector).collect(startDate, endDate, 12, false, false);
        }
    }

    @Nested
    @DisplayName("CX Summary Tests")
    class CxSummaryTests {

        @Test
        @DisplayName("Should generate CX summary successfully")
        void shouldGenerateCxSummary() {
            // Given
            setupAllCollectorMocks();

            CxSummaryDto.NpsSummary npsSummary = CxSummaryDto.NpsSummary.builder()
                    .npsScore(50.0)
                    .totalResponses(1000L)
                    .build();

            CxSummaryDto.RatingsSummary ratingsSummary = CxSummaryDto.RatingsSummary.builder()
                    .overallAverageRating(4.5)
                    .restaurantAverageRating(4.4)
                    .courierAverageRating(4.6)
                    .build();

            CxSummaryDto.SupportSummary supportSummary = CxSummaryDto.SupportSummary.builder()
                    .totalTickets(500L)
                    .openTickets(50L)
                    .slaComplianceRate(95.0)
                    .build();

            when(cxMapper.toNpsSummary(any())).thenReturn(npsSummary);
            when(cxMapper.toRatingsSummary(any(), any(), any())).thenReturn(ratingsSummary);
            when(cxMapper.toSupportSummary(any())).thenReturn(supportSummary);
            when(cxMapper.calculateOverallCxScore(any(), any(), any())).thenReturn(82.5);
            when(cxMapper.determineCxStatus(82.5)).thenReturn("EXCELLENT");

            // When
            CxSummaryDto result = cxAnalyticsService.getCxSummary(startDate, endDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOverallCxScore()).isEqualTo(82.5);
            assertThat(result.getCxStatus()).isEqualTo("EXCELLENT");
            assertThat(result.getNpsSummary()).isNotNull();
            assertThat(result.getRatingsSummary()).isNotNull();
            assertThat(result.getSupportSummary()).isNotNull();
        }

        private void setupAllCollectorMocks() {
            when(npsCollector.collect(any(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(NpsMetricsDto.builder()
                            .npsScore(50.0)
                            .totalResponses(1000L)
                            .build());

            when(ratingCollector.collectRestaurantRatings(
                    any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(RestaurantRatingMetricsDto.builder()
                            .averageRating(4.4)
                            .ratingCount(10000L)
                            .build());

            when(ratingCollector.collectCourierRatings(
                    any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(CourierRatingMetricsDto.builder()
                            .averageRating(4.6)
                            .ratingCount(8000L)
                            .build());

            when(ratingCollector.collectAppStoreRatings(
                    any(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(AppStoreRatingMetricsDto.builder()
                            .overallAverageRating(4.5)
                            .totalReviewCount(50000L)
                            .build());

            when(supportTicketCollector.collect(any(), any(), anyInt(), anyBoolean(), anyBoolean()))
                    .thenReturn(SupportTicketMetricsDto.builder()
                            .totalTickets(500L)
                            .openTickets(50L)
                            .build());
        }
    }

    @Nested
    @DisplayName("Date Validation Tests")
    class DateValidationTests {

        @Test
        @DisplayName("Should reject null start date")
        void shouldRejectNullStartDate() {
            assertThatThrownBy(() ->
                    cxAnalyticsService.getNpsMetrics(null, endDate, false, false, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("required");
        }

        @Test
        @DisplayName("Should reject null end date")
        void shouldRejectNullEndDate() {
            assertThatThrownBy(() ->
                    cxAnalyticsService.getNpsMetrics(startDate, null, false, false, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("required");
        }

        @Test
        @DisplayName("Should reject start date after end date")
        void shouldRejectStartDateAfterEndDate() {
            LocalDateTime laterDate = endDate.plusDays(10);
            assertThatThrownBy(() ->
                    cxAnalyticsService.getNpsMetrics(laterDate, endDate, false, false, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be after");
        }

        @Test
        @DisplayName("Should reject future end date")
        void shouldRejectFutureEndDate() {
            LocalDateTime futureDate = LocalDateTime.now().plusDays(10);
            assertThatThrownBy(() ->
                    cxAnalyticsService.getNpsMetrics(startDate, futureDate, false, false, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("future");
        }
    }

    @Nested
    @DisplayName("Cache Invalidation Tests")
    class CacheInvalidationTests {

        @Test
        @DisplayName("Should invalidate NPS cache")
        void shouldInvalidateNpsCache() {
            // When
            cxAnalyticsService.invalidateNpsCache();

            // Then - no exception thrown indicates success
            // Cache eviction is handled by Spring annotations
        }

        @Test
        @DisplayName("Should invalidate ratings cache")
        void shouldInvalidateRatingsCache() {
            // When
            cxAnalyticsService.invalidateRatingsCache(123L, 456L);

            // Then - no exception thrown indicates success
        }

        @Test
        @DisplayName("Should invalidate support ticket cache")
        void shouldInvalidateSupportTicketCache() {
            // When
            cxAnalyticsService.invalidateSupportTicketCache();

            // Then - no exception thrown indicates success
        }

        @Test
        @DisplayName("Should invalidate all caches")
        void shouldInvalidateAllCaches() {
            // When
            cxAnalyticsService.invalidateAllCaches();

            // Then - no exception thrown indicates success
        }
    }
}
