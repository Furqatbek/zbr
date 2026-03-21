package com.fooddelivery.compensation.entity;

import com.fooddelivery.auth.entity.User;
import com.fooddelivery.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Audit log for all compensations issued.
 */
@Entity
@Table(name = "compensation_logs", indexes = {
        @Index(name = "idx_cl_user_id", columnList = "user_id"),
        @Index(name = "idx_cl_order_id", columnList = "order_id"),
        @Index(name = "idx_cl_status", columnList = "status"),
        @Index(name = "idx_cl_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompensationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 50)
    private CompensationTriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "compensation_type", nullable = false, length = 30)
    private CompensationType compensationType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    private CompensationRule rule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CompensationStatus status = CompensationStatus.PENDING;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Mark compensation as completed.
     */
    public void markCompleted() {
        this.status = CompensationStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Mark compensation as failed.
     */
    public void markFailed() {
        this.status = CompensationStatus.FAILED;
        this.processedAt = LocalDateTime.now();
    }
}
