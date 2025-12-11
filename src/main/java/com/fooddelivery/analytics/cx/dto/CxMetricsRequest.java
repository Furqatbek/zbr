package com.fooddelivery.analytics.cx.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Request DTO for CX metrics queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CxMetricsRequest {

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    // Optional filters
    private Long restaurantId;
    private Long courierId;
    private Long userId;
    private String country;
    private String platform; // IOS, ANDROID
    private String ticketType;
    private String ticketStatus;

    // Aggregation options
    @Builder.Default
    private Boolean includeDistribution = true;

    @Builder.Default
    private Boolean includeTrend = false;

    @Builder.Default
    private Boolean includeTopPerformers = false;

    @Builder.Default
    private Integer topN = 10;

    @Builder.Default
    private String granularity = "DAY"; // DAY, WEEK, MONTH

    // SLA configuration
    @Builder.Default
    private Integer slaHours = 24; // Default SLA for ticket resolution
}
