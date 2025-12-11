package com.fooddelivery.analytics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO containing Average Order Value (AOV) metrics.
 *
 * <p>Formula:</p>
 * <pre>
 * AOV = Total Order Revenue / Number of Completed Orders
 * </pre>
 *
 * <p>AOV is a key metric for understanding customer purchasing behavior
 * and revenue optimization opportunities.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AovDto {

    /**
     * Average order value for today.
     */
    private BigDecimal aovToday;

    /**
     * Average order value for the past 7 days.
     */
    private BigDecimal aovWeek;

    /**
     * Average order value for the past 30 days.
     */
    private BigDecimal aovMonth;

    /**
     * Total revenue today (sum of all completed orders).
     */
    private BigDecimal totalRevenueToday;

    /**
     * Total revenue for the past 7 days.
     */
    private BigDecimal totalRevenueWeek;

    /**
     * Total revenue for the past 30 days.
     */
    private BigDecimal totalRevenueMonth;

    /**
     * Number of completed orders today.
     */
    private Long completedOrdersToday;

    /**
     * Number of completed orders this week.
     */
    private Long completedOrdersWeek;

    /**
     * Number of completed orders this month.
     */
    private Long completedOrdersMonth;

    /**
     * Median order value today (50th percentile).
     */
    private BigDecimal medianOrderValueToday;

    /**
     * Minimum order value today.
     */
    private BigDecimal minOrderValueToday;

    /**
     * Maximum order value today.
     */
    private BigDecimal maxOrderValueToday;

    /**
     * AOV change compared to yesterday (percentage).
     */
    private Double aovChangeFromYesterday;

    /**
     * AOV change compared to last week (percentage).
     */
    private Double aovChangeFromLastWeek;

    /**
     * Average items per order today.
     */
    private Double averageItemsPerOrder;

    /**
     * Average delivery fee today.
     */
    private BigDecimal averageDeliveryFee;

    /**
     * Average tip amount today.
     */
    private BigDecimal averageTipAmount;

    /**
     * Daily AOV trend for the past 7 days.
     */
    private List<DailyAov> dailyTrend;

    /**
     * Currency code for the values.
     */
    @Builder.Default
    private String currency = "UZS";

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
     * Inner class for daily AOV trend.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyAov {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private BigDecimal aov;
        private BigDecimal totalRevenue;
        private Long orderCount;
    }
}
