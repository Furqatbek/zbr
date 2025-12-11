package com.fooddelivery.analytics.cx.service;

import com.fooddelivery.analytics.cx.dto.*;

import java.time.LocalDateTime;

/**
 * Service interface for Customer Experience Analytics.
 * Provides methods to retrieve and analyze CX metrics including NPS,
 * ratings (restaurant, courier, app store), and support tickets.
 */
public interface CustomerExperienceAnalyticsService {

    /**
     * Get NPS (Net Promoter Score) metrics for a date range.
     *
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @param includeDistribution Whether to include score distribution
     * @param includeTrend Whether to include daily trend data
     * @param includeSegments Whether to include segment breakdowns
     * @return NPS metrics DTO
     */
    NpsMetricsDto getNpsMetrics(LocalDateTime startDate, LocalDateTime endDate,
                                boolean includeDistribution, boolean includeTrend,
                                boolean includeSegments);

    /**
     * Get restaurant rating metrics for a specific restaurant.
     *
     * @param restaurantId Restaurant ID (null for overall metrics)
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @param includeDistribution Whether to include rating distribution
     * @param includeTrend Whether to include daily trend data
     * @param includeTopRestaurants Whether to include top/bottom performers
     * @return Restaurant rating metrics DTO
     */
    RestaurantRatingMetricsDto getRestaurantRatingMetrics(Long restaurantId,
                                                          LocalDateTime startDate,
                                                          LocalDateTime endDate,
                                                          boolean includeDistribution,
                                                          boolean includeTrend,
                                                          boolean includeTopRestaurants);

    /**
     * Get courier rating metrics for a specific courier.
     *
     * @param courierId Courier ID (null for overall metrics)
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @param includeDistribution Whether to include rating distribution
     * @param includeTrend Whether to include daily trend data
     * @param includeTopCouriers Whether to include top/bottom performers
     * @return Courier rating metrics DTO
     */
    CourierRatingMetricsDto getCourierRatingMetrics(Long courierId,
                                                    LocalDateTime startDate,
                                                    LocalDateTime endDate,
                                                    boolean includeDistribution,
                                                    boolean includeTrend,
                                                    boolean includeTopCouriers);

    /**
     * Get app store rating metrics.
     *
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @param includeVersionBreakdown Whether to include version breakdown
     * @param includeCountryBreakdown Whether to include country breakdown
     * @param includeTrend Whether to include daily trend data
     * @return App store rating metrics DTO
     */
    AppStoreRatingMetricsDto getAppStoreRatingMetrics(LocalDateTime startDate,
                                                      LocalDateTime endDate,
                                                      boolean includeVersionBreakdown,
                                                      boolean includeCountryBreakdown,
                                                      boolean includeTrend);

    /**
     * Get support ticket metrics.
     *
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @param slaHours SLA hours for compliance calculation
     * @param includeTrend Whether to include daily trend data
     * @param includeAgentPerformance Whether to include agent performance breakdown
     * @return Support ticket metrics DTO
     */
    SupportTicketMetricsDto getSupportTicketMetrics(LocalDateTime startDate,
                                                    LocalDateTime endDate,
                                                    int slaHours,
                                                    boolean includeTrend,
                                                    boolean includeAgentPerformance);

    /**
     * Get a comprehensive CX summary combining all metrics.
     *
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @return CX summary DTO with overall score and breakdown
     */
    CxSummaryDto getCxSummary(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Invalidate NPS metrics cache.
     */
    void invalidateNpsCache();

    /**
     * Invalidate ratings cache.
     *
     * @param restaurantId Optional restaurant ID to invalidate specific cache
     * @param courierId Optional courier ID to invalidate specific cache
     */
    void invalidateRatingsCache(Long restaurantId, Long courierId);

    /**
     * Invalidate support ticket cache.
     */
    void invalidateSupportTicketCache();

    /**
     * Invalidate all CX caches.
     */
    void invalidateAllCaches();
}
