package com.fooddelivery.analytics.operations.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity tracking courier availability (online/offline/busy) status changes.
 * Used for calculating courier availability metrics.
 */
@Entity
@Table(name = "courier_availability_events", indexes = {
        @Index(name = "idx_cae_courier_id", columnList = "courier_id"),
        @Index(name = "idx_cae_status_changed_at", columnList = "status_changed_at"),
        @Index(name = "idx_cae_status", columnList = "status"),
        @Index(name = "idx_cae_courier_changed", columnList = "courier_id, status_changed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierAvailabilityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "courier_id", nullable = false)
    private Long courierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CourierAvailabilityStatus status;

    @Column(name = "previous_status", length = 20)
    private String previousStatus;

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
