package com.fooddelivery.analytics.financial.controller;

import com.fooddelivery.analytics.financial.dto.*;
import com.fooddelivery.analytics.financial.service.FinancialAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for financial analytics endpoints.
 *
 * Provides APIs for:
 * - GMV (Gross Merchandise Value) metrics
 * - Commission revenue metrics
 * - Delivery fee metrics
 * - Promotion and discount metrics
 * - Restaurant payout metrics
 * - Courier payout metrics
 * - Contribution margin and unit economics
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/analytics/financial")
@RequiredArgsConstructor
@Tag(name = "Financial Analytics", description = "Financial metrics and analytics APIs")
@PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM', 'FINANCE_MANAGER')")
public class FinancialAnalyticsController {

    private final FinancialAnalyticsService financialAnalyticsService;

    @PostMapping("/gmv")
    @Operation(summary = "Calculate GMV metrics",
               description = "Calculate Gross Merchandise Value metrics for the specified period. " +
                           "GMV = Sum of all order values before deductions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "GMV metrics calculated successfully",
                    content = @Content(schema = @Schema(implementation = GmvMetricsDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<GmvMetricsDto> getGmvMetrics(
            @Valid @RequestBody FinancialMetricsRequest request) {
        log.info("Calculating GMV metrics for period: {} to {}", request.getStartDate(), request.getEndDate());
        return ResponseEntity.ok(financialAnalyticsService.calculateGmvMetrics(request));
    }

    @PostMapping("/commission")
    @Operation(summary = "Calculate commission revenue metrics",
               description = "Calculate commission revenue metrics for the specified period. " +
                           "Commission = Platform fee charged to restaurants per order.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Commission metrics calculated successfully",
                    content = @Content(schema = @Schema(implementation = CommissionMetricsDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<CommissionMetricsDto> getCommissionMetrics(
            @Valid @RequestBody FinancialMetricsRequest request) {
        log.info("Calculating commission metrics for period: {} to {}", request.getStartDate(), request.getEndDate());
        return ResponseEntity.ok(financialAnalyticsService.calculateCommissionMetrics(request));
    }

    @PostMapping("/delivery-fees")
    @Operation(summary = "Calculate delivery fee metrics",
               description = "Calculate delivery fee metrics for the specified period. " +
                           "Tracks fees collected from customers and paid to couriers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery fee metrics calculated successfully",
                    content = @Content(schema = @Schema(implementation = DeliveryFeeMetricsDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<DeliveryFeeMetricsDto> getDeliveryFeeMetrics(
            @Valid @RequestBody FinancialMetricsRequest request) {
        log.info("Calculating delivery fee metrics for period: {} to {}", request.getStartDate(), request.getEndDate());
        return ResponseEntity.ok(financialAnalyticsService.calculateDeliveryFeeMetrics(request));
    }

    @PostMapping("/promotions")
    @Operation(summary = "Calculate promotion metrics",
               description = "Calculate promotion and discount metrics for the specified period. " +
                           "Tracks all promotional costs and their effectiveness.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promotion metrics calculated successfully",
                    content = @Content(schema = @Schema(implementation = PromotionMetricsDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PromotionMetricsDto> getPromotionMetrics(
            @Valid @RequestBody FinancialMetricsRequest request) {
        log.info("Calculating promotion metrics for period: {} to {}", request.getStartDate(), request.getEndDate());
        return ResponseEntity.ok(financialAnalyticsService.calculatePromotionMetrics(request));
    }

    @PostMapping("/restaurant-payouts")
    @Operation(summary = "Calculate restaurant payout metrics",
               description = "Calculate restaurant payout metrics for the specified period. " +
                           "Net Payout = Gross Sales - Commission - Delivery Subsidies - Promotion Costs + Adjustments - Fees")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Restaurant payout metrics calculated successfully",
                    content = @Content(schema = @Schema(implementation = RestaurantPayoutMetricsDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<RestaurantPayoutMetricsDto> getRestaurantPayoutMetrics(
            @Valid @RequestBody FinancialMetricsRequest request) {
        log.info("Calculating restaurant payout metrics for period: {} to {}", request.getStartDate(), request.getEndDate());
        return ResponseEntity.ok(financialAnalyticsService.calculateRestaurantPayoutMetrics(request));
    }

    @PostMapping("/courier-payouts")
    @Operation(summary = "Calculate courier payout metrics",
               description = "Calculate courier payout metrics for the specified period. " +
                           "Courier Payout = Base Pay + Distance Bonus + Tips + Peak Bonus + Incentives")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Courier payout metrics calculated successfully",
                    content = @Content(schema = @Schema(implementation = CourierPayoutMetricsDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<CourierPayoutMetricsDto> getCourierPayoutMetrics(
            @Valid @RequestBody FinancialMetricsRequest request) {
        log.info("Calculating courier payout metrics for period: {} to {}", request.getStartDate(), request.getEndDate());
        return ResponseEntity.ok(financialAnalyticsService.calculateCourierPayoutMetrics(request));
    }

    @PostMapping("/contribution-margin")
    @Operation(summary = "Calculate contribution margin",
               description = "Calculate contribution margin and unit economics for the specified period. " +
                           "CM = Commission Revenue + Delivery Fee Revenue - Courier Costs - Promotion Costs. " +
                           "Unit Economics = CM / Number of Orders")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contribution margin calculated successfully",
                    content = @Content(schema = @Schema(implementation = ContributionMarginDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<ContributionMarginDto> getContributionMargin(
            @Valid @RequestBody FinancialMetricsRequest request) {
        log.info("Calculating contribution margin for period: {} to {}", request.getStartDate(), request.getEndDate());
        return ResponseEntity.ok(financialAnalyticsService.calculateContributionMargin(request));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get financial summary",
               description = "Get high-level financial summary with key metrics for the specified period.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Financial summary retrieved successfully",
                    content = @Content(schema = @Schema(implementation = FinancialSummaryDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<FinancialSummaryDto> getFinancialSummary(
            @Parameter(description = "Start date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Getting financial summary for period: {} to {}", startDate, endDate);
        return ResponseEntity.ok(financialAnalyticsService.getFinancialSummary(startDate, endDate));
    }

    @GetMapping("/gmv/restaurants")
    @Operation(summary = "Get GMV by restaurants",
               description = "Get GMV metrics for specific restaurants.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "GMV metrics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = GmvMetricsDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<GmvMetricsDto> getGmvByRestaurants(
            @Parameter(description = "Start date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Restaurant IDs", required = true)
            @RequestParam List<Long> restaurantIds) {
        log.info("Getting GMV for restaurants: {} for period: {} to {}", restaurantIds, startDate, endDate);
        return ResponseEntity.ok(financialAnalyticsService.getGmvByRestaurants(startDate, endDate, restaurantIds));
    }

    @GetMapping("/restaurants/{restaurantId}/payouts")
    @Operation(summary = "Get restaurant payout details",
               description = "Get detailed payout information for a specific restaurant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Restaurant payout details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RestaurantPayoutMetricsDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Restaurant not found")
    })
    public ResponseEntity<RestaurantPayoutMetricsDto> getRestaurantPayoutDetails(
            @Parameter(description = "Restaurant ID", required = true)
            @PathVariable Long restaurantId,
            @Parameter(description = "Start date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Getting payout details for restaurant: {} for period: {} to {}", restaurantId, startDate, endDate);
        return ResponseEntity.ok(financialAnalyticsService.getRestaurantPayoutDetails(restaurantId, startDate, endDate));
    }

    @GetMapping("/couriers/{courierId}/payouts")
    @Operation(summary = "Get courier payout details",
               description = "Get detailed payout information for a specific courier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Courier payout details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CourierPayoutMetricsDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Courier not found")
    })
    public ResponseEntity<CourierPayoutMetricsDto> getCourierPayoutDetails(
            @Parameter(description = "Courier ID", required = true)
            @PathVariable Long courierId,
            @Parameter(description = "Start date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Getting payout details for courier: {} for period: {} to {}", courierId, startDate, endDate);
        return ResponseEntity.ok(financialAnalyticsService.getCourierPayoutDetails(courierId, startDate, endDate));
    }
}
