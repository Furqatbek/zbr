package com.fooddelivery.analytics.financial.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Delivery Fee metrics.
 * Tracks delivery fees charged to customers and paid to couriers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryFeeMetricsDto {

    /**
     * Total delivery fees collected from customers.
     */
    private BigDecimal totalDeliveryFeesCollected;

    /**
     * Total delivery fees paid to couriers.
     */
    private BigDecimal totalDeliveryFeesPaid;

    /**
     * Net delivery fee revenue (collected - paid).
     */
    private BigDecimal netDeliveryFeeRevenue;

    /**
     * Number of deliveries.
     */
    private Long deliveryCount;

    /**
     * Average delivery fee per order.
     */
    private BigDecimal averageDeliveryFee;

    /**
     * Average distance per delivery (km).
     */
    private BigDecimal averageDistance;

    /**
     * Total distance bonus paid to couriers.
     */
    private BigDecimal totalDistanceBonus;

    /**
     * Total peak hour surcharges collected.
     */
    private BigDecimal totalPeakSurcharge;

    /**
     * Delivery fee waived through promotions.
     */
    private BigDecimal totalFeesWaived;

    /**
     * Breakdown by distance range.
     */
    private List<DeliveryFeeByDistanceDto> byDistance;

    /**
     * Breakdown by time of day.
     */
    private List<DeliveryFeeByTimeDto> byTimeOfDay;

    /**
     * Breakdown by zone/area.
     */
    private List<DeliveryFeeByZoneDto> byZone;

    /**
     * Daily delivery fee trend.
     */
    private List<DailyDeliveryFeeDto> dailyTrend;

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
    public static class DeliveryFeeByDistanceDto {
        private String distanceRange; // e.g., "0-2km", "2-5km", "5-10km", "10+km"
        private BigDecimal totalFees;
        private Long deliveryCount;
        private BigDecimal averageFee;
        private BigDecimal percentageOfTotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeliveryFeeByTimeDto {
        private String timeSlot; // e.g., "Morning", "Lunch", "Afternoon", "Dinner", "Late Night"
        private BigDecimal totalFees;
        private Long deliveryCount;
        private BigDecimal averageFee;
        private Boolean isPeakHour;
        private BigDecimal peakSurchargeAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeliveryFeeByZoneDto {
        private String zoneName;
        private Long zoneId;
        private BigDecimal totalFees;
        private Long deliveryCount;
        private BigDecimal averageFee;
        private BigDecimal averageDistance;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyDeliveryFeeDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private java.time.LocalDate date;
        private BigDecimal feesCollected;
        private BigDecimal feesPaid;
        private BigDecimal netRevenue;
        private Long deliveryCount;
    }
}
