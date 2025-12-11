package com.fooddelivery.analytics.technical.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Request DTO for technical metrics queries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicalMetricsRequest {

    /**
     * Start of the reporting period.
     */
    @NotNull(message = "Start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;

    /**
     * End of the reporting period.
     */
    @NotNull(message = "End date is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;

    /**
     * Slow query threshold in milliseconds.
     */
    @Builder.Default
    private Long slowQueryThresholdMs = 1000L;

    /**
     * Number of top slow queries to return.
     */
    @Builder.Default
    private Integer topSlowQueriesLimit = 10;

    /**
     * Number of top endpoints to return.
     */
    @Builder.Default
    private Integer topEndpointsLimit = 10;

    /**
     * Include hourly distribution data.
     */
    @Builder.Default
    private Boolean includeHourlyDistribution = true;

    /**
     * Include daily trend data.
     */
    @Builder.Default
    private Boolean includeDailyTrend = true;

    /**
     * Include table-level stats.
     */
    @Builder.Default
    private Boolean includeTableStats = false;
}
