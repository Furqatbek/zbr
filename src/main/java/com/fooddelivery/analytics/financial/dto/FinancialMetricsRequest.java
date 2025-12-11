package com.fooddelivery.analytics.financial.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for financial metrics queries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialMetricsRequest {

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
     * Optional restaurant IDs to filter by.
     */
    private List<Long> restaurantIds;

    /**
     * Optional courier IDs to filter by.
     */
    private List<Long> courierIds;

    /**
     * Optional zone IDs to filter by.
     */
    private List<Long> zoneIds;

    /**
     * Optional category filter.
     */
    private List<String> categories;

    /**
     * Whether to include daily breakdown.
     */
    @Builder.Default
    private Boolean includeDailyTrend = true;

    /**
     * Whether to include restaurant breakdown.
     */
    @Builder.Default
    private Boolean includeRestaurantBreakdown = false;

    /**
     * Whether to include courier breakdown.
     */
    @Builder.Default
    private Boolean includeCourierBreakdown = false;

    /**
     * Maximum number of items in breakdown lists.
     */
    @Builder.Default
    private Integer topN = 10;

    /**
     * Compare with previous period.
     */
    @Builder.Default
    private Boolean compareToPreviousPeriod = true;
}
