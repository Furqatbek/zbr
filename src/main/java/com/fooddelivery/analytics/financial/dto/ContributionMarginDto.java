package com.fooddelivery.analytics.financial.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Contribution Margin metrics.
 * Contribution Margin = Commission Revenue + Delivery Fee Revenue - Courier Costs - Promotion Costs
 * Unit Economics = Contribution Margin / Number of Orders
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributionMarginDto {

    /**
     * Total GMV for the period.
     */
    private BigDecimal gmv;

    /**
     * Total commission revenue.
     */
    private BigDecimal commissionRevenue;

    /**
     * Net delivery fee revenue (collected - paid to couriers).
     */
    private BigDecimal deliveryFeeRevenue;

    /**
     * Total courier costs (base + bonuses + incentives).
     */
    private BigDecimal courierCosts;

    /**
     * Total platform promotion costs.
     */
    private BigDecimal promotionCosts;

    /**
     * Total revenue (commission + delivery fees).
     */
    private BigDecimal totalRevenue;

    /**
     * Total variable costs (courier + promotions).
     */
    private BigDecimal totalVariableCosts;

    /**
     * Contribution margin = Total Revenue - Total Variable Costs.
     */
    private BigDecimal contributionMargin;

    /**
     * Contribution margin percentage (CM / GMV).
     */
    private BigDecimal contributionMarginPercentage;

    /**
     * Number of orders in the period.
     */
    private Long orderCount;

    /**
     * Unit economics (contribution margin per order).
     */
    private BigDecimal unitEconomics;

    /**
     * Revenue per order.
     */
    private BigDecimal revenuePerOrder;

    /**
     * Variable cost per order.
     */
    private BigDecimal variableCostPerOrder;

    /**
     * Break-even order value.
     */
    private BigDecimal breakEvenOrderValue;

    /**
     * Growth rate compared to previous period.
     */
    private BigDecimal growthRate;

    /**
     * Previous period contribution margin.
     */
    private BigDecimal previousPeriodMargin;

    /**
     * Revenue breakdown.
     */
    private RevenueBreakdownDto revenueBreakdown;

    /**
     * Cost breakdown.
     */
    private CostBreakdownDto costBreakdown;

    /**
     * Unit economics by restaurant tier.
     */
    private List<UnitEconomicsByTierDto> byRestaurantTier;

    /**
     * Unit economics by order value range.
     */
    private List<UnitEconomicsByOrderValueDto> byOrderValue;

    /**
     * Daily contribution margin trend.
     */
    private List<DailyContributionMarginDto> dailyTrend;

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
    public static class RevenueBreakdownDto {
        private BigDecimal commissionRevenue;
        private BigDecimal commissionPercentage;
        private BigDecimal deliveryFeeRevenue;
        private BigDecimal deliveryFeePercentage;
        private BigDecimal otherRevenue;
        private BigDecimal otherPercentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CostBreakdownDto {
        private BigDecimal courierBasePay;
        private BigDecimal courierBonuses;
        private BigDecimal courierTips; // pass-through, not a cost
        private BigDecimal promotionCosts;
        private BigDecimal giftCardCosts;
        private BigDecimal referralCosts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnitEconomicsByTierDto {
        private String tier;
        private Long orderCount;
        private BigDecimal gmv;
        private BigDecimal revenue;
        private BigDecimal costs;
        private BigDecimal contributionMargin;
        private BigDecimal unitEconomics;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnitEconomicsByOrderValueDto {
        private String orderValueRange; // e.g., "$0-20", "$20-50", "$50-100", "$100+"
        private Long orderCount;
        private BigDecimal averageOrderValue;
        private BigDecimal revenue;
        private BigDecimal costs;
        private BigDecimal contributionMargin;
        private BigDecimal unitEconomics;
        private Boolean profitable;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyContributionMarginDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private java.time.LocalDate date;
        private BigDecimal gmv;
        private BigDecimal revenue;
        private BigDecimal costs;
        private BigDecimal contributionMargin;
        private BigDecimal marginPercentage;
        private Long orderCount;
        private BigDecimal unitEconomics;
    }
}
