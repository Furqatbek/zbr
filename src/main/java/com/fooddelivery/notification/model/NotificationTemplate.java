package com.fooddelivery.notification.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Template entity for notification messages.
 * Allows customizable notification content per event type and role.
 */
@Entity
@Table(name = "notification_templates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notification_template_type_role",
                        columnNames = {"notification_type", "role"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The notification type this template is for.
     */
    @Column(name = "notification_type", nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    /**
     * The role this template targets.
     */
    @Column(name = "role", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationRole role;

    /**
     * Title template with placeholders (e.g., "Order #{orderId} has been {status}").
     */
    @Column(name = "title_template", nullable = false, length = 255)
    private String titleTemplate;

    /**
     * Message template with placeholders.
     */
    @Column(name = "message_template", nullable = false, columnDefinition = "TEXT")
    private String messageTemplate;

    /**
     * Optional icon identifier.
     */
    @Column(name = "icon", length = 100)
    private String icon;

    /**
     * Optional action URL template.
     */
    @Column(name = "action_url_template", length = 500)
    private String actionUrlTemplate;

    /**
     * Whether this template is active.
     */
    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    /**
     * Default TTL in hours for notifications using this template.
     */
    @Column(name = "default_ttl_hours")
    private Integer defaultTtlHours;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
