package com.fooddelivery.admin.dashboard.service;

import com.fooddelivery.admin.dashboard.collector.*;
import com.fooddelivery.admin.dashboard.dto.*;
import com.fooddelivery.admin.dashboard.util.DashboardConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of AdminDashboardService.
 * Provides real-time operational insights with Redis caching.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final OverviewCollector overviewCollector;
    private final OrdersCollector ordersCollector;
    private final RestaurantCollector restaurantCollector;
    private final CourierCollector courierCollector;
    private final FinanceCollector financeCollector;
    private final SupportCollector supportCollector;
    private final DashboardRefreshLogService refreshLogService;

    @Override
    @Cacheable(value = DashboardConstants.CACHE_OVERVIEW, key = "#startDate.toString() + '-' + #endDate.toString()")
    public DashboardOverviewDto getOverview(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching dashboard overview for period: {} to {}", startDate, endDate);

        long startTime = System.currentTimeMillis();

        DashboardFilterRequest filter = DashboardFilterRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        DashboardOverviewDto overview = overviewCollector.collectOverview(filter);

        long duration = System.currentTimeMillis() - startTime;
        logRefresh("overview", duration, true, null);

        return overview;
    }

    @Override
    @Cacheable(value = DashboardConstants.CACHE_ACTIVE_ORDERS, key = "#filter.hashCode()")
    public ActiveOrdersDto getActiveOrders(DashboardFilterRequest filter) {
        log.debug("Fetching active orders with filter: {}", filter);

        long startTime = System.currentTimeMillis();

        ActiveOrdersDto activeOrders = ordersCollector.collectActiveOrders(filter);

        long duration = System.currentTimeMillis() - startTime;
        logRefresh("activeOrders", duration, true, null);

        return activeOrders;
    }

    @Override
    @Cacheable(value = DashboardConstants.CACHE_STUCK_ORDERS, key = "#filter.hashCode()")
    public StuckOrdersDto getStuckOrders(DashboardFilterRequest filter) {
        log.debug("Fetching stuck orders with filter: {}", filter);

        long startTime = System.currentTimeMillis();

        StuckOrdersDto stuckOrders = ordersCollector.collectStuckOrders(filter);

        long duration = System.currentTimeMillis() - startTime;
        logRefresh("stuckOrders", duration, true, null);

        return stuckOrders;
    }

    @Override
    public CanceledOrdersDto getCancelledOrders(DashboardFilterRequest filter) {
        log.debug("Fetching cancelled orders with filter: {}", filter);

        long startTime = System.currentTimeMillis();

        CanceledOrdersDto cancelledOrders = ordersCollector.collectCancelledOrders(filter);

        long duration = System.currentTimeMillis() - startTime;
        logRefresh("cancelledOrders", duration, true, null);

        return cancelledOrders;
    }

    @Override
    public RejectedOrdersDto getRejectedOrders(DashboardFilterRequest filter) {
        log.debug("Fetching rejected orders with filter: {}", filter);

        long startTime = System.currentTimeMillis();

        RejectedOrdersDto rejectedOrders = ordersCollector.collectRejectedOrders(filter);

        long duration = System.currentTimeMillis() - startTime;
        logRefresh("rejectedOrders", duration, true, null);

        return rejectedOrders;
    }

    @Override
    @Cacheable(value = DashboardConstants.CACHE_RESTAURANT_METRICS, key = "#filter.hashCode()")
    public RestaurantMetricsDto getRestaurantMetrics(DashboardFilterRequest filter) {
        log.debug("Fetching restaurant metrics with filter: {}", filter);

        long startTime = System.currentTimeMillis();

        RestaurantMetricsDto restaurantMetrics = restaurantCollector.collectRestaurantMetrics(filter);

        long duration = System.currentTimeMillis() - startTime;
        logRefresh("restaurants", duration, true, null);

        return restaurantMetrics;
    }

    @Override
    @Cacheable(value = DashboardConstants.CACHE_COURIER_METRICS, key = "#filter.hashCode()")
    public CourierMetricsDto getCourierMetrics(DashboardFilterRequest filter) {
        log.debug("Fetching courier metrics with filter: {}", filter);

        long startTime = System.currentTimeMillis();

        CourierMetricsDto courierMetrics = courierCollector.collectCourierMetrics(filter);

        long duration = System.currentTimeMillis() - startTime;
        logRefresh("couriers", duration, true, null);

        return courierMetrics;
    }

    @Override
    @Cacheable(value = DashboardConstants.CACHE_FINANCE_METRICS, key = "#filter.hashCode()")
    public FinanceMetricsDto getFinanceMetrics(DashboardFilterRequest filter) {
        log.debug("Fetching finance metrics with filter: {}", filter);

        long startTime = System.currentTimeMillis();

        FinanceMetricsDto financeMetrics = financeCollector.collectFinanceMetrics(filter);

        long duration = System.currentTimeMillis() - startTime;
        logRefresh("finance", duration, true, null);

        return financeMetrics;
    }

    @Override
    @Cacheable(value = DashboardConstants.CACHE_SUPPORT_METRICS, key = "#filter.hashCode()")
    public SupportMetricsDto getSupportMetrics(DashboardFilterRequest filter) {
        log.debug("Fetching support metrics with filter: {}", filter);

        long startTime = System.currentTimeMillis();

        SupportMetricsDto supportMetrics = supportCollector.collectSupportMetrics(filter);

        long duration = System.currentTimeMillis() - startTime;
        logRefresh("support", duration, true, null);

        return supportMetrics;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = DashboardConstants.CACHE_OVERVIEW, allEntries = true),
            @CacheEvict(value = DashboardConstants.CACHE_ACTIVE_ORDERS, allEntries = true),
            @CacheEvict(value = DashboardConstants.CACHE_STUCK_ORDERS, allEntries = true),
            @CacheEvict(value = DashboardConstants.CACHE_RESTAURANT_METRICS, allEntries = true),
            @CacheEvict(value = DashboardConstants.CACHE_COURIER_METRICS, allEntries = true),
            @CacheEvict(value = DashboardConstants.CACHE_FINANCE_METRICS, allEntries = true),
            @CacheEvict(value = DashboardConstants.CACHE_SUPPORT_METRICS, allEntries = true)
    })
    @Transactional
    public void refreshCache() {
        log.info("Refreshing all dashboard caches");
        logRefresh("all", 0L, true, "Manual cache refresh");
    }

    @Override
    @Transactional
    public void refreshCache(String cacheKey) {
        log.info("Refreshing dashboard cache: {}", cacheKey);

        switch (cacheKey.toLowerCase()) {
            case "overview" -> evictOverviewCache();
            case "activeorders" -> evictActiveOrdersCache();
            case "stuckorders" -> evictStuckOrdersCache();
            case "restaurants" -> evictRestaurantCache();
            case "couriers" -> evictCourierCache();
            case "finance" -> evictFinanceCache();
            case "support" -> evictSupportCache();
            default -> {
                log.warn("Unknown cache key: {}", cacheKey);
                return;
            }
        }

        logRefresh(cacheKey, 0L, true, "Manual cache refresh");
    }

    @CacheEvict(value = DashboardConstants.CACHE_OVERVIEW, allEntries = true)
    public void evictOverviewCache() {
        log.debug("Evicting overview cache");
    }

    @CacheEvict(value = DashboardConstants.CACHE_ACTIVE_ORDERS, allEntries = true)
    public void evictActiveOrdersCache() {
        log.debug("Evicting active orders cache");
    }

    @CacheEvict(value = DashboardConstants.CACHE_STUCK_ORDERS, allEntries = true)
    public void evictStuckOrdersCache() {
        log.debug("Evicting stuck orders cache");
    }

    @CacheEvict(value = DashboardConstants.CACHE_RESTAURANT_METRICS, allEntries = true)
    public void evictRestaurantCache() {
        log.debug("Evicting restaurant cache");
    }

    @CacheEvict(value = DashboardConstants.CACHE_COURIER_METRICS, allEntries = true)
    public void evictCourierCache() {
        log.debug("Evicting courier cache");
    }

    @CacheEvict(value = DashboardConstants.CACHE_FINANCE_METRICS, allEntries = true)
    public void evictFinanceCache() {
        log.debug("Evicting finance cache");
    }

    @CacheEvict(value = DashboardConstants.CACHE_SUPPORT_METRICS, allEntries = true)
    public void evictSupportCache() {
        log.debug("Evicting support cache");
    }

    /**
     * Log dashboard refresh event using the dedicated logging service.
     */
    private void logRefresh(String component, Long durationMs, boolean success, String message) {
        refreshLogService.logRefresh(component, durationMs, success, message);
    }
}
