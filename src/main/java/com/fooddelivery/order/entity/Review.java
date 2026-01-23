package com.fooddelivery.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a review for an order.
 */
@Entity
@Table(name = "reviews", indexes = {
        @Index(name = "idx_reviews_order_id", columnList = "order_id"),
        @Index(name = "idx_reviews_consumer_id", columnList = "consumer_id"),
        @Index(name = "idx_reviews_restaurant_id", columnList = "restaurant_id"),
        @Index(name = "idx_reviews_courier_id", columnList = "courier_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "consumer_id", nullable = false)
    private Long consumerId;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "courier_id")
    private Long courierId;

    @Column(name = "restaurant_rating")
    private Integer restaurantRating;

    @Column(name = "courier_rating")
    private Integer courierRating;

    @Column(name = "food_rating")
    private Integer foodRating;

    @Column(length = 1000)
    private String comment;

    @Column(length = 500)
    private String tags;

    @Column(name = "is_visible")
    @Builder.Default
    private Boolean isVisible = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
