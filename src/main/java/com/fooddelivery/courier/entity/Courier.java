package com.fooddelivery.courier.entity;

import com.fooddelivery.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Courier entity representing delivery couriers.
 */
@Entity
@Table(name = "couriers", indexes = {
        @Index(name = "idx_couriers_user", columnList = "user_id"),
        @Index(name = "idx_couriers_status", columnList = "status"),
        @Index(name = "idx_couriers_location", columnList = "current_lat, current_lng")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Courier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CourierStatus status = CourierStatus.OFFLINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    @Builder.Default
    private VehicleType vehicleType = VehicleType.BICYCLE;

    @Column(name = "vehicle_number", length = 50)
    private String vehicleNumber;

    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    // Current location
    @Column(name = "current_lat", precision = 10, scale = 7)
    private BigDecimal currentLat;

    @Column(name = "current_lng", precision = 10, scale = 7)
    private BigDecimal currentLng;

    @Column(name = "location_updated_at")
    private LocalDateTime locationUpdatedAt;

    // Performance metrics
    @Column(name = "total_deliveries")
    @Builder.Default
    private Integer totalDeliveries = 0;

    @Column(name = "average_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "total_ratings")
    @Builder.Default
    private Integer totalRatings = 0;

    @Column(name = "acceptance_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal acceptanceRate = new BigDecimal("100.00");

    // Earnings
    @Column(name = "total_earnings", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    // Settings
    @Column(name = "max_concurrent_orders")
    @Builder.Default
    private Integer maxConcurrentOrders = 3;

    @Column(name = "current_order_count")
    @Builder.Default
    private Integer currentOrderCount = 0;

    @Column(name = "preferred_radius_km")
    @Builder.Default
    private Integer preferredRadiusKm = 5;

    // Verification
    @Column(name = "is_verified")
    @Builder.Default
    private Boolean verified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "documents_submitted")
    @Builder.Default
    private Boolean documentsSubmitted = false;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isAvailable() {
        return status == CourierStatus.AVAILABLE &&
                verified &&
                currentOrderCount < maxConcurrentOrders;
    }

    public void updateLocation(BigDecimal lat, BigDecimal lng) {
        this.currentLat = lat;
        this.currentLng = lng;
        this.locationUpdatedAt = LocalDateTime.now();
    }

    public void incrementDeliveryCount() {
        this.totalDeliveries++;
        this.currentOrderCount--;
    }

    public void updateRating(BigDecimal newRating) {
        if (totalRatings == 0) {
            this.averageRating = newRating;
        } else {
            BigDecimal total = averageRating.multiply(BigDecimal.valueOf(totalRatings)).add(newRating);
            this.averageRating = total.divide(BigDecimal.valueOf(totalRatings + 1), 2, java.math.RoundingMode.HALF_UP);
        }
        this.totalRatings++;
    }

    public void addEarnings(BigDecimal amount) {
        this.totalEarnings = this.totalEarnings.add(amount);
    }

    public void assignOrder() {
        this.currentOrderCount++;
        if (currentOrderCount >= maxConcurrentOrders) {
            this.status = CourierStatus.BUSY;
        }
    }

    public void completeOrder() {
        this.currentOrderCount = Math.max(0, this.currentOrderCount - 1);
        this.totalDeliveries++;
        if (status == CourierStatus.BUSY && currentOrderCount < maxConcurrentOrders) {
            this.status = CourierStatus.AVAILABLE;
        }
    }
}
