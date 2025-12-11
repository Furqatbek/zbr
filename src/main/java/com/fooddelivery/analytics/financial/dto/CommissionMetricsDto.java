package com.fooddelivery.analytics.financial.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fooddelivery.analytics.financial.model.CommissionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Commission Revenue metrics.
 * Commission = Platform fee charged to restaurants per order
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionMetricsDto {

    /**
     * Total commission revenue for the period.
     */
    private BigDecimal totalCommission;

    /**
     * Number of orders with commissions.
     */
    private Long orderCount;

    /**
     * Average commission per order.
     */
    private BigDecimal averageCommissionPerOrder;

    /**
     * Effective commission rate (total commission / total GMV).
     */
    private BigDecimal effectiveCommissionRate;

    /**
     * Commission from percentage-based fees.
     */
    private BigDecimal percentageBasedCommission;

    /**
     * Commission from fixed fees.
     */
    private BigDecimal fixedCommission;

    /**
     * Commission growth rate compared to previous period.
     */
    private BigDecimal growthRate;

    /**
     * Previous period commission for comparison.
     */
    private BigDecimal previousPeriodCommission;

    /**
     * Breakdown by commission type.
     */
    private List<CommissionByTypeDto> byType;

    /**
     * Breakdown by restaurant tier/category.
     */
    private List<CommissionByTierDto> byTier;

    /**
     * Top restaurants by commission generated.
     */
    private List<RestaurantCommissionDto> topRestaurants;

    /**
     * Daily commission trend.
     */
    private List<DailyCommissionDto> dailyTrend;

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
    public static class CommissionByTypeDto {
        private CommissionType commissionType;
        private BigDecimal totalAmount;
        private Long orderCount;
        private BigDecimal percentageOfTotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommissionByTierDto {
        private String tier;
        private BigDecimal totalCommission;
        private BigDecimal averageRate;
        private Long restaurantCount;
        private Long orderCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RestaurantCommissionDto {
        private Long restaurantId;
        private String restaurantName;
        private BigDecimal totalCommission;
        private BigDecimal commissionRate;
        private Long orderCount;
        private BigDecimal averageOrderValue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyCommissionDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private java.time.LocalDate date;
        private BigDecimal commission;
        private Long orderCount;
        private BigDecimal averageCommission;
    }
}
