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
 * One free delivery, owed to one person.
 *
 * <p>Granted to every customer when they register, and to a referrer when the
 * person they brought completes a first delivery. Spent automatically on the
 * next delivery order that has a fee.
 *
 * <p>It reduces what the customer pays, not what the courier is paid. The order
 * keeps its delivery fee and carries an equal discount, so the courier's
 * earnings are untouched and the platform absorbs the cost — the alternative
 * pays for the marketing out of the courier's pocket.
 */
@Entity
@Table(name = "delivery_credits", indexes = {
        @Index(name = "idx_delivery_credits_unused", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryCredit {

    public enum Reason {
        /** Every new customer, once, ever. */
        WELCOME,
        /** The referrer's side of a referral that produced a delivery. */
        REFERRAL_REWARD
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private Reason reason;

    /** Which referral earned it, for {@link Reason#REFERRAL_REWARD}. */
    @Column(name = "source_referral_id")
    private Long sourceReferralId;

    @Column(name = "order_id")
    private Long orderId;

    /** What it turned out to be worth — the fee on the order that spent it. */
    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    void onCreate() {
        if (grantedAt == null) {
            grantedAt = LocalDateTime.now();
        }
    }

    public boolean isSpendable() {
        return usedAt == null
                && (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()));
    }

    public void spendOn(Long orderId, BigDecimal deliveryFee) {
        this.orderId = orderId;
        this.amount = deliveryFee;
        this.usedAt = LocalDateTime.now();
    }
}
