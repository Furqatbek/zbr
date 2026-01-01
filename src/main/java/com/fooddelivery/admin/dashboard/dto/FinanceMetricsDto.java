package com.fooddelivery.admin.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for finance metrics panel.
 * Provides financial analytics including revenue, commissions, payouts, and refunds.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceMetricsDto {

    // Gross Merchandise Value (GMV)
    private BigDecimal gmv;
    private BigDecimal gmvToday;
    private BigDecimal gmvThisWeek;
    private BigDecimal gmvThisMonth;
    private BigDecimal gmvYtd;

    // Platform Revenue
    private BigDecimal commissionRevenue;
    private BigDecimal commissionRevenueToday;
    private BigDecimal commissionRevenueThisWeek;
    private BigDecimal commissionRevenueThisMonth;
    private BigDecimal deliveryFeeRevenue;
    private BigDecimal deliveryFeeRevenueToday;
    private BigDecimal totalRevenue;
    private BigDecimal netRevenue;
    private BigDecimal totalPlatformRevenueToday;
    private BigDecimal commissionRate;

    // Order breakdown
    private Long totalOrders;
    private Long totalOrdersToday;
    private BigDecimal avgOrderValue;
    private BigDecimal avgCommissionPerOrder;

    // Payouts
    private BigDecimal restaurantPayouts;
    private BigDecimal courierPayouts;
    private BigDecimal discountsUsed;
    private BigDecimal refundsPaid;
    private BigDecimal unsettledRestaurantPayouts;
    private BigDecimal unsettledCourierPayouts;
    private PayoutSummaryDto payoutSummary;

    // Discounts & Promotions
    private DiscountSummaryDto discounts;
    private DiscountSummaryDto discountSummary;

    // Refunds
    private RefundSummaryDto refunds;
    private RefundSummaryDto refundSummary;

    // Unsettled amounts
    private BigDecimal totalUnsettledAmount;
    private Long unsettledPayoutsCount;

    // Daily revenue trend
    private List<DailyRevenueDto> dailyRevenue;
    private List<DailyRevenueDto> dailyRevenueTrend;

    // Revenue breakdown
    private Map<String, BigDecimal> revenueByCategory;
    private Map<String, BigDecimal> paymentMethodBreakdown;
    private Map<String, Object> periodComparison;

    // Top performing restaurants by revenue
    private List<EntityRevenueDto> topRestaurantsByRevenue;

    // Comparison
    private FinanceComparisonDto comparison;

    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayoutSummaryDto {
        private BigDecimal pendingPayouts;
        private BigDecimal processedToday;
        private BigDecimal processedThisWeek;
        private BigDecimal processedThisMonth;
        private Long pendingCount;
        private Long processedCountToday;
        private BigDecimal avgPayoutAmount;
        private LocalDateTime lastPayoutAt;

        // Total payouts
        private BigDecimal totalRestaurantPayouts;
        private BigDecimal totalCourierPayouts;

        // Pending payouts breakdown
        private Long pendingRestaurantPayouts;
        private Long pendingCourierPayouts;

        // Unsettled breakdown
        private Long unsettledCount;
        private BigDecimal unsettledAmount;
        private BigDecimal unsettledRestaurantAmount;
        private BigDecimal unsettledCourierAmount;
        private Long overdueCount; // Past scheduled payout date
        private BigDecimal overdueAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscountSummaryDto {
        private BigDecimal totalDiscounts;
        private BigDecimal totalDiscountsToday;
        private BigDecimal totalDiscountsThisWeek;
        private BigDecimal totalDiscountsThisMonth;
        private Long discountUsageCountToday;
        private Double avgDiscountPercent;

        // Orders with discounts
        private Long ordersWithDiscount;
        private Double discountedOrdersPercentage;
        private Double discountUsageRate;

        // By type
        private BigDecimal promoCodeDiscounts;
        private BigDecimal loyaltyDiscounts;
        private BigDecimal firstOrderDiscounts;
        private BigDecimal referralDiscounts;

        // Platform funded vs restaurant funded
        private BigDecimal platformFundedDiscounts;
        private BigDecimal restaurantFundedDiscounts;

        // Top promo codes
        private List<PromoCodeUsageDto> topPromoCodes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundSummaryDto {
        private BigDecimal totalRefundAmount;
        private BigDecimal totalRefundsToday;
        private BigDecimal totalRefundsThisWeek;
        private BigDecimal totalRefundsThisMonth;
        private Long refundCount;
        private Long refundCountToday;
        private Long refundCountThisWeek;
        private Long approvedRefunds;
        private Long pendingRefunds;
        private Long rejectedRefunds;
        private BigDecimal avgRefundAmount;
        private Double refundRate; // percentage of orders refunded

        // By reason
        private Map<String, BigDecimal> refundsByReason;

        // Pending refunds
        private Long pendingRefundCount;
        private BigDecimal pendingRefundAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenueDto {
        private LocalDate date;
        private BigDecimal gmv;
        private BigDecimal platformRevenue;
        private BigDecimal commissionRevenue;
        private BigDecimal deliveryFeeRevenue;
        private BigDecimal discounts;
        private Long orderCount;
        private BigDecimal avgOrderValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityRevenueDto {
        private Long entityId;
        private String entityName;
        private String entityType; // RESTAURANT
        private BigDecimal revenue;
        private BigDecimal commission;
        private Long orderCount;
        private Double percentageOfTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromoCodeUsageDto {
        private String promoCode;
        private Long usageCount;
        private BigDecimal totalDiscountAmount;
        private BigDecimal avgDiscountPerUse;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinanceComparisonDto {
        private BigDecimal gmvYesterday;
        private BigDecimal gmvLastWeek;
        private Double gmvChangePercentDay;
        private Double gmvChangePercentWeek;
        private Double revenueChangePercentDay;
        private Double revenueChangePercentWeek;
        private String trend; // UP, DOWN, STABLE
    }
}
