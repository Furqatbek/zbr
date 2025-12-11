package com.fooddelivery.analytics.financial.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity tracking payout disputes from restaurants or couriers.
 */
@Entity
@Table(name = "payout_disputes", indexes = {
        @Index(name = "idx_pd_entity_id", columnList = "entity_id"),
        @Index(name = "idx_pd_entity_type", columnList = "entity_type"),
        @Index(name = "idx_pd_status", columnList = "status"),
        @Index(name = "idx_pd_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutDispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Entity ID (restaurant_id or courier_id).
     */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /**
     * Entity type: RESTAURANT or COURIER.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private DisputeEntityType entityType;

    /**
     * Related payout ID (if disputing a specific payout).
     */
    @Column(name = "payout_id")
    private Long payoutId;

    /**
     * Related order ID (if disputing a specific order).
     */
    @Column(name = "order_id")
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispute_type", nullable = false, length = 30)
    private DisputeType disputeType;

    /**
     * Amount in dispute.
     */
    @Column(name = "disputed_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal disputedAmount;

    /**
     * Description of the dispute.
     */
    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DisputeStatus status = DisputeStatus.OPEN;

    /**
     * Resolution outcome.
     */
    @Column(name = "resolution", length = 500)
    private String resolution;

    /**
     * Amount resolved/adjusted.
     */
    @Column(name = "resolved_amount", precision = 10, scale = 2)
    private BigDecimal resolvedAmount;

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
