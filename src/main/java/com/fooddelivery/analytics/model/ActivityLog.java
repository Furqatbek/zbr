package com.fooddelivery.analytics.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity for tracking user activity events for analytics purposes.
 * Used to calculate metrics like DAU/WAU/MAU, conversion funnels, etc.
 */
@Entity
@Table(name = "activity_logs", indexes = {
        @Index(name = "idx_activity_user_id", columnList = "user_id"),
        @Index(name = "idx_activity_event_type", columnList = "event_type"),
        @Index(name = "idx_activity_created_at", columnList = "created_at"),
        @Index(name = "idx_activity_user_event_date", columnList = "user_id, event_type, created_at"),
        @Index(name = "idx_activity_restaurant_id", columnList = "restaurant_id"),
        @Index(name = "idx_activity_order_id", columnList = "order_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User who performed the action. Nullable for anonymous events.
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * Type of activity event. See {@link ActivityEventType}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private ActivityEventType eventType;

    /**
     * Restaurant ID if the event is related to a restaurant.
     */
    @Column(name = "restaurant_id")
    private Long restaurantId;

    /**
     * Order ID if the event is related to an order.
     */
    @Column(name = "order_id")
    private Long orderId;

    /**
     * Menu item ID if the event is related to a menu item.
     */
    @Column(name = "menu_item_id")
    private Long menuItemId;

    /**
     * Session ID to track user journey within a session.
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * Device type: MOBILE, DESKTOP, TABLET.
     */
    @Column(name = "device_type", length = 20)
    private String deviceType;

    /**
     * User's IP address for geo-analytics.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent string for platform analytics.
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Referral source (e.g., "google", "direct", "referral").
     */
    @Column(name = "referral_source", length = 100)
    private String referralSource;

    /**
     * Additional metadata stored as JSON.
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Duration of the activity in milliseconds (e.g., time spent on page).
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * Timestamp when the activity occurred.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
