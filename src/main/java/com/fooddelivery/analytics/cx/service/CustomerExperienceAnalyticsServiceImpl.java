package com.fooddelivery.analytics.cx.service;

import com.fooddelivery.analytics.cx.collector.NpsCollector;
import com.fooddelivery.analytics.cx.collector.RatingCollector;
import com.fooddelivery.analytics.cx.collector.SupportTicketCollector;
import com.fooddelivery.analytics.cx.dto.*;
import com.fooddelivery.analytics.cx.mapper.CxMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of Customer Experience Analytics Service.
 * Provides cached access to CX metrics with configurable TTLs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CustomerExperienceAnalyticsServiceImpl implements CustomerExperienceAnalyticsService {

    private final NpsCollector npsCollector;
    private final RatingCollector ratingCollector;
    private final SupportTicketCollector supportTicketCollector;
    private final CxMapper cxMapper;

    // Cache names
    public static final String NPS_CACHE = "cx-nps-metrics";
    public static final String RESTAURANT_RATING_CACHE = "cx-restaurant-ratings";
    public static final String COURIER_RATING_CACHE = "cx-courier-ratings";
    public static final String APP_STORE_RATING_CACHE = "cx-app-store-ratings";
    public static final String SUPPORT_TICKET_CACHE = "cx-support-tickets";
    public static final String CX_SUMMARY_CACHE = "cx-summary";

    @Override
    @Cacheable(value = NPS_CACHE,
            key = "'nps:' + #startDate.toString() + ':' + #endDate.toString() + ':' + #includeDistribution + ':' + #includeTrend + ':' + #includeSegments",
            unless = "#result == null")
    public NpsMetricsDto getNpsMetrics(LocalDateTime startDate, LocalDateTime endDate,
                                       boolean includeDistribution, boolean includeTrend,
                                       boolean includeSegments) {
        log.debug("Fetching NPS metrics from {} to {} (cache miss)", startDate, endDate);
        validateDateRange(startDate, endDate);

        return npsCollector.collect(startDate, endDate, includeDistribution, includeTrend, includeSegments);
    }

    @Override
    @Cacheable(value = RESTAURANT_RATING_CACHE,
            key = "'restaurant:' + (#restaurantId != null ? #restaurantId : 'all') + ':' + #startDate.toString() + ':' + #endDate.toString() + ':' + #includeDistribution + ':' + #includeTrend + ':' + #includeTopRestaurants",
            unless = "#result == null")
    public RestaurantRatingMetricsDto getRestaurantRatingMetrics(Long restaurantId,
                                                                  LocalDateTime startDate,
                                                                  LocalDateTime endDate,
                                                                  boolean includeDistribution,
                                                                  boolean includeTrend,
                                                                  boolean includeTopRestaurants) {
        log.debug("Fetching restaurant rating metrics for {} from {} to {} (cache miss)",
                restaurantId != null ? restaurantId : "all", startDate, endDate);
        validateDateRange(startDate, endDate);

        return ratingCollector.collectRestaurantRatings(restaurantId, startDate, endDate,
                includeDistribution, includeTrend, includeTopRestaurants);
    }

    @Override
    @Cacheable(value = COURIER_RATING_CACHE,
            key = "'courier:' + (#courierId != null ? #courierId : 'all') + ':' + #startDate.toString() + ':' + #endDate.toString() + ':' + #includeDistribution + ':' + #includeTrend + ':' + #includeTopCouriers",
            unless = "#result == null")
    public CourierRatingMetricsDto getCourierRatingMetrics(Long courierId,
                                                           LocalDateTime startDate,
                                                           LocalDateTime endDate,
                                                           boolean includeDistribution,
                                                           boolean includeTrend,
                                                           boolean includeTopCouriers) {
        log.debug("Fetching courier rating metrics for {} from {} to {} (cache miss)",
                courierId != null ? courierId : "all", startDate, endDate);
        validateDateRange(startDate, endDate);

        return ratingCollector.collectCourierRatings(courierId, startDate, endDate,
                includeDistribution, includeTrend, includeTopCouriers);
    }

    @Override
    @Cacheable(value = APP_STORE_RATING_CACHE,
            key = "'appstore:' + #startDate.toString() + ':' + #endDate.toString() + ':' + #includeVersionBreakdown + ':' + #includeCountryBreakdown + ':' + #includeTrend",
            unless = "#result == null")
    public AppStoreRatingMetricsDto getAppStoreRatingMetrics(LocalDateTime startDate,
                                                              LocalDateTime endDate,
                                                              boolean includeVersionBreakdown,
                                                              boolean includeCountryBreakdown,
                                                              boolean includeTrend) {
        log.debug("Fetching app store rating metrics from {} to {} (cache miss)", startDate, endDate);
        validateDateRange(startDate, endDate);

        return ratingCollector.collectAppStoreRatings(startDate, endDate,
                includeVersionBreakdown, includeCountryBreakdown, includeTrend);
    }

    @Override
    @Cacheable(value = SUPPORT_TICKET_CACHE,
            key = "'tickets:' + #startDate.toString() + ':' + #endDate.toString() + ':' + #slaHours + ':' + #includeTrend + ':' + #includeAgentPerformance",
            unless = "#result == null")
    public SupportTicketMetricsDto getSupportTicketMetrics(LocalDateTime startDate,
                                                           LocalDateTime endDate,
                                                           int slaHours,
                                                           boolean includeTrend,
                                                           boolean includeAgentPerformance) {
        log.debug("Fetching support ticket metrics from {} to {} (cache miss)", startDate, endDate);
        validateDateRange(startDate, endDate);

        return supportTicketCollector.collect(startDate, endDate, slaHours, includeTrend, includeAgentPerformance);
    }

    @Override
    @Cacheable(value = CX_SUMMARY_CACHE,
            key = "'summary:' + #startDate.toString() + ':' + #endDate.toString()",
            unless = "#result == null")
    public CxSummaryDto getCxSummary(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching CX summary from {} to {} (cache miss)", startDate, endDate);
        validateDateRange(startDate, endDate);

        // Collect all metrics
        NpsMetricsDto npsMetrics = npsCollector.collect(startDate, endDate, false, false, false);
        RestaurantRatingMetricsDto restaurantMetrics = ratingCollector.collectRestaurantRatings(
                null, startDate, endDate, false, false, false);
        CourierRatingMetricsDto courierMetrics = ratingCollector.collectCourierRatings(
                null, startDate, endDate, false, false, false);
        AppStoreRatingMetricsDto appStoreMetrics = ratingCollector.collectAppStoreRatings(
                startDate, endDate, false, false, false);
        SupportTicketMetricsDto ticketMetrics = supportTicketCollector.collect(
                startDate, endDate, 24, false, false);

        // Map to summaries
        CxSummaryDto.NpsSummary npsSummary = cxMapper.toNpsSummary(npsMetrics);
        CxSummaryDto.RatingsSummary ratingsSummary = cxMapper.toRatingsSummary(
                restaurantMetrics, courierMetrics, appStoreMetrics);
        CxSummaryDto.SupportSummary supportSummary = cxMapper.toSupportSummary(ticketMetrics);

        // Calculate overall CX score
        Double cxScore = cxMapper.calculateOverallCxScore(npsSummary, ratingsSummary, supportSummary);
        String cxStatus = cxMapper.determineCxStatus(cxScore);

        return CxSummaryDto.builder()
                .overallCxScore(cxScore)
                .cxStatus(cxStatus)
                .npsSummary(npsSummary)
                .ratingsSummary(ratingsSummary)
                .supportSummary(supportSummary)
                .periodStart(startDate)
                .periodEnd(endDate)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @CacheEvict(value = NPS_CACHE, allEntries = true)
    public void invalidateNpsCache() {
        log.info("NPS metrics cache invalidated");
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = RESTAURANT_RATING_CACHE, allEntries = true),
            @CacheEvict(value = COURIER_RATING_CACHE, allEntries = true),
            @CacheEvict(value = APP_STORE_RATING_CACHE, allEntries = true)
    })
    public void invalidateRatingsCache(Long restaurantId, Long courierId) {
        log.info("Ratings cache invalidated (restaurantId={}, courierId={})", restaurantId, courierId);
    }

    @Override
    @CacheEvict(value = SUPPORT_TICKET_CACHE, allEntries = true)
    public void invalidateSupportTicketCache() {
        log.info("Support ticket metrics cache invalidated");
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = NPS_CACHE, allEntries = true),
            @CacheEvict(value = RESTAURANT_RATING_CACHE, allEntries = true),
            @CacheEvict(value = COURIER_RATING_CACHE, allEntries = true),
            @CacheEvict(value = APP_STORE_RATING_CACHE, allEntries = true),
            @CacheEvict(value = SUPPORT_TICKET_CACHE, allEntries = true),
            @CacheEvict(value = CX_SUMMARY_CACHE, allEntries = true)
    })
    public void invalidateAllCaches() {
        log.info("All CX analytics caches invalidated");
    }

    /**
     * Validate date range parameters.
     */
    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        if (endDate.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("End date cannot be in the future");
        }
    }
}
