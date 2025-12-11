package com.fooddelivery.analytics.cx.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for courier ratings from customers.
 */
@Entity
@Table(name = "rating_courier", indexes = {
        @Index(name = "idx_rating_courier_courier", columnList = "courier_id"),
        @Index(name = "idx_rating_courier_user", columnList = "user_id"),
        @Index(name = "idx_rating_courier_created", columnList = "created_at"),
        @Index(name = "idx_rating_courier_score", columnList = "score")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "courier_id", nullable = false)
    private Long courierId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "delivery_id")
    private Long deliveryId;

    @Column(name = "score", nullable = false)
    private Integer score; // 1-5

    @Column(name = "professionalism_score")
    private Integer professionalismScore; // 1-5

    @Column(name = "communication_score")
    private Integer communicationScore; // 1-5

    @Column(name = "timeliness_score")
    private Integer timelinessScore; // 1-5

    @Column(name = "comment", length = 2000)
    private String comment;

    @Column(name = "tip_amount")
    private Double tipAmount;

    @Column(name = "is_verified_delivery")
    @Builder.Default
    private Boolean isVerifiedDelivery = true;

    @Column(name = "is_anonymous")
    @Builder.Default
    private Boolean isAnonymous = false;

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
