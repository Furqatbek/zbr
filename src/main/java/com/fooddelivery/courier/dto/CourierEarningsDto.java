package com.fooddelivery.courier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierEarningsDto {
    private BigDecimal todayEarnings;
    private BigDecimal weekEarnings;
    private BigDecimal monthEarnings;
    private BigDecimal totalEarnings;
    private Integer todayDeliveries;
    private Integer weekDeliveries;
    private Integer monthDeliveries;
    private Integer totalDeliveries;
    private BigDecimal averagePerDelivery;
    private BigDecimal pendingPayout;
}
