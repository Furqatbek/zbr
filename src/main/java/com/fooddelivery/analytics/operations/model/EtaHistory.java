package com.fooddelivery.analytics.operations.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity tracking ETA predictions vs actual delivery times.
 * Used for calculating ETA accuracy metrics.
 */
@Entity
@Table(name = "eta_history", indexes = {
        @Index(name = "idx_eta_order_id", columnList = "order_id"),
        @Index(name = "idx_eta_restaurant_id", columnList = "restaurant_id"),
        @Index(name = "idx_eta_courier_id", columnList = "courier_id"),
        @Index(name = "idx_eta_predicted_at", columnList = "predicted_at"),
        @Index(name = "idx_eta_delivered_at", columnList = "actual_delivered_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtaHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "courier_id")
    private Long courierId;

    /**
     * The ETA shown to the user at order time (in minutes from order placed).
     */
    @Column(name = "eta_shown_minutes", nullable = false)
    private Integer etaShownMinutes;

    /**
     * The predicted delivery time shown to the user.
     */
    @Column(name = "predicted_delivery_time", nullable = false)
    private LocalDateTime predictedDeliveryTime;

    /**
     * When the ETA was predicted/shown to user.
     */
    @Column(name = "predicted_at", nullable = false)
    private LocalDateTime predictedAt;

    /**
     * Actual delivery time.
     */
    @Column(name = "actual_delivered_at")
    private LocalDateTime actualDeliveredAt;

    /**
     * Actual delivery duration in minutes (from order placed).
     */
    @Column(name = "actual_delivery_minutes")
    private Integer actualDeliveryMinutes;

    /**
     * ETA error in minutes (positive = late, negative = early).
     */
    @Column(name = "eta_error_minutes")
    private Integer etaErrorMinutes;

    /**
     * Absolute ETA error in minutes.
     */
    @Column(name = "eta_error_abs_minutes")
    private Integer etaErrorAbsMinutes;

    /**
     * Whether the delivery was late (actual > predicted).
     */
    @Column(name = "was_late")
    private Boolean wasLate;

    /**
     * Whether the order was completed (not cancelled).
     */
    @Column(name = "was_completed")
    @Builder.Default
    private Boolean wasCompleted = false;

    /**
     * Breakdown: ETA for restaurant preparation.
     */
    @Column(name = "eta_prep_minutes")
    private Integer etaPrepMinutes;

    /**
     * Breakdown: Actual preparation time.
     */
    @Column(name = "actual_prep_minutes")
    private Integer actualPrepMinutes;

    /**
     * Breakdown: ETA for courier pickup.
     */
    @Column(name = "eta_pickup_minutes")
    private Integer etaPickupMinutes;

    /**
     * Breakdown: Actual pickup time.
     */
    @Column(name = "actual_pickup_minutes")
    private Integer actualPickupMinutes;

    /**
     * Breakdown: ETA for delivery.
     */
    @Column(name = "eta_delivery_minutes")
    private Integer etaDeliveryMinutes;

    /**
     * Breakdown: Actual delivery time.
     */
    @Column(name = "actual_travel_minutes")
    private Integer actualTravelMinutes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Calculate and set ETA error after delivery is completed.
     */
    public void calculateEtaError() {
        if (actualDeliveryMinutes != null && etaShownMinutes != null) {
            this.etaErrorMinutes = actualDeliveryMinutes - etaShownMinutes;
            this.etaErrorAbsMinutes = Math.abs(this.etaErrorMinutes);
            this.wasLate = this.etaErrorMinutes > 0;
        }
    }
}
