package com.fooddelivery.notification.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Notification entity for persistent notification storage.
 * Supports targeting by user ID and/or role for broadcasts.
 */
@Entity
@Table(name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_user_id", columnList = "user_id"),
                @Index(name = "idx_notifications_role", columnList = "role"),
                @Index(name = "idx_notifications_read_at", columnList = "read_at"),
                @Index(name = "idx_notifications_created_at", columnList = "created_at DESC"),
                @Index(name = "idx_notifications_category", columnList = "category"),
                @Index(name = "idx_notifications_order_id", columnList = "order_id"),
                @Index(name = "idx_notifications_restaurant_id", columnList = "restaurant_id"),
                @Index(name = "idx_notifications_user_role_read", columnList = "user_id, role, read_at"),
                @Index(name = "idx_notifications_expires_at", columnList = "expires_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Target user ID. Nullable for role-based broadcasts.
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * Target role for the notification.
     */
    @Column(name = "role", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationRole role;

    /**
     * Notification title.
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /**
     * Notification message body.
     */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Notification category for filtering and grouping.
     */
    @Column(name = "category", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationCategory category;

    /**
     * Notification type/event that triggered this notification.
     */
    @Column(name = "notification_type", nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    /**
     * Priority level of the notification.
     */
    @Column(name = "priority", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;

    /**
     * Related order ID, if applicable.
     */
    @Column(name = "order_id")
    private Long orderId;

    /**
     * Related restaurant ID, for restaurant-specific notifications.
     */
    @Column(name = "restaurant_id")
    private Long restaurantId;

    /**
     * Related entity ID (restaurant, courier, ticket, etc.)
     */
    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    /**
     * Type of related entity.
     */
    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    /**
     * When the notification was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When the notification was read. Null if unread.
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * When the notification expires and can be auto-cleaned.
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Additional metadata stored as JSON.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Optional action URL for the notification.
     */
    @Column(name = "action_url", length = 500)
    private String actionUrl;

    /**
     * Optional icon identifier.
     */
    @Column(name = "icon", length = 100)
    private String icon;

    /**
     * Flag indicating if the notification has been dismissed (soft delete).
     */
    @Column(name = "dismissed")
    @Builder.Default
    private Boolean dismissed = false;

    /**
     * When the notification was dismissed.
     */
    @Column(name = "dismissed_at")
    private LocalDateTime dismissedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        if (dismissed == null) {
            dismissed = false;
        }
        if (priority == null) {
            priority = NotificationPriority.NORMAL;
        }
    }

    /**
     * Check if the notification has been read.
     */
    public boolean isRead() {
        return readAt != null;
    }

    /**
     * Check if the notification is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Mark the notification as read.
     */
    public void markAsRead() {
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    /**
     * Dismiss the notification.
     */
    public void dismiss() {
        this.dismissed = true;
        this.dismissedAt = LocalDateTime.now();
    }
}
