package com.fooddelivery.analytics.cx.controller;

import com.fooddelivery.analytics.cx.dto.*;
import com.fooddelivery.analytics.cx.service.CustomerExperienceAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST controller for Customer Experience Analytics API.
 *
 * Provides endpoints for:
 * - NPS (Net Promoter Score) metrics
 * - Restaurant rating metrics
 * - Courier rating metrics
 * - App store rating metrics
 * - Support ticket metrics
 * - Overall CX summary
 */
@RestController
@RequestMapping("/api/v1/analytics/cx")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Experience Analytics", description = "APIs for customer experience metrics and analytics")
public class CustomerExperienceAnalyticsController {

    private final CustomerExperienceAnalyticsService cxAnalyticsService;

    // ==================== NPS Endpoints ====================

    @GetMapping("/nps")
    @Operation(summary = "Get NPS metrics", description = "Retrieve Net Promoter Score metrics for a date range")
    public ResponseEntity<NpsMetricsDto> getNpsMetrics(
            @Parameter(description = "Start date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @Parameter(description = "End date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,

            @Parameter(description = "Include score distribution (0-10)")
            @RequestParam(defaultValue = "true") boolean includeDistribution,

            @Parameter(description = "Include daily trend data")
            @RequestParam(defaultValue = "false") boolean includeTrend,

            @Parameter(description = "Include segment breakdowns")
            @RequestParam(defaultValue = "false") boolean includeSegments) {

        log.info("NPS metrics requested from {} to {}", startDate, endDate);
        NpsMetricsDto metrics = cxAnalyticsService.getNpsMetrics(
                startDate, endDate, includeDistribution, includeTrend, includeSegments);
        return ResponseEntity.ok(metrics);
    }

    // ==================== Restaurant Rating Endpoints ====================

    @GetMapping("/ratings/restaurant")
    @Operation(summary = "Get overall restaurant ratings", description = "Retrieve overall restaurant rating metrics")
    public ResponseEntity<RestaurantRatingMetricsDto> getOverallRestaurantRatings(
            @Parameter(description = "Start date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @Parameter(description = "End date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,

            @Parameter(description = "Include rating distribution (1-5)")
            @RequestParam(defaultValue = "true") boolean includeDistribution,

            @Parameter(description = "Include daily trend data")
            @RequestParam(defaultValue = "false") boolean includeTrend,

            @Parameter(description = "Include top/bottom performers")
            @RequestParam(defaultValue = "false") boolean includeTopRestaurants) {

        log.info("Overall restaurant ratings requested from {} to {}", startDate, endDate);
        RestaurantRatingMetricsDto metrics = cxAnalyticsService.getRestaurantRatingMetrics(
                null, startDate, endDate, includeDistribution, includeTrend, includeTopRestaurants);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/ratings/restaurant/{restaurantId}")
    @Operation(summary = "Get restaurant ratings by ID", description = "Retrieve rating metrics for a specific restaurant")
    public ResponseEntity<RestaurantRatingMetricsDto> getRestaurantRatings(
            @Parameter(description = "Restaurant ID", required = true)
            @PathVariable Long restaurantId,

            @Parameter(description = "Start date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @Parameter(description = "End date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,

            @Parameter(description = "Include rating distribution (1-5)")
            @RequestParam(defaultValue = "true") boolean includeDistribution,

            @Parameter(description = "Include daily trend data")
            @RequestParam(defaultValue = "false") boolean includeTrend) {

        log.info("Restaurant ratings requested for {} from {} to {}", restaurantId, startDate, endDate);
        RestaurantRatingMetricsDto metrics = cxAnalyticsService.getRestaurantRatingMetrics(
                restaurantId, startDate, endDate, includeDistribution, includeTrend, false);
        return ResponseEntity.ok(metrics);
    }

    // ==================== Courier Rating Endpoints ====================

    @GetMapping("/ratings/courier")
    @Operation(summary = "Get overall courier ratings", description = "Retrieve overall courier rating metrics")
    public ResponseEntity<CourierRatingMetricsDto> getOverallCourierRatings(
            @Parameter(description = "Start date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @Parameter(description = "End date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,

            @Parameter(description = "Include rating distribution (1-5)")
            @RequestParam(defaultValue = "true") boolean includeDistribution,

            @Parameter(description = "Include daily trend data")
            @RequestParam(defaultValue = "false") boolean includeTrend,

            @Parameter(description = "Include top/bottom performers")
            @RequestParam(defaultValue = "false") boolean includeTopCouriers) {

        log.info("Overall courier ratings requested from {} to {}", startDate, endDate);
        CourierRatingMetricsDto metrics = cxAnalyticsService.getCourierRatingMetrics(
                null, startDate, endDate, includeDistribution, includeTrend, includeTopCouriers);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/ratings/courier/{courierId}")
    @Operation(summary = "Get courier ratings by ID", description = "Retrieve rating metrics for a specific courier")
    public ResponseEntity<CourierRatingMetricsDto> getCourierRatings(
            @Parameter(description = "Courier ID", required = true)
            @PathVariable Long courierId,

            @Parameter(description = "Start date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @Parameter(description = "End date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,

            @Parameter(description = "Include rating distribution (1-5)")
            @RequestParam(defaultValue = "true") boolean includeDistribution,

            @Parameter(description = "Include daily trend data")
            @RequestParam(defaultValue = "false") boolean includeTrend) {

        log.info("Courier ratings requested for {} from {} to {}", courierId, startDate, endDate);
        CourierRatingMetricsDto metrics = cxAnalyticsService.getCourierRatingMetrics(
                courierId, startDate, endDate, includeDistribution, includeTrend, false);
        return ResponseEntity.ok(metrics);
    }

    // ==================== App Store Rating Endpoints ====================

    @GetMapping("/ratings/app-store")
    @Operation(summary = "Get app store ratings", description = "Retrieve app store rating metrics (iOS and Android)")
    public ResponseEntity<AppStoreRatingMetricsDto> getAppStoreRatings(
            @Parameter(description = "Start date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @Parameter(description = "End date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,

            @Parameter(description = "Include version breakdown")
            @RequestParam(defaultValue = "false") boolean includeVersionBreakdown,

            @Parameter(description = "Include country breakdown")
            @RequestParam(defaultValue = "false") boolean includeCountryBreakdown,

            @Parameter(description = "Include daily trend data")
            @RequestParam(defaultValue = "false") boolean includeTrend) {

        log.info("App store ratings requested from {} to {}", startDate, endDate);
        AppStoreRatingMetricsDto metrics = cxAnalyticsService.getAppStoreRatingMetrics(
                startDate, endDate, includeVersionBreakdown, includeCountryBreakdown, includeTrend);
        return ResponseEntity.ok(metrics);
    }

    // ==================== Support Ticket Endpoints ====================

    @GetMapping("/support-tickets")
    @Operation(summary = "Get support ticket metrics", description = "Retrieve support ticket metrics with SLA tracking")
    public ResponseEntity<SupportTicketMetricsDto> getSupportTicketMetrics(
            @Parameter(description = "Start date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @Parameter(description = "End date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,

            @Parameter(description = "SLA hours for compliance calculation")
            @RequestParam(defaultValue = "24") int slaHours,

            @Parameter(description = "Include daily trend data")
            @RequestParam(defaultValue = "false") boolean includeTrend,

            @Parameter(description = "Include agent performance breakdown")
            @RequestParam(defaultValue = "false") boolean includeAgentPerformance) {

        log.info("Support ticket metrics requested from {} to {} (SLA: {} hours)", startDate, endDate, slaHours);
        SupportTicketMetricsDto metrics = cxAnalyticsService.getSupportTicketMetrics(
                startDate, endDate, slaHours, includeTrend, includeAgentPerformance);
        return ResponseEntity.ok(metrics);
    }

    // ==================== CX Summary Endpoints ====================

    @GetMapping("/summary")
    @Operation(summary = "Get CX summary", description = "Retrieve comprehensive CX summary with overall score")
    public ResponseEntity<CxSummaryDto> getCxSummary(
            @Parameter(description = "Start date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @Parameter(description = "End date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("CX summary requested from {} to {}", startDate, endDate);
        CxSummaryDto summary = cxAnalyticsService.getCxSummary(startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    // ==================== Cache Management Endpoints ====================

    @PostMapping("/cache/invalidate/nps")
    @Operation(summary = "Invalidate NPS cache", description = "Clear the NPS metrics cache")
    public ResponseEntity<Void> invalidateNpsCache() {
        log.info("NPS cache invalidation requested");
        cxAnalyticsService.invalidateNpsCache();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cache/invalidate/ratings")
    @Operation(summary = "Invalidate ratings cache", description = "Clear the ratings cache")
    public ResponseEntity<Void> invalidateRatingsCache(
            @Parameter(description = "Specific restaurant ID to invalidate")
            @RequestParam(required = false) Long restaurantId,

            @Parameter(description = "Specific courier ID to invalidate")
            @RequestParam(required = false) Long courierId) {

        log.info("Ratings cache invalidation requested (restaurantId={}, courierId={})", restaurantId, courierId);
        cxAnalyticsService.invalidateRatingsCache(restaurantId, courierId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cache/invalidate/support-tickets")
    @Operation(summary = "Invalidate support ticket cache", description = "Clear the support ticket metrics cache")
    public ResponseEntity<Void> invalidateSupportTicketCache() {
        log.info("Support ticket cache invalidation requested");
        cxAnalyticsService.invalidateSupportTicketCache();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cache/invalidate/all")
    @Operation(summary = "Invalidate all CX caches", description = "Clear all CX analytics caches")
    public ResponseEntity<Void> invalidateAllCaches() {
        log.info("All CX caches invalidation requested");
        cxAnalyticsService.invalidateAllCaches();
        return ResponseEntity.noContent().build();
    }
}
