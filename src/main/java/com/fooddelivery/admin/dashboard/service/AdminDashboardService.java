package com.fooddelivery.admin.dashboard.service;

import com.fooddelivery.admin.dashboard.dto.*;

import java.time.LocalDateTime;

/**
 * Service interface for admin dashboard operations.
 * Provides real-time operational insights across orders, restaurants,
 * couriers, finance, and customer support.
 */
public interface AdminDashboardService {

    /**
     * Get comprehensive dashboard overview with all key metrics.
     *
     * @param startDate start of the date range
     * @param endDate end of the date range
     * @return dashboard overview with orders, revenue, and system status
     */
    DashboardOverviewDto getOverview(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get all active orders with optional filters.
     *
     * @param filter filter criteria including pagination
     * @return active orders with details
     */
    ActiveOrdersDto getActiveOrders(DashboardFilterRequest filter);

    /**
     * Get stuck orders that have exceeded their stage thresholds.
     *
     * @param filter filter criteria including custom thresholds
     * @return stuck orders prioritized by urgency
     */
    StuckOrdersDto getStuckOrders(DashboardFilterRequest filter);

    /**
     * Get cancelled orders within the date range.
     *
     * @param filter filter criteria
     * @return cancelled orders with breakdown by reason
     */
    CanceledOrdersDto getCancelledOrders(DashboardFilterRequest filter);

    /**
     * Get rejected orders within the date range.
     *
     * @param filter filter criteria
     * @return rejected orders with restaurant breakdown
     */
    RejectedOrdersDto getRejectedOrders(DashboardFilterRequest filter);

    /**
     * Get restaurant metrics and performance data.
     *
     * @param filter filter criteria including restaurant filters
     * @return restaurant metrics with online/offline status and performance
     */
    RestaurantMetricsDto getRestaurantMetrics(DashboardFilterRequest filter);

    /**
     * Get courier metrics including locations and performance.
     *
     * @param filter filter criteria including courier filters
     * @return courier metrics with locations and availability
     */
    CourierMetricsDto getCourierMetrics(DashboardFilterRequest filter);

    /**
     * Get financial metrics including GMV, revenue, and payouts.
     *
     * @param filter filter criteria
     * @return financial metrics with detailed breakdowns
     */
    FinanceMetricsDto getFinanceMetrics(DashboardFilterRequest filter);

    /**
     * Get customer support metrics and ticket information.
     *
     * @param filter filter criteria
     * @return support metrics with ticket details and agent performance
     */
    SupportMetricsDto getSupportMetrics(DashboardFilterRequest filter);

    /**
     * Refresh all dashboard caches.
     * Use sparingly as it triggers recalculation of all metrics.
     */
    void refreshCache();

    /**
     * Refresh specific dashboard cache.
     *
     * @param cacheKey the cache key to refresh
     */
    void refreshCache(String cacheKey);
}
