package com.fooddelivery.analytics.operations.service;

import com.fooddelivery.analytics.operations.dto.*;

import java.time.LocalDate;

/**
 * Service interface for operations analytics.
 * Provides methods to calculate operational metrics for orders, restaurants, and couriers.
 */
public interface OperationsAnalyticsService {

    /**
     * Get order fulfillment metrics for a specific order.
     * Includes acceptance time, preparation time, pickup time, and delivery time.
     *
     * @param orderId The order ID
     * @return OrderFulfillmentMetricsDto with all timing metrics
     */
    OrderFulfillmentMetricsDto getOrderFulfillmentMetrics(Long orderId);

    /**
     * Get restaurant performance metrics for a date range.
     * Includes order acceptance rate, preparation time, offline time, menu update frequency.
     *
     * @param restaurantId The restaurant ID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return RestaurantPerformanceDto with all restaurant metrics
     */
    RestaurantPerformanceDto getRestaurantPerformanceMetrics(Long restaurantId, LocalDate startDate, LocalDate endDate);

    /**
     * Get courier performance metrics for a date range.
     * Includes acceptance rate, delivery time, availability, location update frequency.
     *
     * @param courierId The courier ID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return CourierPerformanceDto with all courier metrics
     */
    CourierPerformanceDto getCourierPerformanceMetrics(Long courierId, LocalDate startDate, LocalDate endDate);

    /**
     * Get delivery success rate metrics for a date range.
     * Includes completion rate, cancellation breakdown, delay analysis.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return DeliverySuccessRateDto with success metrics
     */
    DeliverySuccessRateDto getDeliverySuccessRate(LocalDate startDate, LocalDate endDate);

    /**
     * Get ETA accuracy metrics for a date range.
     * Includes average error, over/under estimation, accuracy by time and restaurant.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return EtaAccuracyDto with ETA accuracy metrics
     */
    EtaAccuracyDto getEtaAccuracyMetrics(LocalDate startDate, LocalDate endDate);

    /**
     * Get operations summary for a date range.
     * Provides a high-level overview of all operational metrics.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return OperationsSummaryDto with summary metrics
     */
    OperationsSummaryDto getOperationsSummary(LocalDate startDate, LocalDate endDate);

    /**
     * Evict all operations analytics caches.
     */
    void evictAllCaches();

    /**
     * Evict a specific cache by name.
     *
     * @param cacheName Name of the cache to evict
     */
    void evictCache(String cacheName);
}
