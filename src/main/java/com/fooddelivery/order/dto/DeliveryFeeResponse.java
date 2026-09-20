package com.fooddelivery.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for delivery fee calculation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Delivery fee calculation response")
public class DeliveryFeeResponse {

    @Schema(description = "Calculated delivery fee")
    private BigDecimal deliveryFee;

    @Schema(description = "Base fee component from settings")
    private BigDecimal baseFee;

    @Schema(description = "Per-kilometer fee rate from settings")
    private BigDecimal perKmFee;

    @Schema(description = "Distance-based fee component (perKmFee * distanceKm)")
    private BigDecimal distanceFee;

    @Schema(description = "Distance in kilometers between restaurant and delivery location")
    private Double distanceKm;

    @Schema(description = "Whether the delivery location is within the restaurant's delivery radius")
    private boolean withinDeliveryRadius;

    @Schema(description = "Restaurant's delivery radius in kilometers")
    private Integer deliveryRadiusKm;

    @Schema(description = "Whether peak hour surcharge is applied")
    private boolean peakHourSurchargeApplied;

    @Schema(description = "Peak hour surcharge amount (if applied)")
    private BigDecimal peakHourSurcharge;

    @Schema(description = "Minimum fee cap applied")
    private BigDecimal minFee;

    @Schema(description = "Maximum fee cap applied")
    private BigDecimal maxFee;

    // Arrival time, from the same distance the fee was computed from. Null when
    // there were no coordinates to measure — the same condition that falls the
    // fee back to the base rate.

    @Schema(description = "Kitchen time in minutes used in the estimate")
    private Integer prepMinutes;

    @Schema(description = "Courier time in minutes: travel plus parking and handover")
    private Integer travelMinutes;

    @Schema(description = "Lower bound in minutes for the food to arrive")
    private Integer etaMinutesMin;

    @Schema(description = "Upper bound in minutes for the food to arrive")
    private Integer etaMinutesMax;
}
