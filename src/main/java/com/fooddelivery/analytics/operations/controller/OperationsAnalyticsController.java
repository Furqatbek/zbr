package com.fooddelivery.analytics.operations.controller;

import com.fooddelivery.analytics.operations.dto.*;
import com.fooddelivery.analytics.operations.service.OperationsAnalyticsService;
import com.fooddelivery.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.PastOrPresent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST controller for operations analytics endpoints.
 *
 * <p>All endpoints require ADMIN or PLATFORM role.</p>
 *
 * <p>Available Endpoints:</p>
 * <ul>
 *   <li>GET /api/v1/analytics/operations/order/{orderId} - Order fulfillment metrics</li>
 *   <li>GET /api/v1/analytics/operations/restaurant/{restaurantId} - Restaurant performance</li>
 *   <li>GET /api/v1/analytics/operations/courier/{courierId} - Courier performance</li>
 *   <li>GET /api/v1/analytics/operations/delivery-success - Delivery success rate</li>
 *   <li>GET /api/v1/analytics/operations/eta-accuracy - ETA accuracy metrics</li>
 *   <li>GET /api/v1/analytics/operations/summary - Operations summary</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/analytics/operations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Operations Analytics", description = "Operational metrics for orders, restaurants, and couriers")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM')")
public class OperationsAnalyticsController {

    private final OperationsAnalyticsService analyticsService;

