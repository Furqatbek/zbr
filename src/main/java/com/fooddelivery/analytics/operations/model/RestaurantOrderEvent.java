package com.fooddelivery.analytics.operations.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity tracking restaurant order events.
 * Used for calculating restaurant acceptance time and preparation metrics.
 */
@Entity
@Table(name = "restaurant_order_events", indexes = {
        @Index(name = "idx_roe_restaurant_id", columnList = "restaurant_id"),
        @Index(name = "idx_roe_order_id", columnList = "order_id"),
        @Index(name = "idx_roe_event_type", columnList = "event_type"),
        @Index(name = "idx_roe_timestamp", columnList = "event_timestamp"),
        @Index(name = "idx_roe_restaurant_timestamp", columnList = "restaurant_id, event_timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantOrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private RestaurantOrderEventType eventType;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "estimated_prep_minutes")
    private Integer estimatedPrepMinutes;

    @Column(name = "actual_prep_minutes")
    private Integer actualPrepMinutes;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
