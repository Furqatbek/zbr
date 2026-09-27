package com.fooddelivery.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One customer's use of one promo code on one order.
 *
 * <p>{@code promo_codes.user_usage_limit} existed from the start and could
 * never be enforced, because {@code usage_count} is a single global number with
 * no record of who spent it. A code meant to be used once per customer could be
 * used by the same customer every day.
 *
 * <p>It also answers the question a campaign actually asks — which code, and
 * therefore which restaurant's QR poster, brought this order.
 */
@Entity
@Table(name = "promo_code_usages", indexes = {
        @Index(name = "idx_promo_usages_code_user", columnList = "promo_code_id, user_id"),
        @Index(name = "idx_promo_usages_order", columnList = "order_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCodeUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "promo_code_id", nullable = false)
    private Long promoCodeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** What this use was worth, kept even if the code's terms change later. */
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;

    @PrePersist
    void onCreate() {
        if (usedAt == null) {
            usedAt = LocalDateTime.now();
        }
    }
}
