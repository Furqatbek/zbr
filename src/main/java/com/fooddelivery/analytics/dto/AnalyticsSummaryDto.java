package com.fooddelivery.analytics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO containing a quick summary of all key analytics metrics.
 * Useful for dashboards and quick overview.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryDto {

    /**
     * Daily Active Users.
     */
    private Long dailyActiveUsers;

    /**
     * Total orders placed today.
     */
    private Long ordersToday;

    /**
     * Overall conversion rate (percentage).
     */
    private Double conversionRate;

    /**
     * Average Order Value.
     */
    private BigDecimal averageOrderValue;

    /**
     * User churn rate (percentage).
     */
    private Double userChurnRate;

    /**
     * Restaurant churn rate (percentage).
     */
    private Double restaurantChurnRate;

    /**
     * Courier churn rate (percentage).
     */
    private Double courierChurnRate;

    /**
     * Timestamp when this summary was generated.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime generatedAt = LocalDateTime.now();
}
