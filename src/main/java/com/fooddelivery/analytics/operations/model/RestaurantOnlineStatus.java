package com.fooddelivery.analytics.operations.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity tracking restaurant online/offline status changes.
 * Used for calculating restaurant offline time metrics.
 */
@Entity
@Table(name = "restaurant_online_status", indexes = {
        @Index(name = "idx_ros_restaurant_id", columnList = "restaurant_id"),
        @Index(name = "idx_ros_status_changed_at", columnList = "status_changed_at"),
        @Index(name = "idx_ros_restaurant_changed", columnList = "restaurant_id, status_changed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantOnlineStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "is_online", nullable = false)
    private Boolean isOnline;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "status_changed_at", nullable = false)
    private LocalDateTime statusChangedAt;

    @Column(name = "previous_status_duration_minutes")
    private Long previousStatusDurationMinutes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
