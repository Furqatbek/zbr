package com.fooddelivery.escalation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Audit log for ticket escalations.
 */
@Entity
@Table(name = "escalation_logs", indexes = {
        @Index(name = "idx_el_ticket_id", columnList = "ticket_id"),
        @Index(name = "idx_el_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscalationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "previous_level", nullable = false)
    private Integer previousLevel;

    @Column(name = "new_level", nullable = false)
    private Integer newLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    private EscalationRule rule;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "escalated_by", nullable = false, length = 50)
    private EscalationInitiator escalatedBy;

    @Column(name = "escalated_by_user_id")
    private Long escalatedByUserId;

    @Column(length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public EscalationLevel getPreviousEscalationLevel() {
        return EscalationLevel.fromLevel(previousLevel);
    }

    public EscalationLevel getNewEscalationLevel() {
        return EscalationLevel.fromLevel(newLevel);
    }
}
