package com.fooddelivery.admin.dashboard.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fooddelivery.common.util.FlexibleLocalDateTimeDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for dashboard filter requests.
 * Used to filter data across all dashboard panels.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardFilterRequest {

    // Date range - accepts both date (yyyy-MM-dd) and datetime (yyyy-MM-dd'T'HH:mm:ss) formats
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime startDate;

    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime endDate;
    private LocalDate date; // For single day queries

    // Entity filters
    private Long restaurantId;
    private List<Long> restaurantIds;
    private Long courierId;
    private List<Long> courierIds;
    private Long customerId;

    // Status filters
    private List<String> orderStatuses;
    private List<String> ticketStatuses;
    private List<String> ticketTypes;
    private List<String> ticketPriorities;

    // Order filters
    private String orderType; // DELIVERY, PICKUP, DINE_IN

    // Stuck order thresholds (in minutes)
    private Integer pendingThresholdMinutes;
    private Integer acceptedThresholdMinutes;
    private Integer preparingThresholdMinutes;
    private Integer readyThresholdMinutes;
    private Integer inTransitThresholdMinutes;

    // Pagination
    private Integer page;
    private Integer size;
    private Integer pageSize; // Alias for size
    private String sortBy;
    private String sortDirection; // ASC, DESC

    // Include flags
    private Boolean includeInactive;
    private Boolean includeDeleted;

    /**
     * Create default filter for today.
     */
    public static DashboardFilterRequest today() {
        LocalDateTime now = LocalDateTime.now();
        return DashboardFilterRequest.builder()
                .startDate(now.toLocalDate().atStartOfDay())
                .endDate(now)
                .page(0)
                .size(50)
                .sortBy("createdAt")
                .sortDirection("DESC")
                .build();
    }

    /**
     * Create default filter with stuck thresholds.
     */
    public static DashboardFilterRequest withDefaultStuckThresholds() {
        return DashboardFilterRequest.builder()
                .pendingThresholdMinutes(10)
                .acceptedThresholdMinutes(15)
                .preparingThresholdMinutes(30)
                .readyThresholdMinutes(15)
                .inTransitThresholdMinutes(45)
                .page(0)
                .size(50)
                .sortBy("createdAt")
                .sortDirection("DESC")
                .build();
    }

    /**
     * Get effective start date (defaults to start of today).
     */
    public LocalDateTime getEffectiveStartDate() {
        if (startDate != null) {
            return startDate;
        }
        if (date != null) {
            return date.atStartOfDay();
        }
        return LocalDateTime.now().toLocalDate().atStartOfDay();
    }

    /**
     * Get effective end date (defaults to now).
     */
    public LocalDateTime getEffectiveEndDate() {
        if (endDate != null) {
            return endDate;
        }
        if (date != null) {
            return date.plusDays(1).atStartOfDay();
        }
        return LocalDateTime.now();
    }

    /**
     * Get page number with default.
     */
    public int getPageNumber() {
        return page != null ? page : 0;
    }

    /**
     * Get page size with default.
     */
    public int getPageSize() {
        if (pageSize != null) return pageSize;
        return size != null ? size : 50;
    }

    /**
     * Set page size.
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
        this.size = pageSize;
    }

    /**
     * Check if page size is explicitly set.
     */
    public boolean isPageSizeSet() {
        return pageSize != null || size != null;
    }

    /**
     * Check if page is explicitly set.
     */
    public boolean isPageSet() {
        return page != null;
    }
}
