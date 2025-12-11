package com.fooddelivery.restaurant.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Menu item entity representing individual dishes/products.
 */
@Entity
@Table(name = "menu_items", indexes = {
        @Index(name = "idx_menu_items_category", columnList = "category_id"),
        @Index(name = "idx_menu_items_in_stock", columnList = "in_stock"),
        @Index(name = "idx_menu_items_featured", columnList = "featured")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private MenuCategory category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "price_with_margin", precision = 10, scale = 2)
    private BigDecimal priceWithMargin;

    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "in_stock", nullable = false)
    @Builder.Default
    private Boolean inStock = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean featured = false;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "prep_time_minutes")
    private Integer prepTimeMinutes;

    @Column(name = "calories")
    private Integer calories;

    @Column(name = "is_vegetarian")
    @Builder.Default
    private Boolean vegetarian = false;

    @Column(name = "is_vegan")
    @Builder.Default
    private Boolean vegan = false;

    @Column(name = "is_gluten_free")
    @Builder.Default
    private Boolean glutenFree = false;

    @Column(name = "is_spicy")
    @Builder.Default
    private Boolean spicy = false;

    @Column(name = "allergens", length = 500)
    private String allergens;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<ItemVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<ItemOption> options = new ArrayList<>();

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper method to calculate effective price
    public BigDecimal getEffectivePrice() {
        return priceWithMargin != null ? priceWithMargin : price;
    }

    // Helper method to check if on sale
    public boolean isOnSale() {
        return originalPrice != null && originalPrice.compareTo(price) > 0;
    }

    // Helper method to get discount percentage
    public int getDiscountPercentage() {
        if (!isOnSale()) return 0;
        return originalPrice.subtract(price)
                .divide(originalPrice, 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .intValue();
    }
}
