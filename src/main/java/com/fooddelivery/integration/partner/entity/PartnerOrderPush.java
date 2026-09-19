package com.fooddelivery.integration.partner.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * The record of one order's journey to a partner's till.
 *
 * <p>Exists so a failed push is visible rather than silent. An order that never
 * reached the kitchen looks exactly like one that did, right up until the
 * customer asks where their food is — this row is the difference.
 */
@Entity
@Table(name = "partner_order_pushes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerOrderPush {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(name = "external_order_no", nullable = false, length = 50)
    private String externalOrderNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PartnerPushStatus status = PartnerPushStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "partner_order_id", length = 100)
    private String partnerOrderId;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    /**
     * Set when the partner refused our cancellation because their kitchen had
     * already started: the food was made and the venue is owed for it.
     *
     * <p>Not an error and not a retry. It is the one durable record that this
     * ticket has a cost somebody carries, written before the commercial
     * question is settled so the answer can be applied backwards when it
     * arrives.
     */
    @Column(name = "venue_owed_at")
    private LocalDateTime venueOwedAt;

    @Column(name = "venue_owed_reason", length = 1000)
    private String venueOwedReason;
}
