package com.fooddelivery.analytics.financial.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fooddelivery.analytics.financial.model.DisputeStatus;
import com.fooddelivery.analytics.financial.model.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Restaurant Payout metrics.
 * Net Payout = Gross Sales - Commission - Delivery Subsidies - Promotion Costs + Adjustments - Fees
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantPayoutMetricsDto {

    /**
     * Total gross sales (before deductions).
     */
    private BigDecimal totalGrossSales;

    /**
     * Total commissions deducted.
     */
    private BigDecimal totalCommissionsDeducted;

    /**
     * Total delivery subsidies deducted.
     */
    private BigDecimal totalDeliverySubsidies;

    /**
     * Total promotion costs deducted from restaurants.
     */
    private BigDecimal totalPromotionCosts;

    /**
     * Total adjustments (positive or negative).
     */
    private BigDecimal totalAdjustments;

    /**
     * Total fees deducted.
     */
    private BigDecimal totalFees;

    /**
     * Net payout amount.
     */
    private BigDecimal netPayoutAmount;

    /**
     * Number of restaurants with payouts.
     */
    private Long restaurantCount;

    /**
     * Average payout per restaurant.
     */
    private BigDecimal averagePayoutPerRestaurant;

    /**
     * Payouts pending processing.
     */
    private BigDecimal pendingPayouts;

    /**
     * Payouts completed.
     */
    private BigDecimal completedPayouts;

    /**
     * Failed payouts requiring attention.
     */
    private BigDecimal failedPayouts;

    /**
     * Average payout processing time (hours).
     */
    private BigDecimal averageProcessingTime;

    /**
     * Total disputes raised.
     */
    private Long disputeCount;

    /**
     * Dispute amount pending resolution.
     */
    private BigDecimal disputeAmountPending;

    /**
     * Breakdown by payout status.
     */
    private List<PayoutByStatusDto> byStatus;

    /**
     * Top restaurants by payout amount.
     */
    private List<RestaurantPayoutDetailDto> topRestaurants;

    /**
     * Dispute summary.
     */
    private List<DisputeSummaryDto> disputeSummary;

    /**
     * Daily payout trend.
     */
    private List<DailyPayoutDto> dailyTrend;

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
    public static class PayoutByStatusDto {
        private PaymentStatus status;
        private BigDecimal totalAmount;
        private Long payoutCount;
        private BigDecimal percentageOfTotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RestaurantPayoutDetailDto {
        private Long restaurantId;
        private String restaurantName;
        private BigDecimal grossSales;
        private BigDecimal commissionsDeducted;
        private BigDecimal netPayout;
        private Long orderCount;
        private BigDecimal effectiveCommissionRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DisputeSummaryDto {
        private DisputeStatus status;
        private Long count;
        private BigDecimal totalAmount;
        private BigDecimal averageResolutionDays;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyPayoutDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private java.time.LocalDate date;
        private BigDecimal grossSales;
        private BigDecimal netPayouts;
        private Long restaurantCount;
        private Long disputeCount;
    }
}