    /**
     * Get order fulfillment metrics for a specific order.
     *
     * @param orderId The order ID
     * @return Order fulfillment timing breakdown
     */
    @GetMapping("/order/{orderId}")
    @Operation(
            summary = "Get order fulfillment metrics",
            description = "Returns detailed timing breakdown for an order: acceptance time, " +
                    "preparation time, pickup wait time, and delivery time."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Order fulfillment metrics retrieved",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Order fulfillment metrics retrieved",
                                      "data": {
                                        "orderId": 12345,
                                        "totalFulfillmentTimeMinutes": 41,
                                        "acceptanceTimeMinutes": 3,
                                        "preparationTimeMinutes": 15,
                                        "pickupWaitTimeMinutes": 8,
                                        "deliveryTimeMinutes": 15,
                                        "onTime": true
                                      }
                                    }
                                    """)
                    )
            )
    })
    public ResponseEntity<ApiResponse<OrderFulfillmentMetricsDto>> getOrderFulfillmentMetrics(
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        log.info("GET /api/v1/analytics/operations/order/{} - Fetching order fulfillment metrics", orderId);
        OrderFulfillmentMetricsDto metrics = analyticsService.getOrderFulfillmentMetrics(orderId);
        return ResponseEntity.ok(ApiResponse.success("Order fulfillment metrics retrieved", metrics));
    }

    /**
     * Get restaurant performance metrics for a date range.
     *
     * @param restaurantId The restaurant ID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Restaurant performance metrics
     */
    @GetMapping("/restaurant/{restaurantId}")
    @Operation(
            summary = "Get restaurant performance metrics",
            description = "Returns restaurant performance metrics including order acceptance rate, " +
                    "average preparation time, offline time, and menu update frequency."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Restaurant performance metrics retrieved",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Restaurant performance metrics retrieved",
                                      "data": {
                                        "restaurantId": 1,
                                        "restaurantName": "Pizza Palace",
                                        "orderAcceptanceRate": 95.5,
                                        "averageAcceptanceTimeMinutes": 2.5,
                                        "averagePreparationTimeMinutes": 18.3,
                                        "uptimePercentage": 98.5,
                                        "menuUpdatesPerWeek": 3.2
                                      }
                                    }
                                    """)
                    )
            )
    })
    public ResponseEntity<ApiResponse<RestaurantPerformanceDto>> getRestaurantPerformanceMetrics(
            @Parameter(description = "Restaurant ID") @PathVariable Long restaurantId,
            @Parameter(description = "Start date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @PastOrPresent LocalDate startDate,
            @Parameter(description = "End date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/v1/analytics/operations/restaurant/{} - Fetching restaurant metrics from {} to {}",
                restaurantId, startDate, endDate);

        validateDateRange(startDate, endDate);
        RestaurantPerformanceDto metrics = analyticsService.getRestaurantPerformanceMetrics(restaurantId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Restaurant performance metrics retrieved", metrics));
    }

    /**
     * Get courier performance metrics for a date range.
     *
     * @param courierId The courier ID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Courier performance metrics
     */
    @GetMapping("/courier/{courierId}")
    @Operation(
            summary = "Get courier performance metrics",
            description = "Returns courier performance metrics including acceptance rate, " +
                    "delivery time, availability, and location update frequency."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Courier performance metrics retrieved",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Courier performance metrics retrieved",
                                      "data": {
                                        "courierId": 1,
                                        "courierName": "John Doe",
                                        "acceptanceRate": 87.5,
                                        "averageDeliveryTimeMinutes": 12.5,
                                        "deliveriesCompleted": 45,
                                        "utilizationRate": 72.3,
                                        "locationUpdatesPerHour": 120.5
                                      }
                                    }
                                    """)
                    )
            )
    })
    public ResponseEntity<ApiResponse<CourierPerformanceDto>> getCourierPerformanceMetrics(
            @Parameter(description = "Courier ID") @PathVariable Long courierId,
            @Parameter(description = "Start date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @PastOrPresent LocalDate startDate,
            @Parameter(description = "End date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/v1/analytics/operations/courier/{} - Fetching courier metrics from {} to {}",
                courierId, startDate, endDate);

        validateDateRange(startDate, endDate);
        CourierPerformanceDto metrics = analyticsService.getCourierPerformanceMetrics(courierId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Courier performance metrics retrieved", metrics));
    }

    /**
     * Get delivery success rate metrics for a date range.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Delivery success rate metrics
     */
    @GetMapping("/delivery-success")
    @Operation(
            summary = "Get delivery success rate",
            description = "Returns delivery success rate including completion rate, " +
                    "cancellation breakdown, and delay analysis."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Delivery success rate retrieved",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Delivery success rate retrieved",
                                      "data": {
                                        "totalOrders": 1250,
                                        "successfulDeliveries": 1182,
                                        "successRate": 94.56,
                                        "cancellationRate": 4.16,
                                        "onTimeRate": 89.2,
                                        "majorDelayRate": 2.8
                                      }
                                    }
                                    """)
                    )
            )
    })
    public ResponseEntity<ApiResponse<DeliverySuccessRateDto>> getDeliverySuccessRate(
            @Parameter(description = "Start date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @PastOrPresent LocalDate startDate,
            @Parameter(description = "End date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/v1/analytics/operations/delivery-success - Fetching delivery success rate from {} to {}",
                startDate, endDate);

        validateDateRange(startDate, endDate);
        DeliverySuccessRateDto metrics = analyticsService.getDeliverySuccessRate(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Delivery success rate retrieved", metrics));
    }

    /**
     * Get ETA accuracy metrics for a date range.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return ETA accuracy metrics
     */
    @GetMapping("/eta-accuracy")
    @Operation(
            summary = "Get ETA accuracy metrics",
            description = "Returns ETA accuracy metrics including average error, " +
                    "over/under estimation rates, and accuracy by time period."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "ETA accuracy metrics retrieved",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "ETA accuracy metrics retrieved",
                                      "data": {
                                        "totalDeliveries": 1182,
                                        "averageEtaErrorMinutes": 2.5,
                                        "averageAbsoluteErrorMinutes": 5.3,
                                        "accuracyRate5Min": 72.5,
                                        "overEstimationCount": 423,
                                        "underEstimationCount": 759,
                                        "overEstimationRate": 35.8,
                                        "underEstimationRate": 64.2
                                      }
                                    }
                                    """)
                    )
            )
    })
    public ResponseEntity<ApiResponse<EtaAccuracyDto>> getEtaAccuracyMetrics(
            @Parameter(description = "Start date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @PastOrPresent LocalDate startDate,
            @Parameter(description = "End date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/v1/analytics/operations/eta-accuracy - Fetching ETA accuracy from {} to {}",
                startDate, endDate);

        validateDateRange(startDate, endDate);
        EtaAccuracyDto metrics = analyticsService.getEtaAccuracyMetrics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("ETA accuracy metrics retrieved", metrics));
    }

    /**
     * Get operations summary for a date range.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Operations summary
     */
    @GetMapping("/summary")
    @Operation(
            summary = "Get operations summary",
            description = "Returns a high-level summary of all operational metrics " +
                    "including fulfillment times, success rates, and ETA accuracy."
    )
    public ResponseEntity<ApiResponse<OperationsSummaryDto>> getOperationsSummary(
            @Parameter(description = "Start date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @PastOrPresent LocalDate startDate,
            @Parameter(description = "End date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/v1/analytics/operations/summary - Fetching operations summary from {} to {}",
                startDate, endDate);

        validateDateRange(startDate, endDate);
        OperationsSummaryDto summary = analyticsService.getOperationsSummary(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Operations summary retrieved", summary));
    }

    /**
     * Force refresh all operations analytics caches.
     *
     * @return Success message
     */
    @PostMapping("/cache/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Refresh operations cache",
            description = "Evicts all operations analytics caches to force fresh data on next request. " +
                    "Requires ADMIN role."
    )
    public ResponseEntity<ApiResponse<Void>> refreshCache() {
        log.info("POST /api/v1/analytics/operations/cache/refresh - Refreshing operations cache");
        analyticsService.evictAllCaches();
        return ResponseEntity.ok(ApiResponse.success("Operations analytics cache refreshed"));
    }

    /**
     * Refresh a specific operations cache.
     *
     * @param cacheName Name of the cache to refresh
     * @return Success message
     */
    @PostMapping("/cache/refresh/{cacheName}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Refresh specific cache",
            description = "Evicts a specific operations analytics cache. Available caches: " +
                    "ops:order_fulfillment, ops:restaurant_performance, ops:courier_performance, " +
                    "ops:delivery_success, ops:eta_accuracy, ops:summary"
    )
    public ResponseEntity<ApiResponse<Void>> refreshSpecificCache(
            @PathVariable String cacheName) {
        log.info("POST /api/v1/analytics/operations/cache/refresh/{} - Refreshing specific cache", cacheName);
        analyticsService.evictCache("ops:" + cacheName);
        return ResponseEntity.ok(ApiResponse.success("Cache '" + cacheName + "' refreshed"));
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        if (startDate.isBefore(LocalDate.now().minusYears(1))) {
            throw new IllegalArgumentException("Start date cannot be more than 1 year in the past");
        }
    }
}
