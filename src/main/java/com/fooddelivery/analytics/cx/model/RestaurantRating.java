package com.fooddelivery.analytics.cx.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for restaurant ratings from customers.
 */
@Entity
@Table(name = "rating_restaurant", indexes = {
        @Index(name = "idx_rating_restaurant_restaurant", columnList = "restaurant_id"),
        @Index(name = "idx_rating_restaurant_user", columnList = "user_id"),
        @Index(name = "idx_rating_restaurant_created", columnList = "created_at"),
        @Index(name = "idx_rating_restaurant_score", columnList = "score")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "score", nullable = false)
    private Integer score; // 1-5

    @Column(name = "food_quality_score")
    private Integer foodQualityScore; // 1-5

    @Column(name = "portion_size_score")
    private Integer portionSizeScore; // 1-5

    @Column(name = "value_for_money_score")
    private Integer valueForMoneyScore; // 1-5

    @Column(name = "comment", length = 2000)
    private String comment;

    @Column(name = "is_verified_purchase")
    @Builder.Default
    private Boolean isVerifiedPurchase = true;

    @Column(name = "is_anonymous")
    @Builder.Default
    private Boolean isAnonymous = false;

    @Column(name = "helpful_count")
    @Builder.Default
    private Integer helpfulCount = 0;

    @Column(name = "restaurant_response", length = 1000)
    private String restaurantResponse;

    @Column(name = "restaurant_responded_at")
    private LocalDateTime restaurantRespondedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
