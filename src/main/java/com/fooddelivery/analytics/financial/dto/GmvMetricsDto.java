package com.fooddelivery.analytics.financial.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Gross Merchandise Value (GMV) metrics.
 * GMV = Sum of all order values before deductions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GmvMetricsDto {

    /**
     * Total GMV for the period.
     */
    private BigDecimal totalGmv;

    /**
     * Number of orders contributing to GMV.
     */
    private Long totalOrders;

    /**
     * Average order value.
     */
    private BigDecimal averageOrderValue;

    /**
     * GMV from food items only (excludes delivery fees).
     */
    private BigDecimal foodGmv;

    /**
     * GMV from delivery fees.
     */
    private BigDecimal deliveryFeeGmv;

    /**
     * GMV from tips.
     */
    private BigDecimal tipGmv;

    /**
     * GMV growth rate compared to previous period (percentage).
     */
    private BigDecimal growthRate;

    /**
     * GMV from previous period for comparison.
     */
    private BigDecimal previousPeriodGmv;

    /**
     * Breakdown by restaurant.
     */
    private List<RestaurantGmvDto> byRestaurant;

    /**
     * Breakdown by category.
     */
    private List<CategoryGmvDto> byCategory;

    /**
     * Daily GMV trend.
     */
    private List<DailyGmvDto> dailyTrend;

    /**
     * Start of the reporting period.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime periodStart;

    /**
     * End of the reporting period.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime periodEnd;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RestaurantGmvDto {
        private Long restaurantId;
        private String restaurantName;
        private BigDecimal gmv;
        private Long orderCount;
        private BigDecimal percentageOfTotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryGmvDto {
        private String category;
        private BigDecimal gmv;
        private Long orderCount;
        private BigDecimal percentageOfTotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyGmvDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private java.time.LocalDate date;
        private BigDecimal gmv;
        private Long orderCount;
        private BigDecimal averageOrderValue;
    }
}
