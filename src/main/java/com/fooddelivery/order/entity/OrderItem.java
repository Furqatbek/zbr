package com.fooddelivery.order.entity;

import com.fooddelivery.restaurant.entity.MenuItem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order item entity representing individual items in an order.
 */
@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_items_order", columnList = "order_id"),
        @Index(name = "idx_order_items_menu_item", columnList = "menu_item_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    // Selected variant (e.g., "Large")
    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "variant_name", length = 100)
    private String variantName;

    @Column(name = "variant_price_delta", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal variantPriceDelta = BigDecimal.ZERO;

    // Selected options/modifiers stored as JSON
    // Format: [{"id": 1, "name": "Extra Cheese", "price": 1.50}, ...]
    @Column(name = "modifiers_json", columnDefinition = "TEXT")
    private String modifiersJson;

    @Column(name = "modifiers_total", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal modifiersTotal = BigDecimal.ZERO;

    // Special instructions for this item
    @Column(name = "special_instructions", length = 500)
    private String specialInstructions;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Helper methods
    public void calculateTotalPrice() {
        BigDecimal basePrice = unitPrice;

        if (variantPriceDelta != null) {
            basePrice = basePrice.add(variantPriceDelta);
        }

        if (modifiersTotal != null) {
            basePrice = basePrice.add(modifiersTotal);
        }

        this.totalPrice = basePrice.multiply(BigDecimal.valueOf(quantity));
    }
}
