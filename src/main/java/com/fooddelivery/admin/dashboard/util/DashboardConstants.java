package com.fooddelivery.admin.dashboard.util;

import lombok.experimental.UtilityClass;

import java.util.Map;

/**
 * Constants used across the admin dashboard module.
 */
@UtilityClass
public class DashboardConstants {

    // ===== Cache Configuration =====

    /**
     * Cache name for dashboard overview metrics.
     */
    public static final String CACHE_OVERVIEW = "dashboard:overview";

    /**
     * Cache name for active orders.
     */
    public static final String CACHE_ACTIVE_ORDERS = "dashboard:activeOrders";

    /**
     * Cache name for stuck orders.
     */
    public static final String CACHE_STUCK_ORDERS = "dashboard:stuckOrders";

    /**
     * Cache name for restaurant metrics.
     */
    public static final String CACHE_RESTAURANT_METRICS = "dashboard:restaurants";

    /**
     * Cache name for courier metrics.
     */
    public static final String CACHE_COURIER_METRICS = "dashboard:couriers";

    /**
     * Cache name for finance metrics.
     */
    public static final String CACHE_FINANCE_METRICS = "dashboard:finance";

    /**
     * Cache name for support metrics.
     */
    public static final String CACHE_SUPPORT_METRICS = "dashboard:support";

    // ===== Cache TTL (in seconds) =====

    /**
     * TTL for overview cache (15 seconds).
     */
    public static final long CACHE_TTL_OVERVIEW = 15;

    /**
     * TTL for active orders cache (5 seconds).
     */
    public static final long CACHE_TTL_ACTIVE_ORDERS = 5;

    /**
     * TTL for stuck orders cache (10 seconds).
     */
    public static final long CACHE_TTL_STUCK_ORDERS = 10;

    /**
     * TTL for restaurant metrics cache (30 seconds).
     */
    public static final long CACHE_TTL_RESTAURANT = 30;

    /**
     * TTL for courier metrics cache (15 seconds).
     */
    public static final long CACHE_TTL_COURIER = 15;

    /**
     * TTL for finance metrics cache (30 seconds).
     */
    public static final long CACHE_TTL_FINANCE = 30;

    /**
     * TTL for support metrics cache (30 seconds).
     */
    public static final long CACHE_TTL_SUPPORT = 30;

    // ===== Stuck Order Thresholds (in minutes) =====

    /**
     * Default threshold for PENDING status (10 minutes).
     */
    public static final int STUCK_THRESHOLD_PENDING = 10;

    /**
     * Default threshold for ACCEPTED status (15 minutes).
     */
    public static final int STUCK_THRESHOLD_ACCEPTED = 15;

    /**
     * Default threshold for PREPARING status (30 minutes).
     */
    public static final int STUCK_THRESHOLD_PREPARING = 30;

    /**
     * Default threshold for READY status (15 minutes).
     */
    public static final int STUCK_THRESHOLD_READY = 15;

    /**
     * Default threshold for IN_TRANSIT status (45 minutes).
     */
    public static final int STUCK_THRESHOLD_IN_TRANSIT = 45;

    /**
     * Get default stuck thresholds as a map.
     */
    public static Map<String, Integer> getDefaultStuckThresholds() {
        return Map.of(
                "PENDING", STUCK_THRESHOLD_PENDING,
                "ACCEPTED", STUCK_THRESHOLD_ACCEPTED,
                "PREPARING", STUCK_THRESHOLD_PREPARING,
                "READY", STUCK_THRESHOLD_READY,
                "IN_TRANSIT", STUCK_THRESHOLD_IN_TRANSIT
        );
    }

    // ===== Courier Activity Thresholds =====

    /**
     * Minutes since last ping to consider courier active.
     */
    public static final int COURIER_ACTIVE_PING_THRESHOLD_MINUTES = 5;

    /**
     * Minutes since last ping to consider courier idle.
     */
    public static final int COURIER_IDLE_THRESHOLD_MINUTES = 15;

    // ===== System Health Thresholds =====

    /**
     * API latency threshold for degraded status (milliseconds).
     */
    public static final double API_LATENCY_DEGRADED_THRESHOLD = 1000;

    /**
     * API latency threshold for critical status (milliseconds).
     */
    public static final double API_LATENCY_CRITICAL_THRESHOLD = 2000;

    /**
     * Database load threshold for degraded status (percentage).
     */
    public static final double DB_LOAD_DEGRADED_THRESHOLD = 70;

    /**
     * Database load threshold for critical status (percentage).
     */
    public static final double DB_LOAD_CRITICAL_THRESHOLD = 90;

    /**
     * Redis latency threshold for degraded status (milliseconds).
     */
    public static final double REDIS_LATENCY_DEGRADED_THRESHOLD = 50;

