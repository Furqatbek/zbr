package com.fooddelivery.courier.reassignment.entity;

import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Audit log for order reassignments.
 */
@Entity
@Table(name = "reassignment_logs", indexes = {
        @Index(name = "idx_rl_order_id", columnList = "order_id"),
        @Index(name = "idx_rl_previous_courier_id", columnList = "previous_courier_id"),
        @Index(name = "idx_rl_new_courier_id", columnList = "new_courier_id"),
        @Index(name = "idx_rl_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReassignmentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_courier_id", nullable = false)
    private Courier previousCourier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_courier_id")
    private Courier newCourier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReassignmentReason reason;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "initiated_by", nullable = false, length = 50)
    private ReassignmentInitiator initiatedBy;

    @Column(name = "initiated_by_user_id")
    private Long initiatedByUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
