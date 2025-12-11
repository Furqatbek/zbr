package com.fooddelivery.analytics.financial.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity tracking promotion/discount usage on orders.
 */
@Entity
@Table(name = "promotion_usages", indexes = {
        @Index(name = "idx_pu_order_id", columnList = "order_id"),
        @Index(name = "idx_pu_promotion_id", columnList = "promotion_id"),
        @Index(name = "idx_pu_user_id", columnList = "user_id"),
        @Index(name = "idx_pu_used_at", columnList = "used_at"),
        @Index(name = "idx_pu_promo_type", columnList = "promotion_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "promotion_id")
    private Long promotionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "promo_code", length = 50)
    private String promoCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_type", nullable = false, length = 30)
    private PromotionType promotionType;

    /**
     * Original order value before discount.
     */
    @Column(name = "original_order_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal originalOrderValue;

    /**
     * Discount amount applied.
     */
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    /**
     * Who funded this promotion: PLATFORM, RESTAURANT, SHARED.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "funded_by", nullable = false, length = 20)
    @Builder.Default
    private PromotionFunder fundedBy = PromotionFunder.PLATFORM;

    /**
     * Platform's share of the discount (if SHARED).
     */
    @Column(name = "platform_cost", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal platformCost = BigDecimal.ZERO;

    /**
     * Restaurant's share of the discount (if SHARED).
     */
    @Column(name = "restaurant_cost", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal restaurantCost = BigDecimal.ZERO;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
