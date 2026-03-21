package com.fooddelivery.compensation.entity;

import com.fooddelivery.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity tracking customer credit balance.
 */
@Entity
@Table(name = "customer_credits", indexes = {
        @Index(name = "idx_cc_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "lifetime_earned", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal lifetimeEarned = BigDecimal.ZERO;

    @Column(name = "lifetime_used", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal lifetimeUsed = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Add credit to customer balance.
     */
    public void addCredit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        this.lifetimeEarned = this.lifetimeEarned.add(amount);
    }

    /**
     * Use credit from customer balance.
     */
    public boolean useCredit(BigDecimal amount) {
        if (this.balance.compareTo(amount) >= 0) {
            this.balance = this.balance.subtract(amount);
            this.lifetimeUsed = this.lifetimeUsed.add(amount);
            return true;
        }
        return false;
    }

    /**
     * Check if customer has sufficient credit.
     */
    public boolean hasSufficientCredit(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }
}
