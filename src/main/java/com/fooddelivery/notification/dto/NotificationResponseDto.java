package com.fooddelivery.notification.dto;

import com.fooddelivery.notification.model.NotificationCategory;
import com.fooddelivery.notification.model.NotificationPriority;
import com.fooddelivery.notification.model.NotificationRole;
import com.fooddelivery.notification.model.NotificationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for notification response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseDto {

    private Long id;

    private Long userId;

    private NotificationRole role;

    private String roleDisplayName;

    private String title;

    private String message;

    private NotificationCategory category;

    private String categoryDisplayName;

    private NotificationType notificationType;

    private String notificationTypeDisplayName;

    private NotificationPriority priority;

    private String priorityDisplayName;

    private Long orderId;

    private Long relatedEntityId;

    private String relatedEntityType;

    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    private LocalDateTime expiresAt;

    private Map<String, Object> metadata;

    private String actionUrl;

    private String icon;

    private Boolean dismissed;

    private LocalDateTime dismissedAt;

    /**
     * Computed fields
     */
    private Boolean isRead;

    private Boolean isExpired;

    private String timeAgo;
}
