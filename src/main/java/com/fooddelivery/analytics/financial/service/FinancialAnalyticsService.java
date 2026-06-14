package com.fooddelivery.analytics.financial.service;

import com.fooddelivery.analytics.financial.dto.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for financial analytics calculations.
 */
public interface FinancialAnalyticsService {

    /**
     * Calculate GMV (Gross Merchandise Value) metrics.
     * GMV = Sum of all order values before deductions
     *
     * @param request the metrics request with date range and filters
     * @return GMV metrics including breakdown by restaurant and category
     */
    GmvMetricsDto calculateGmvMetrics(FinancialMetricsRequest request);

    /**
     * Calculate commission revenue metrics.
     * Commission = Platform fee charged to restaurants per order
     *
     * @param request the metrics request with date range and filters
     * @return Commission metrics including breakdown by type and restaurant
     */
    CommissionMetricsDto calculateCommissionMetrics(FinancialMetricsRequest request);

    /**
     * Calculate delivery fee metrics.
     * Tracks fees collected from customers and paid to couriers.
     *
     * @param request the metrics request with date range and filters
     * @return Delivery fee metrics including breakdown by distance and time
     */
    DeliveryFeeMetricsDto calculateDeliveryFeeMetrics(FinancialMetricsRequest request);

    /**
     * Calculate promotion and discount metrics.
     * Tracks all promotional costs and their effectiveness.
     *
     * @param request the metrics request with date range and filters
     * @return Promotion metrics including cost attribution
     */
    PromotionMetricsDto calculatePromotionMetrics(FinancialMetricsRequest request);

    /**
     * Calculate restaurant payout metrics.
     * Net Payout = Gross Sales - Commission - Delivery Subsidies - Promotion Costs + Adjustments - Fees
     *
     * @param request the metrics request with date range and filters
     * @return Restaurant payout metrics including dispute summary
     */
    RestaurantPayoutMetricsDto calculateRestaurantPayoutMetrics(FinancialMetricsRequest request);

    /**
     * Calculate courier payout metrics.
     * Courier Payout = Base Pay + Distance Bonus + Tips + Peak Bonus + Incentives
     *
     * @param request the metrics request with date range and filters
     * @return Courier payout metrics including bonus breakdown
     */
    CourierPayoutMetricsDto calculateCourierPayoutMetrics(FinancialMetricsRequest request);

    /**
     * Calculate contribution margin metrics.
     * CM = Commission Revenue + Delivery Fee Revenue - Courier Costs - Promotion Costs
     * Unit Economics = CM / Number of Orders
     *
     * @param request the metrics request with date range and filters
     * @return Contribution margin metrics including unit economics
     */
    ContributionMarginDto calculateContributionMargin(FinancialMetricsRequest request);

    /**
     * Get high-level financial summary.
     *
     * @param startDate start of reporting period
     * @param endDate end of reporting period
     * @return Financial summary with key metrics
     */
    FinancialSummaryDto getFinancialSummary(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get GMV for specific restaurants.
     *
     * @param startDate start of reporting period
     * @param endDate end of reporting period
     * @param restaurantIds list of restaurant IDs
     * @return GMV metrics for specified restaurants
     */
    GmvMetricsDto getGmvByRestaurants(LocalDateTime startDate, LocalDateTime endDate, List<Long> restaurantIds);

    /**
     * Restaurant-scoped GMV (food revenue, order count, commission) for the report.
     */
    GmvMetricsDto getGmvForRestaurant(Long restaurantId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get payout details for a specific restaurant.
     *
     * @param restaurantId the restaurant ID
     * @param startDate start of reporting period
     * @param endDate end of reporting period
     * @return Payout metrics for the restaurant
     */
    RestaurantPayoutMetricsDto getRestaurantPayoutDetails(Long restaurantId,
                                                          LocalDateTime startDate,
                                                          LocalDateTime endDate);

    /**
     * Get payout details for a specific courier.
     *
     * @param courierId the courier ID
     * @param startDate start of reporting period
     * @param endDate end of reporting period
     * @return Payout metrics for the courier
     */
    CourierPayoutMetricsDto getCourierPayoutDetails(Long courierId,
                                                    LocalDateTime startDate,
                                                    LocalDateTime endDate);
}
