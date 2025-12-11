package com.fooddelivery.notification.dto;

import com.fooddelivery.notification.model.NotificationCategory;
import com.fooddelivery.notification.model.NotificationPriority;
import com.fooddelivery.notification.model.NotificationRole;
import com.fooddelivery.notification.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Map;

/**
 * DTO for creating a new notification.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCreateDto {

    /**
     * Target user ID. Optional for role-based broadcasts.
     */
    private Long userId;

    /**
     * Target role for the notification.
     */
    @NotNull(message = "Role is required")
    private NotificationRole role;

    /**
     * Notification title.
     */
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    /**
     * Notification message body.
     */
    @NotBlank(message = "Message is required")
    private String message;

    /**
     * Notification category.
     */
    @NotNull(message = "Category is required")
    private NotificationCategory category;

    /**
     * Notification type/event.
     */
    @NotNull(message = "Notification type is required")
    private NotificationType notificationType;

    /**
     * Priority level. Defaults to NORMAL if not specified.
     */
    private NotificationPriority priority;

    /**
     * Related order ID, if applicable.
     */
    private Long orderId;

    /**
     * Related entity ID (restaurant, courier, ticket, etc.)
     */
    private Long relatedEntityId;

    /**
     * Type of related entity.
     */
    private String relatedEntityType;

    /**
     * Additional metadata.
     */
    private Map<String, Object> metadata;

    /**
     * Optional action URL.
     */
    @Size(max = 500, message = "Action URL must not exceed 500 characters")
    private String actionUrl;

    /**
     * Optional icon identifier.
     */
    @Size(max = 100, message = "Icon must not exceed 100 characters")
    private String icon;

    /**
     * TTL in hours. Null means no expiration.
     */
    private Integer ttlHours;
}
