package com.fooddelivery.analytics.financial.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fooddelivery.analytics.financial.model.BonusType;
import com.fooddelivery.analytics.financial.model.CourierPaymentType;
import com.fooddelivery.analytics.financial.model.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Courier Payout metrics.
 * Courier Payout = Base Pay + Distance Bonus + Tips + Peak Bonus + Incentives
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierPayoutMetricsDto {

    /**
     * Total base payments.
     */
    private BigDecimal totalBasePayments;

    /**
     * Total distance bonuses.
     */
    private BigDecimal totalDistanceBonuses;

    /**
     * Total tips received by couriers.
     */
    private BigDecimal totalTips;

    /**
     * Total peak hour bonuses.
     */
    private BigDecimal totalPeakBonuses;

    /**
     * Total incentives/bonuses paid.
     */
    private BigDecimal totalIncentives;

    /**
     * Total courier payouts.
     */
    private BigDecimal totalPayouts;

    /**
     * Number of active couriers.
     */
    private Long activeCourierCount;

    /**
     * Total deliveries completed.
     */
    private Long totalDeliveries;

    /**
     * Average payout per delivery.
     */
    private BigDecimal averagePayoutPerDelivery;

    /**
     * Average payout per courier.
     */
    private BigDecimal averagePayoutPerCourier;

    /**
     * Average deliveries per courier.
     */
    private BigDecimal averageDeliveriesPerCourier;

    /**
     * Average tip per delivery.
     */
    private BigDecimal averageTipPerDelivery;

    /**
     * Tip rate (deliveries with tips / total deliveries).
     */
    private BigDecimal tipRate;

    /**
     * Pending payouts.
     */
    private BigDecimal pendingPayouts;

    /**
     * Completed payouts.
     */
    private BigDecimal completedPayouts;

    /**
     * Breakdown by payment type.
     */
    private List<PayoutByTypeDto> byPaymentType;

    /**
     * Breakdown by bonus type.
     */
    private List<BonusByTypeDto> byBonusType;

    /**
     * Breakdown by payout status.
     */
    private List<PayoutByStatusDto> byStatus;

    /**
     * Top couriers by earnings.
     */
    private List<CourierPayoutDetailDto> topCouriers;

    /**
     * Daily payout trend.
     */
    private List<DailyCourierPayoutDto> dailyTrend;

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
    public static class PayoutByTypeDto {
        private CourierPaymentType paymentType;
        private BigDecimal totalAmount;
        private Long count;
        private BigDecimal percentageOfTotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BonusByTypeDto {
        private BonusType bonusType;
        private BigDecimal totalAmount;
        private Long recipientCount;
        private BigDecimal averageBonus;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PayoutByStatusDto {
        private PaymentStatus status;
        private BigDecimal totalAmount;
        private Long count;
        private BigDecimal percentageOfTotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourierPayoutDetailDto {
        private Long courierId;
        private String courierName;
        private BigDecimal totalEarnings;
        private BigDecimal baseEarnings;
        private BigDecimal bonusEarnings;
        private BigDecimal tipEarnings;
        private Long deliveryCount;
        private BigDecimal averagePerDelivery;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyCourierPayoutDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private java.time.LocalDate date;
        private BigDecimal totalPayouts;
        private BigDecimal basePayments;
        private BigDecimal bonuses;
        private BigDecimal tips;
        private Long activeCouriers;
        private Long deliveries;
    }
}
