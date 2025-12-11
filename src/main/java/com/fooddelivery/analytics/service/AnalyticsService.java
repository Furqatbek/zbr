package com.fooddelivery.analytics.service;

import com.fooddelivery.analytics.dto.*;

/**
 * Service interface for business analytics metrics.
 *
 * <p>Provides methods to calculate key product metrics:</p>
 * <ul>
 *   <li>User Activity (DAU/WAU/MAU)</li>
 *   <li>Order Volume</li>
 *   <li>Conversion Funnel</li>
 *   <li>Average Order Value (AOV)</li>
 *   <li>User Activation</li>
 *   <li>Churn Metrics</li>
 * </ul>
 *
 * <p>All methods implement Redis caching with appropriate TTLs.</p>
 */
public interface AnalyticsService {

    // ============ User Activity Metrics ============

    /**
     * Get Daily Active Users count.
     *
     * @return Count of distinct users who performed any action today
     */
    Long getDailyActiveUsers();

    /**
     * Get Weekly Active Users count.
     *
     * @return Count of distinct users in the past 7 days
     */
    Long getWeeklyActiveUsers();

    /**
     * Get Monthly Active Users count.
     *
     * @return Count of distinct users in the past 30 days
     */
    Long getMonthlyActiveUsers();

    /**
     * Get comprehensive DAU/WAU/MAU metrics with ratios.
     *
     * @return DTO containing all user activity metrics
     */
    DauWauMauDto getUserActivityMetrics();

    // ============ Order Volume Metrics ============

    /**
     * Get orders placed today.
     *
     * @return Count of orders created today
     */
    Long getOrdersPerDay();

    /**
     * Get order distribution by hour for today.
     *
     * @return DTO containing hourly order distribution and peak hours
     */
    OrderVolumeDto getOrdersPerHour();

    /**
     * Get comprehensive order volume metrics.
     *
     * @return DTO containing all order volume metrics
     */
    OrderVolumeDto getOrderVolumeMetrics();

    // ============ Conversion Funnel Metrics ============

    /**
     * Get overall conversion rate.
     *
     * @return Conversion rate as percentage (payments / restaurant views)
     */
    Double getConversionRate();

    /**
     * Get detailed conversion funnel metrics.
     *
     * @return DTO containing all funnel steps and conversion rates
     */
    ConversionRateDto getConversionMetrics();

    /**
     * Get conversion metrics for a specific time period.
     *
     * @param days Number of days to analyze
     * @return DTO containing conversion metrics for the period
     */
    ConversionRateDto getConversionMetrics(int days);

    // ============ AOV Metrics ============

    /**
     * Get Average Order Value for today.
     *
     * @return Average order value for completed orders today
     */
    java.math.BigDecimal getAverageOrderValue();

    /**
     * Get comprehensive AOV metrics.
     *
     * @return DTO containing all AOV metrics and trends
     */
    AovDto getAovMetrics();

    // ============ User Activation Metrics ============

    /**
     * Get count of users who completed their first delivery.
     *
     * @return Count of first-time delivery completions
     */
    Long getFirstDeliveryCount();

    /**
     * Get average activation time (registration to first order).
     *
     * @return Average activation time in hours
     */
    Double getActivationTime();

    /**
     * Get referral usage rate.
     *
     * @return Percentage of new users who signed up with a referral
     */
    Double getReferralUsageRate();

    /**
     * Get comprehensive activation metrics.
     *
     * @return DTO containing all activation metrics
     */
    ActivationMetricsDto getUserActivationMetrics();

    // ============ Churn Metrics ============

    /**
     * Get user churn rate.
     *
     * @return Percentage of users not ordering in last 30 days
     */
    Double getUserChurnRate();

    /**
     * Get restaurant churn rate.
     *
     * @return Percentage of restaurants with zero orders in last 30 days
     */
    Double getRestaurantChurnRate();

    /**
     * Get courier churn rate.
     *
     * @return Percentage of couriers inactive for last 14 days
     */
    Double getCourierChurnRate();

    /**
     * Get comprehensive churn metrics.
     *
     * @return DTO containing all churn metrics for users, restaurants, and couriers
     */
    ChurnDto getChurnMetrics();

    // ============ Cache Management ============

    /**
     * Evict all analytics caches.
     * Useful for manual cache refresh.
     */
    void evictAllCaches();

    /**
     * Evict specific cache by name.
     *
     * @param cacheName Name of the cache to evict
     */
    void evictCache(String cacheName);
}
