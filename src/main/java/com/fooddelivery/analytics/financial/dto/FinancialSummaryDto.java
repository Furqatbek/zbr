package com.fooddelivery.analytics.financial.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Summary DTO providing high-level financial overview.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialSummaryDto {

    /**
     * Gross Merchandise Value.
     */
    private BigDecimal gmv;

    /**
     * GMV growth rate (percentage).
     */
    private BigDecimal gmvGrowthRate;

    /**
     * Total revenue (commission + delivery fees).
     */
    private BigDecimal totalRevenue;

    /**
     * Revenue growth rate (percentage).
     */
    private BigDecimal revenueGrowthRate;

    /**
     * Commission revenue.
     */
    private BigDecimal commissionRevenue;

    /**
     * Effective commission rate.
     */
    private BigDecimal effectiveCommissionRate;

    /**
     * Net delivery fee revenue.
     */
    private BigDecimal deliveryFeeRevenue;

    /**
     * Total promotion costs (platform-funded).
     */
    private BigDecimal promotionCosts;

    /**
     * Courier costs.
     */
    private BigDecimal courierCosts;

    /**
     * Contribution margin.
     */
    private BigDecimal contributionMargin;

    /**
     * Contribution margin percentage.
     */
    private BigDecimal contributionMarginPercentage;

    /**
     * Unit economics (CM per order).
     */
    private BigDecimal unitEconomics;

    /**
     * Total orders.
     */
    private Long totalOrders;

    /**
     * Average order value.
     */
    private BigDecimal averageOrderValue;

    /**
     * Restaurant payouts pending.
     */
    private BigDecimal restaurantPayoutsPending;

    /**
     * Courier payouts pending.
     */
    private BigDecimal courierPayoutsPending;

    /**
     * Active restaurants.
     */
    private Long activeRestaurants;

    /**
     * Active couriers.
     */
    private Long activeCouriers;

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
}
