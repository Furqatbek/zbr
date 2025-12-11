package com.fooddelivery.analytics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO containing order volume metrics.
 *
 * <p>Provides insights into:</p>
 * <ul>
 *   <li>Orders per day</li>
 *   <li>Orders per hour (peak hours analysis)</li>
 *   <li>First orders vs repeat orders</li>
 *   <li>Failed/cancelled order tracking</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderVolumeDto {

    /**
     * Total orders for the reference date.
     */
    private Long totalOrdersToday;

    /**
     * Total orders for the past 7 days.
     */
    private Long totalOrdersWeek;

    /**
     * Total orders for the past 30 days.
     */
    private Long totalOrdersMonth;

    /**
     * Successfully completed orders today.
     */
    private Long completedOrdersToday;

    /**
     * Cancelled orders today.
     */
    private Long cancelledOrdersToday;

    /**
     * Failed orders today (payment failed, etc.).
     */
    private Long failedOrdersToday;

    /**
     * Order success rate (completed / total).
     */
    private Double successRate;

    /**
     * Cancellation rate (cancelled / total).
     */
    private Double cancellationRate;

    /**
     * First-time orders (users placing their first ever order) today.
     */
    private Long firstTimeOrdersToday;

    /**
     * Repeat orders (users who have ordered before) today.
     */
    private Long repeatOrdersToday;

    /**
     * Repeat order rate (repeat / total).
     */
    private Double repeatOrderRate;

    /**
     * Orders breakdown by hour (0-23) for today.
     * Key: hour (0-23), Value: order count.
     */
    private Map<Integer, Long> ordersPerHour;

    /**
     * Peak hour with most orders.
     */
    private Integer peakHour;

    /**
     * Number of orders during peak hour.
     */
    private Long peakHourOrders;

    /**
     * Average orders per hour today.
     */
    private Double averageOrdersPerHour;

    /**
     * Daily order trend for the past 7 days.
     */
    private List<DailyOrderCount> dailyTrend;

    /**
     * Reference date for the metrics.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate referenceDate;

    /**
     * Timestamp when this metric was calculated.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime calculatedAt;

    /**
     * Calculate derived metrics like rates and peak hours.
     */
    public void calculateDerivedMetrics() {
        if (totalOrdersToday != null && totalOrdersToday > 0) {
            this.successRate = completedOrdersToday != null ?
                    Math.round((double) completedOrdersToday / totalOrdersToday * 10000) / 100.0 : 0.0;
            this.cancellationRate = cancelledOrdersToday != null ?
                    Math.round((double) cancelledOrdersToday / totalOrdersToday * 10000) / 100.0 : 0.0;
            this.repeatOrderRate = repeatOrdersToday != null ?
                    Math.round((double) repeatOrdersToday / totalOrdersToday * 10000) / 100.0 : 0.0;
        }

        if (ordersPerHour != null && !ordersPerHour.isEmpty()) {
            var maxEntry = ordersPerHour.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);
            if (maxEntry != null) {
                this.peakHour = maxEntry.getKey();
                this.peakHourOrders = maxEntry.getValue();
            }
            this.averageOrdersPerHour = Math.round(ordersPerHour.values().stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0) * 100) / 100.0;
        }
    }

    /**
     * Inner class for daily order count trend.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyOrderCount {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private Long totalOrders;
        private Long completedOrders;
        private Long cancelledOrders;
    }
}
