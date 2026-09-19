package com.fooddelivery.restaurant.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Item variant entity for size/type variations of menu items.
 * Example: Small/Medium/Large pizza sizes
 */
@Entity
@Table(name = "item_variants", indexes = {
        @Index(name = "idx_item_variants_menu_item", columnList = "menu_item_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "price_delta", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal priceDelta = BigDecimal.ZERO;

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
    /**
     * This variant's id in the system it was imported from. An order pushed to
     * that system names the variant by this, never by ours.
     */
    @Column(name = "external_id")
    private Long externalId;

    @Column(name = "external_source", length = 50)
    private String externalSource;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Calculate the total price for this variant.
     */
    public BigDecimal calculateTotalPrice() {
        return menuItem.getEffectivePrice().add(priceDelta);
    }
}
