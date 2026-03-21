package com.fooddelivery.order.entity;

import com.fooddelivery.courier.entity.Courier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity tracking delivery issues reported by couriers.
 */
@Entity
@Table(name = "delivery_issues", indexes = {
        @Index(name = "idx_di_order_id", columnList = "order_id"),
        @Index(name = "idx_di_courier_id", columnList = "courier_id"),
        @Index(name = "idx_di_issue_type", columnList = "issue_type"),
        @Index(name = "idx_di_resolved", columnList = "resolved"),
        @Index(name = "idx_di_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id", nullable = false)
    private Courier courier;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false, length = 30)
    private DeliveryIssueType issueType;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "resolved", nullable = false)
    @Builder.Default
    private Boolean resolved = false;

    @Column(name = "resolution", length = 500)
    private String resolution;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
