package com.fooddelivery.analytics.operations.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity tracking courier order events (offers, accepts, rejects, etc.).
 * Used for calculating courier acceptance rate and performance metrics.
 */
@Entity
@Table(name = "courier_order_events", indexes = {
        @Index(name = "idx_coe_courier_id", columnList = "courier_id"),
        @Index(name = "idx_coe_order_id", columnList = "order_id"),
        @Index(name = "idx_coe_event_type", columnList = "event_type"),
        @Index(name = "idx_coe_timestamp", columnList = "event_timestamp"),
        @Index(name = "idx_coe_courier_timestamp", columnList = "courier_id, event_timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierOrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "courier_id", nullable = false)
    private Long courierId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private CourierOrderEventType eventType;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "response_time_seconds")
    private Long responseTimeSeconds;

    @Column(name = "distance_to_restaurant_km")
    private Double distanceToRestaurantKm;

    @Column(name = "distance_to_customer_km")
    private Double distanceToCustomerKm;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
