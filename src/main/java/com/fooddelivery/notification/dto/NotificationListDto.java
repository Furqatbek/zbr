package com.fooddelivery.notification.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * DTO for paginated notification list response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationListDto {

    /**
     * List of notifications.
     */
    private List<NotificationResponseDto> notifications;

    /**
     * Current page number (0-indexed).
     */
    private Integer page;

    /**
     * Number of items per page.
     */
    private Integer pageSize;

    /**
     * Total number of notifications matching the query.
     */
    private Long totalElements;

    /**
     * Total number of pages.
     */
    private Integer totalPages;

    /**
     * Whether there are more pages.
     */
    private Boolean hasNext;

    /**
     * Whether there is a previous page.
     */
    private Boolean hasPrevious;

    /**
     * Count of unread notifications for this query.
     */
    private Long unreadCount;

    /**
     * Breakdown by category.
     */
    private Map<String, Long> categoryBreakdown;

    /**
     * Breakdown by priority.
     */
    private Map<String, Long> priorityBreakdown;
}
