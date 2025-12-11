package com.fooddelivery.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDto {
    private int periodDays;
    private long totalOrders;
    private BigDecimal gmv;
    private BigDecimal averageOrderValue;
    private String currency;
}
