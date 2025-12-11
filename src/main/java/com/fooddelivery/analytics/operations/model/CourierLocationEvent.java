package com.fooddelivery.analytics.operations.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity tracking courier location updates for analytics.
 * Used for calculating location update frequency and courier availability.
 */
@Entity
@Table(name = "courier_location_events", indexes = {
        @Index(name = "idx_cle_courier_id", columnList = "courier_id"),
        @Index(name = "idx_cle_recorded_at", columnList = "recorded_at"),
        @Index(name = "idx_cle_courier_recorded", columnList = "courier_id, recorded_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierLocationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "courier_id", nullable = false)
    private Long courierId;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "accuracy_meters")
    private Double accuracyMeters;

    @Column(name = "speed_kmh")
    private Double speedKmh;

    @Column(name = "heading")
    private Double heading;

    @Column(name = "altitude_meters")
    private Double altitudeMeters;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "is_moving")
    @Builder.Default
    private Boolean isMoving = false;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;
}
