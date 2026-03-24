package com.fooddelivery.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO representing all delivery fee settings as a single object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryFeeSettingsDto {
    private BigDecimal baseFee;
    private BigDecimal perKmFee;
    private BigDecimal minFee;
    private BigDecimal maxFee;
    private BigDecimal peakHourSurcharge;
    private Integer peakStartHour;
    private Integer peakEndHour;
    private Integer eveningPeakStartHour;
    private Integer eveningPeakEndHour;
    private Boolean routingEnabled;
    private String routingOsrmBaseUrl;
    private Integer routingConnectTimeout;
    private Integer routingReadTimeout;
}
