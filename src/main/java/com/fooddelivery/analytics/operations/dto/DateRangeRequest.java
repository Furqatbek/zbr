package com.fooddelivery.analytics.operations.dto;

import com.fooddelivery.common.time.BusinessTime;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Request DTO for date range queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Date range for analytics queries")
public class DateRangeRequest {

    @Schema(description = "Start date (inclusive)", example = "2024-01-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull(message = "Start date is required")
    @PastOrPresent(message = "Start date cannot be in the future")
    private LocalDate startDate;

    @Schema(description = "End date (inclusive)", example = "2024-01-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull(message = "End date is required")
    private LocalDate endDate;

    /**
     * Get start datetime (start of day).
     */
    public LocalDateTime getStartDateTime() {
        return startDate != null ? BusinessTime.startOfDay(startDate) : null;
    }

    /**
     * Get end datetime (end of day).
     */
    public LocalDateTime getEndDateTime() {
        return endDate != null ? BusinessTime.endOfDayInclusive(endDate) : null;
    }

    /**
     * Validate that start is before or equal to end.
     */
    public boolean isValid() {
        if (startDate == null || endDate == null) {
            return false;
        }
        return !startDate.isAfter(endDate);
    }

    /**
     * Get number of days in range.
     */
    public long getDaysInRange() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /**
     * Create a date range for the last N days.
     */
    public static DateRangeRequest lastNDays(int days) {
        LocalDate end = BusinessTime.today();
        LocalDate start = end.minusDays(days - 1);
        return DateRangeRequest.builder()
                .startDate(start)
                .endDate(end)
                .build();
    }

    /**
     * Create a date range for today.
     */
    public static DateRangeRequest today() {
        LocalDate today = BusinessTime.today();
        return DateRangeRequest.builder()
                .startDate(today)
                .endDate(today)
                .build();
    }

    /**
     * Create a date range for this week.
     */
    public static DateRangeRequest thisWeek() {
        LocalDate today = BusinessTime.today();
        LocalDate startOfWeek = today.with(java.time.DayOfWeek.MONDAY);
        return DateRangeRequest.builder()
                .startDate(startOfWeek)
                .endDate(today)
                .build();
    }

    /**
     * Create a date range for this month.
     */
    public static DateRangeRequest thisMonth() {
        LocalDate today = BusinessTime.today();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        return DateRangeRequest.builder()
                .startDate(startOfMonth)
                .endDate(today)
                .build();
    }
}
