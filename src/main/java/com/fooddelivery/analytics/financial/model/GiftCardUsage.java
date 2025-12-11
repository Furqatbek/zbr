package com.fooddelivery.analytics.financial.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity tracking gift card redemptions.
 */
@Entity
@Table(name = "gift_card_usages", indexes = {
        @Index(name = "idx_gcu_order_id", columnList = "order_id"),
        @Index(name = "idx_gcu_gift_card_id", columnList = "gift_card_id"),
        @Index(name = "idx_gcu_user_id", columnList = "user_id"),
        @Index(name = "idx_gcu_used_at", columnList = "used_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GiftCardUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gift_card_id", nullable = false)
    private Long giftCardId;

    @Column(name = "gift_card_code", nullable = false, length = 50)
    private String giftCardCode;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Amount redeemed from the gift card.
     */
    @Column(name = "redeemed_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal redeemedAmount;

    /**
     * Gift card balance before redemption.
     */
    @Column(name = "balance_before", nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceBefore;

    /**
     * Gift card balance after redemption.
     */
    @Column(name = "balance_after", nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
