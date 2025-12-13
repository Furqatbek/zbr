package com.fooddelivery.restaurant.entity;

import com.fooddelivery.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Restaurant entity representing restaurants in the platform.
 */
@Entity
@Table(name = "restaurants", indexes = {
        @Index(name = "idx_restaurants_owner", columnList = "owner_id"),
        @Index(name = "idx_restaurants_status", columnList = "status"),
        @Index(name = "idx_restaurants_slug", columnList = "slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(unique = true, length = 250)
    private String slug;

    @Column(length = 1000)
    private String description;

    @Column(name = "cuisine_type", length = 100)
    private String cuisineType;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(length = 50)
    private String phone;

    @Column(length = 255)
    private String email;

    // Address fields
    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 100)
    private String country;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    // Business settings
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RestaurantStatus status = RestaurantStatus.PENDING;

    @Column(name = "is_featured")
    @Builder.Default
    private Boolean featured = false;

    @Column(name = "accepts_delivery")
    @Builder.Default
    private Boolean acceptsDelivery = true;

    @Column(name = "accepts_takeaway")
    @Builder.Default
    private Boolean acceptsTakeaway = true;

    @Column(name = "accepts_dine_in")
    @Builder.Default
    private Boolean acceptsDineIn = false;

    @Column(name = "minimum_order", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minimumOrder = BigDecimal.ZERO;

    @Column(name = "delivery_fee", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "delivery_radius_km")
    @Builder.Default
    private Integer deliveryRadiusKm = 10;

    @Column(name = "average_prep_time_minutes")
    @Builder.Default
    private Integer averagePrepTimeMinutes = 30;

    // Operating hours (simplified - could be expanded to per-day)
    @Column(name = "opens_at")
    private LocalTime opensAt;

    @Column(name = "closes_at")
    private LocalTime closesAt;

    @Column(name = "is_open")
    @Builder.Default
    private Boolean isOpen = false;

    // Platform commission
    @Column(name = "commission_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal commissionRate = new BigDecimal("15.00");

    // Currency for pricing
    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "USD";

    // Ratings
    @Column(name = "average_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "total_ratings")
    @Builder.Default
    private Integer totalRatings = 0;

    @Column(name = "total_orders")
    @Builder.Default
    private Integer totalOrders = 0;

    // Additional config stored as JSON
    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (addressLine1 != null) sb.append(addressLine1);
        if (addressLine2 != null) sb.append(", ").append(addressLine2);
        if (city != null) sb.append(", ").append(city);
        if (state != null) sb.append(", ").append(state);
        if (postalCode != null) sb.append(" ").append(postalCode);
        if (country != null) sb.append(", ").append(country);
        return sb.toString();
    }

    public boolean isCurrentlyOpen() {
        if (!Boolean.TRUE.equals(isOpen) || status != RestaurantStatus.ACTIVE) {
            return false;
        }
        if (opensAt == null || closesAt == null) {
            return true;
        }
        LocalTime now = LocalTime.now();
        return !now.isBefore(opensAt) && !now.isAfter(closesAt);
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

    public void incrementOrderCount() {
        this.totalOrders++;
    }
}