    /**
     * Redis latency threshold for critical status (milliseconds).
     */
    public static final double REDIS_LATENCY_CRITICAL_THRESHOLD = 100;

    /**
     * Error rate threshold for degraded status (percentage).
     */
    public static final double ERROR_RATE_DEGRADED_THRESHOLD = 1;

    /**
     * Error rate threshold for critical status (percentage).
     */
    public static final double ERROR_RATE_CRITICAL_THRESHOLD = 5;

    // ===== Pagination Defaults =====

    /**
     * Default page size for lists.
     */
    public static final int DEFAULT_PAGE_SIZE = 50;

    /**
     * Maximum page size allowed.
     */
    public static final int MAX_PAGE_SIZE = 200;

    /**
     * Default page number (0-indexed).
     */
    public static final int DEFAULT_PAGE = 0;

    // ===== Order Statuses =====

    /**
     * All active order statuses (orders in progress).
     */
    public static final String[] ACTIVE_ORDER_STATUSES = {
            "PENDING", "ACCEPTED", "PREPARING", "READY", "PICKED_UP", "IN_TRANSIT"
    };

    /**
     * Completed order statuses.
     */
    public static final String[] COMPLETED_ORDER_STATUSES = {
            "DELIVERED", "CANCELLED", "REJECTED"
    };

    // ===== Support Ticket SLA (in minutes) =====

    /**
     * SLA for CRITICAL priority tickets.
     */
    public static final int SLA_CRITICAL_MINUTES = 15;

    /**
     * SLA for HIGH priority tickets.
     */
    public static final int SLA_HIGH_MINUTES = 30;

    /**
     * SLA for MEDIUM priority tickets.
     */
    public static final int SLA_MEDIUM_MINUTES = 60;

    /**
     * SLA for LOW priority tickets.
     */
    public static final int SLA_LOW_MINUTES = 120;

    /**
     * Get SLA threshold for a priority level.
     */
    public static int getSlaThreshold(String priority) {
        return switch (priority != null ? priority.toUpperCase() : "MEDIUM") {
            case "CRITICAL" -> SLA_CRITICAL_MINUTES;
            case "HIGH" -> SLA_HIGH_MINUTES;
            case "MEDIUM" -> SLA_MEDIUM_MINUTES;
            case "LOW" -> SLA_LOW_MINUTES;
            default -> SLA_MEDIUM_MINUTES;
        };
    }

    // ===== Commission Rate =====

    /**
     * Default commission rate (15%).
     */
    public static final double DEFAULT_COMMISSION_RATE = 0.15;

    // ===== Performance Score Weights =====

    /**
     * Weight for rating in restaurant performance score.
     */
    public static final double RESTAURANT_RATING_WEIGHT = 0.40;

    /**
     * Weight for acceptance rate in restaurant performance score.
     */
    public static final double RESTAURANT_ACCEPTANCE_WEIGHT = 0.30;

    /**
     * Weight for prep time in restaurant performance score.
     */
    public static final double RESTAURANT_PREP_TIME_WEIGHT = 0.30;

    /**
     * Weight for rating in courier performance score.
     */
    public static final double COURIER_RATING_WEIGHT = 0.35;

    /**
     * Weight for on-time rate in courier performance score.
     */
    public static final double COURIER_ONTIME_WEIGHT = 0.35;

    /**
     * Weight for delivery time in courier performance score.
     */
    public static final double COURIER_DELIVERY_TIME_WEIGHT = 0.30;

    // ===== API Endpoints =====

    /**
     * Base path for admin dashboard API.
     */
    public static final String API_BASE_PATH = "/api/v1/admin/dashboard";

    /**
     * Endpoint for overview metrics.
     */
    public static final String ENDPOINT_OVERVIEW = "/overview";

    /**
     * Endpoint for active orders.
     */
    public static final String ENDPOINT_ACTIVE_ORDERS = "/orders/active";

    /**
     * Endpoint for stuck orders.
     */
    public static final String ENDPOINT_STUCK_ORDERS = "/orders/stuck";

    /**
     * Endpoint for restaurant metrics.
     */
    public static final String ENDPOINT_RESTAURANTS = "/restaurants";

    /**
     * Endpoint for courier metrics.
     */
    public static final String ENDPOINT_COURIERS = "/couriers";

    /**
     * Endpoint for finance metrics.
     */
    public static final String ENDPOINT_FINANCE = "/finance";

    /**
     * Endpoint for support metrics.
     */
    public static final String ENDPOINT_SUPPORT = "/support";

    /**
     * Endpoint for cache refresh.
     */
    public static final String ENDPOINT_CACHE_REFRESH = "/cache/refresh";
}
