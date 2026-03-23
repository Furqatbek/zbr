package com.fooddelivery.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for updating delivery fee settings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeliveryFeeSettingsRequest {

    @DecimalMin(value = "0.0", message = "Base fee must be non-negative")
    private BigDecimal baseFee;

    @DecimalMin(value = "0.0", message = "Per-km fee must be non-negative")
    private BigDecimal perKmFee;

    @DecimalMin(value = "0.0", message = "Minimum fee must be non-negative")
    private BigDecimal minFee;

    @DecimalMin(value = "0.0", message = "Maximum fee must be non-negative")
    private BigDecimal maxFee;

    @DecimalMin(value = "0.0", message = "Peak hour surcharge must be non-negative")
    private BigDecimal peakHourSurcharge;

    @Min(value = 0, message = "Peak start hour must be between 0 and 23")
    @Max(value = 23, message = "Peak start hour must be between 0 and 23")
    private Integer peakStartHour;

    @Min(value = 0, message = "Peak end hour must be between 0 and 23")
    @Max(value = 23, message = "Peak end hour must be between 0 and 23")
    private Integer peakEndHour;

    @Min(value = 0, message = "Evening peak start hour must be between 0 and 23")
    @Max(value = 23, message = "Evening peak start hour must be between 0 and 23")
    private Integer eveningPeakStartHour;

    @Min(value = 0, message = "Evening peak end hour must be between 0 and 23")
    @Max(value = 23, message = "Evening peak end hour must be between 0 and 23")
    private Integer eveningPeakEndHour;
}
