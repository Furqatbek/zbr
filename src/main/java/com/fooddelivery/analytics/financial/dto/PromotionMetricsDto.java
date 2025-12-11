package com.fooddelivery.analytics.financial.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fooddelivery.analytics.financial.model.PromotionFunder;
import com.fooddelivery.analytics.financial.model.PromotionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Promotion and Discount metrics.
 * Tracks all promotional costs and their effectiveness.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionMetricsDto {

    /**
     * Total discount value given to customers.
     */
    private BigDecimal totalDiscountValue;

    /**
     * Total promotion cost borne by platform.
     */
    private BigDecimal platformCost;

    /**
     * Total promotion cost borne by restaurants.
     */
    private BigDecimal restaurantCost;

    /**
     * Number of orders with promotions applied.
     */
    private Long promotedOrderCount;

    /**
     * Total orders in the period.
     */
    private Long totalOrderCount;

    /**
     * Promotion usage rate (promoted orders / total orders).
     */
    private BigDecimal promotionUsageRate;

    /**
     * Average discount per promoted order.
     */
    private BigDecimal averageDiscountPerOrder;

    /**
     * Total gift card redemptions.
     */
    private BigDecimal giftCardRedemptions;

    /**
     * Total referral rewards paid.
     */
    private BigDecimal referralRewardsPaid;

    /**
     * Estimated incremental revenue from promotions.
     */
    private BigDecimal estimatedIncrementalRevenue;

    /**
     * Return on promotion spend (incremental revenue / platform cost).
     */
    private BigDecimal promotionRoi;

    /**
     * Breakdown by promotion type.
     */
    private List<PromotionByTypeDto> byType;

    /**
     * Breakdown by funder.
     */
    private List<PromotionByFunderDto> byFunder;

    /**
     * Top promotions by usage.
     */
    private List<TopPromotionDto> topPromotions;

    /**
     * Daily promotion trend.
     */
    private List<DailyPromotionDto> dailyTrend;

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
    public static class PromotionByTypeDto {
        private PromotionType promotionType;
        private BigDecimal totalValue;
        private Long usageCount;
        private BigDecimal averageDiscount;
        private BigDecimal percentageOfTotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PromotionByFunderDto {
        private PromotionFunder funder;
        private BigDecimal totalCost;
        private Long promotionCount;
        private BigDecimal percentageOfTotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopPromotionDto {
        private Long promotionId;
        private String promotionCode;
        private String promotionName;
        private PromotionType type;
        private BigDecimal totalDiscountGiven;
        private Long usageCount;
        private BigDecimal platformCost;
        private BigDecimal restaurantCost;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyPromotionDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private java.time.LocalDate date;
        private BigDecimal totalDiscount;
        private BigDecimal platformCost;
        private BigDecimal restaurantCost;
        private Long promotedOrders;
        private BigDecimal usageRate;
    }
}
