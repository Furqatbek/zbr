package com.fooddelivery.notification.dto;

import com.fooddelivery.notification.model.NotificationCategory;
import com.fooddelivery.notification.model.NotificationPriority;
import com.fooddelivery.notification.model.NotificationRole;
import com.fooddelivery.notification.model.NotificationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for filtering notifications in list queries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationFilterDto {

    /**
     * Filter by user ID.
     */
    private Long userId;

    /**
     * Filter by role.
     */
    private NotificationRole role;

    /**
     * Filter by multiple roles.
     */
    private List<NotificationRole> roles;

    /**
     * Filter by read status.
     * True = read only, False = unread only, null = all
     */
    private Boolean isRead;

    /**
     * Filter by category.
     */
    private NotificationCategory category;

    /**
     * Filter by multiple categories.
     */
    private List<NotificationCategory> categories;

    /**
     * Filter by notification type.
     */
    private NotificationType notificationType;

    /**
     * Filter by priority.
     */
    private NotificationPriority priority;

    /**
     * Filter by minimum priority.
     */
    private NotificationPriority minPriority;

    /**
     * Filter by order ID.
     */
    private Long orderId;

    /**
     * Filter by related entity ID.
     */
    private Long relatedEntityId;

    /**
     * Filter by related entity type.
     */
    private String relatedEntityType;

    /**
     * Filter by creation date - start (inclusive).
     */
    private LocalDateTime createdFrom;

    /**
     * Filter by creation date - end (inclusive).
     */
    private LocalDateTime createdTo;

    /**
     * Include dismissed notifications. Default: false.
     */
    @Builder.Default
    private Boolean includeDismissed = false;

    /**
     * Include expired notifications. Default: false.
     */
    @Builder.Default
    private Boolean includeExpired = false;

    /**
     * Search term for title/message.
     */
    private String searchTerm;

    /**
     * Page number (0-indexed).
     */
    @Builder.Default
    private Integer page = 0;

    /**
     * Page size.
     */
    @Builder.Default
    private Integer pageSize = 20;

    /**
     * Sort field.
     */
    @Builder.Default
    private String sortBy = "createdAt";

    /**
     * Sort direction (ASC or DESC).
     */
    @Builder.Default
    private String sortDirection = "DESC";
}
