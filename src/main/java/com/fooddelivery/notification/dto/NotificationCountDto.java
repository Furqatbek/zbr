package com.fooddelivery.notification.dto;

import lombok.*;

import java.util.Map;

/**
 * DTO for notification counts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCountDto {

    /**
     * Total notification count.
     */
    private Long total;

    /**
     * Unread notification count.
     */
    private Long unread;

    /**
     * Read notification count.
     */
    private Long read;

    /**
     * Unread count by category.
     */
    private Map<String, Long> unreadByCategory;

    /**
     * Unread count by priority.
     */
    private Map<String, Long> unreadByPriority;

    /**
     * Count of urgent notifications.
     */
    private Long urgentCount;

    /**
     * Count of high priority notifications.
     */
    private Long highPriorityCount;
}
