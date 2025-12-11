package com.fooddelivery.restaurant.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Item option entity for add-ons and modifiers.
 * Example: Extra cheese, No onions, etc.
 */
@Entity
@Table(name = "item_options", indexes = {
        @Index(name = "idx_item_options_menu_item", columnList = "menu_item_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(name = "group_name", length = 100)
    private String groupName;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "price_delta", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal priceDelta = BigDecimal.ZERO;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "max_selections")
    @Builder.Default
    private Integer maxSelections = 1;

    @Column(name = "is_required")
    @Builder.Default
    private Boolean required = false;

    @Column(name = "in_stock", nullable = false)
    @Builder.Default
    private Boolean inStock = true;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
