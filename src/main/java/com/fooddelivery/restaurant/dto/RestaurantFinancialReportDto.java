package com.fooddelivery.restaurant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fooddelivery.analytics.financial.dto.GmvMetricsDto;
import com.fooddelivery.analytics.financial.dto.RestaurantPayoutMetricsDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for restaurant financial report.
 * Combines revenue metrics and payout information for a specific restaurant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Restaurant financial report with revenue, payouts, and order metrics")
public class RestaurantFinancialReportDto {

    @Schema(description = "Restaurant ID")
    private Long restaurantId;

    @Schema(description = "Start of the reporting period")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime periodStart;

    @Schema(description = "End of the reporting period")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime periodEnd;

    // Revenue metrics
    @Schema(description = "Total revenue (GMV) for the period")
    private BigDecimal totalRevenue;

    @Schema(description = "Total number of orders")
    private Long totalOrders;

    @Schema(description = "Average order value")
    private BigDecimal averageOrderValue;

    @Schema(description = "Revenue from food items only")
    private BigDecimal foodRevenue;

    @Schema(description = "Revenue from delivery fees")
    private BigDecimal deliveryFeeRevenue;

    @Schema(description = "Revenue from tips")
    private BigDecimal tipRevenue;

    @Schema(description = "Growth rate compared to previous period (percentage)")
    private BigDecimal growthRate;

    // Payout metrics
    @Schema(description = "Gross sales before deductions")
    private BigDecimal grossSales;

    @Schema(description = "Total commissions deducted")
    private BigDecimal commissionsDeducted;

    @Schema(description = "Total delivery subsidies deducted")
    private BigDecimal deliverySubsidies;

    @Schema(description = "Total promotion costs deducted")
    private BigDecimal promotionCosts;

    @Schema(description = "Total adjustments (positive or negative)")
    private BigDecimal adjustments;

    @Schema(description = "Total fees deducted")
    private BigDecimal fees;

    @Schema(description = "Net payout amount")
    private BigDecimal netPayout;

    @Schema(description = "Payouts pending processing")
    private BigDecimal pendingPayouts;

    @Schema(description = "Payouts completed")
    private BigDecimal completedPayouts;

    // Trends
    @Schema(description = "Daily revenue trend")
    private List<GmvMetricsDto.DailyGmvDto> dailyRevenueTrend;

    @Schema(description = "Daily payout trend")
    private List<RestaurantPayoutMetricsDto.DailyPayoutDto> dailyPayoutTrend;
}
