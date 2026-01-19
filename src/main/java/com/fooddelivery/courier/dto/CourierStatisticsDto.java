package com.fooddelivery.courier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for courier statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierStatisticsDto {

    private long totalCouriers;

    private long pendingApproval;

    private long online;

    private long offline;

    private long suspended;

    private long verified;

    private long available;

    private long busy;

    private long onBreak;
}
