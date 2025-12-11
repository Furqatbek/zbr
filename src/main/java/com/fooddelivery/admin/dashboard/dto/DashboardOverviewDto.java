package com.fooddelivery.admin.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for the main dashboard overview panel.
 * Contains top-level summary widgets for real-time operational insights.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewDto {

    // Order Metrics
    private Long ordersToday;
    private Long ordersPendingAcceptance;
    private Long ordersInPreparation;
    private Long ordersInDelivery;
    private Long ordersCompletedToday;
    private Long ordersCancelledToday;

    // Revenue Metrics
    private BigDecimal revenueToday; // GMV
    private BigDecimal averageOrderValue;
    private BigDecimal totalTipsToday;
    private BigDecimal totalDiscountsToday;

    // Active Entities
    private Long activeCouriers;
    private Long totalCouriersOnline;
    private Long availableCouriers;
    private Long busyCouriers;

    private Long activeRestaurants;
    private Long totalRestaurantsOnline;
    private Long restaurantsAcceptingOrders;

    // Performance Metrics
    private Double avgDeliveryTimeToday; // in minutes
    private Double avgPrepTimeToday; // in minutes
    private Double avgAcceptanceTimeToday; // in minutes
    private Double orderFulfillmentRate; // percentage

    // Comparison with Previous Day
    private OrderComparisonDto comparison;

    // System Status
    private SystemStatusDto systemStatus;

    // Timestamp
    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderComparisonDto {
        private Long ordersYesterday;
        private BigDecimal revenueYesterday;
        private Double orderChangePercent;
        private Double revenueChangePercent;
        private String trend; // UP, DOWN, STABLE
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemStatusDto {
        private Integer apiLatencyP50;
        private Integer apiLatencyP90;
        private Integer dbLoad; // active connections
        private Integer dbIdleConnections;
        private Integer redisLatency;
        private Long queueBacklog;
        private Double errorRatePercent;
        private Double cpuUsagePercent;
        private Double memoryUsagePercent;
        private String overallStatus; // HEALTHY, DEGRADED, CRITICAL
    }
}
