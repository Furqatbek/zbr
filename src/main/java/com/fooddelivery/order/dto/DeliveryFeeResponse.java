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
}
