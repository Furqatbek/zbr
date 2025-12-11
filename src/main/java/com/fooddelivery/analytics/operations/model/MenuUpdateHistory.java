package com.fooddelivery.analytics.operations.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity tracking menu update history for restaurants.
 * Used for calculating menu update frequency metrics.
 */
@Entity
@Table(name = "menu_update_history", indexes = {
        @Index(name = "idx_muh_restaurant_id", columnList = "restaurant_id"),
        @Index(name = "idx_muh_updated_at", columnList = "updated_at"),
        @Index(name = "idx_muh_restaurant_updated", columnList = "restaurant_id, updated_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuUpdateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "update_type", nullable = false, length = 30)
    private MenuUpdateType updateType;

    @Column(name = "items_added")
    @Builder.Default
    private Integer itemsAdded = 0;

    @Column(name = "items_removed")
    @Builder.Default
    private Integer itemsRemoved = 0;

    @Column(name = "items_modified")
    @Builder.Default
    private Integer itemsModified = 0;

    @Column(name = "categories_changed")
    @Builder.Default
    private Integer categoriesChanged = 0;

    @Column(name = "prices_updated")
    @Builder.Default
    private Integer pricesUpdated = 0;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
