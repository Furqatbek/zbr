package com.fooddelivery.analytics.controller;

import com.fooddelivery.analytics.dto.*;
import com.fooddelivery.analytics.service.AnalyticsService;
import com.fooddelivery.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for business analytics endpoints.
 *
 * <p>All endpoints require ADMIN or PLATFORM role.</p>
 *
 * <p>Available Endpoints:</p>
 * <ul>
 *   <li>GET /api/v1/analytics/activity - User activity metrics (DAU/WAU/MAU)</li>
 *   <li>GET /api/v1/analytics/orders - Order volume metrics</li>
 *   <li>GET /api/v1/analytics/conversion - Conversion funnel metrics</li>
 *   <li>GET /api/v1/analytics/aov - Average Order Value metrics</li>
 *   <li>GET /api/v1/analytics/activation - User activation metrics</li>
 *   <li>GET /api/v1/analytics/churn - Churn metrics</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics", description = "Business analytics and metrics endpoints")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Get user activity metrics (DAU/WAU/MAU).
     *
     * @return DTO containing daily, weekly, and monthly active users with ratios
     */
    @GetMapping("/activity")
    @Operation(
            summary = "Get user activity metrics",
            description = "Returns Daily Active Users (DAU), Weekly Active Users (WAU), " +
                    "Monthly Active Users (MAU), and related ratios like DAU/MAU stickiness."
    )
    public ResponseEntity<ApiResponse<DauWauMauDto>> getUserActivityMetrics() {
        log.info("GET /api/v1/analytics/activity - Fetching user activity metrics");
        DauWauMauDto metrics = analyticsService.getUserActivityMetrics();
        return ResponseEntity.ok(ApiResponse.success("User activity metrics retrieved", metrics));
    }

    /**
     * Get order volume metrics.
     *
     * @return DTO containing orders per day, per hour, first vs repeat orders, and trends
     */
    @GetMapping("/orders")
    @Operation(
            summary = "Get order volume metrics",
            description = "Returns order counts by period (day/week/month), hourly distribution, " +
                    "peak hours, first-time vs repeat orders, and success/cancellation rates."
    )
    public ResponseEntity<ApiResponse<OrderVolumeDto>> getOrderVolumeMetrics() {
        log.info("GET /api/v1/analytics/orders - Fetching order volume metrics");
        OrderVolumeDto metrics = analyticsService.getOrderVolumeMetrics();
        return ResponseEntity.ok(ApiResponse.success("Order volume metrics retrieved", metrics));
    }

    /**
     * Get conversion funnel metrics.
     *
     * @param days Number of days to analyze (default: 1)
     * @return DTO containing conversion rates at each funnel step
     */
    @GetMapping("/conversion")
    @Operation(
            summary = "Get conversion funnel metrics",
            description = "Returns conversion rates through the funnel: " +
                    "Restaurant View → Add to Cart → Checkout → Payment. " +
                    "Includes drop-off analysis at each step."
    )
    public ResponseEntity<ApiResponse<ConversionRateDto>> getConversionMetrics(
            @Parameter(description = "Number of days to analyze (1-30)")
            @RequestParam(defaultValue = "1") int days) {
        log.info("GET /api/v1/analytics/conversion - Fetching conversion metrics for {} days", days);
        if (days < 1) days = 1;
        if (days > 30) days = 30;
        ConversionRateDto metrics = analyticsService.getConversionMetrics(days);
        return ResponseEntity.ok(ApiResponse.success("Conversion metrics retrieved", metrics));
    }

    /**
     * Get Average Order Value (AOV) metrics.
     *
     * @return DTO containing AOV by period, revenue, and order value distribution
     */
    @GetMapping("/aov")
    @Operation(
            summary = "Get Average Order Value metrics",
            description = "Returns AOV for today/week/month, total revenue, " +
                    "min/max/median order values, average items per order, and daily trends."
    )
    public ResponseEntity<ApiResponse<AovDto>> getAovMetrics() {
        log.info("GET /api/v1/analytics/aov - Fetching AOV metrics");
        AovDto metrics = analyticsService.getAovMetrics();
        return ResponseEntity.ok(ApiResponse.success("AOV metrics retrieved", metrics));
    }

    /**
     * Get user activation metrics.
     *
     * @return DTO containing first delivery counts, activation time, and referral usage
     */
    @GetMapping("/activation")
    @Operation(
            summary = "Get user activation metrics",
            description = "Returns first delivery completion counts, average time to first order, " +
                    "referral signup rates, and activation time distribution."
    )
    public ResponseEntity<ApiResponse<ActivationMetricsDto>> getActivationMetrics() {
        log.info("GET /api/v1/analytics/activation - Fetching activation metrics");
        ActivationMetricsDto metrics = analyticsService.getUserActivationMetrics();
        return ResponseEntity.ok(ApiResponse.success("Activation metrics retrieved", metrics));
    }

    /**
     * Get churn metrics for users, restaurants, and couriers.
     *
     * @return DTO containing churn rates and at-risk counts for all entities
     */
    @GetMapping("/churn")
    @Operation(
            summary = "Get churn metrics",
            description = "Returns churn rates and counts for users (30-day inactivity), " +
                    "restaurants (zero orders in 30 days), and couriers (14-day inactivity). " +
                    "Includes at-risk entities and retention cohort analysis."
    )
    public ResponseEntity<ApiResponse<ChurnDto>> getChurnMetrics() {
        log.info("GET /api/v1/analytics/churn - Fetching churn metrics");
        ChurnDto metrics = analyticsService.getChurnMetrics();
        return ResponseEntity.ok(ApiResponse.success("Churn metrics retrieved", metrics));
    }

    /**
     * Get a summary of all key metrics.
     *
     * @return Combined DTO with key metrics from all categories
     */
    @GetMapping("/summary")
    @Operation(
            summary = "Get analytics summary",
            description = "Returns a quick summary of key metrics from all categories: " +
                    "DAU, orders today, conversion rate, AOV, and churn rates."
    )
    public ResponseEntity<ApiResponse<AnalyticsSummaryDto>> getAnalyticsSummary() {
        log.info("GET /api/v1/analytics/summary - Fetching analytics summary");

        AnalyticsSummaryDto summary = AnalyticsSummaryDto.builder()
                .dailyActiveUsers(analyticsService.getDailyActiveUsers())
                .ordersToday(analyticsService.getOrdersPerDay())
                .conversionRate(analyticsService.getConversionRate())
                .averageOrderValue(analyticsService.getAverageOrderValue())
                .userChurnRate(analyticsService.getUserChurnRate())
                .restaurantChurnRate(analyticsService.getRestaurantChurnRate())
                .courierChurnRate(analyticsService.getCourierChurnRate())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Analytics summary retrieved", summary));
    }

    /**
     * Force refresh all analytics caches.
     *
     * @return Success message
     */
    @PostMapping("/cache/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Refresh analytics cache",
            description = "Evicts all analytics caches to force fresh data on next request. " +
                    "Requires ADMIN role."
    )
    public ResponseEntity<ApiResponse<Void>> refreshCache() {
        log.info("POST /api/v1/analytics/cache/refresh - Refreshing analytics cache");
        analyticsService.evictAllCaches();
        return ResponseEntity.ok(ApiResponse.success("Analytics cache refreshed"));
    }

    /**
     * Refresh a specific analytics cache.
     *
     * @param cacheName Name of the cache to refresh
     * @return Success message
     */
    @PostMapping("/cache/refresh/{cacheName}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Refresh specific cache",
            description = "Evicts a specific analytics cache. Available caches: " +
                    "analytics:dau_wau_mau, analytics:order_volume, analytics:conversion, " +
                    "analytics:aov, analytics:activation, analytics:churn"
    )
    public ResponseEntity<ApiResponse<Void>> refreshSpecificCache(
            @PathVariable String cacheName) {
        log.info("POST /api/v1/analytics/cache/refresh/{} - Refreshing specific cache", cacheName);
        analyticsService.evictCache("analytics:" + cacheName);
        return ResponseEntity.ok(ApiResponse.success("Cache '" + cacheName + "' refreshed"));
    }
}
