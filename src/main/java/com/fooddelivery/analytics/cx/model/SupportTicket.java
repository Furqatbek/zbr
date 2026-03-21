package com.fooddelivery.analytics.cx.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for customer support tickets.
 */
@Entity
@Table(name = "support_tickets", indexes = {
        @Index(name = "idx_ticket_user", columnList = "user_id"),
        @Index(name = "idx_ticket_restaurant", columnList = "restaurant_id"),
        @Index(name = "idx_ticket_courier", columnList = "courier_id"),
        @Index(name = "idx_ticket_type", columnList = "ticket_type"),
        @Index(name = "idx_ticket_status", columnList = "status"),
        @Index(name = "idx_ticket_created", columnList = "created_at"),
        @Index(name = "idx_ticket_priority", columnList = "priority"),
        @Index(name = "idx_ticket_assigned", columnList = "assigned_to")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", unique = true, nullable = false, length = 20)
    private String ticketNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "restaurant_id")
    private Long restaurantId;

    @Column(name = "courier_id")
    private Long courierId;

    @Column(name = "order_id")
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type", nullable = false, length = 30)
    private TicketType ticketType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "description", length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20)
    private TicketChannel channel;

    @Column(name = "assigned_to")
    private Long assignedTo; // Support agent ID

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "first_response_at")
    private LocalDateTime firstResponseAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "resolution_notes", length = 2000)
    private String resolutionNotes;

    @Column(name = "refund_amount")
    private Double refundAmount;

    @Column(name = "is_refunded")
    @Builder.Default
    private Boolean isRefunded = false;

    @Column(name = "customer_satisfaction_score")
    private Integer customerSatisfactionScore; // 1-5 CSAT score

    @Column(name = "sla_breach")
    @Builder.Default
    private Boolean slaBreach = false;

    @Column(name = "escalated")
    @Builder.Default
    private Boolean escalated = false;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "escalation_level", nullable = false)
    @Builder.Default
    private Integer escalationLevel = 1;

    @Column(name = "last_escalated_at")
    private LocalDateTime lastEscalatedAt;

    @Column(name = "sla_breached_at")
    private LocalDateTime slaBreachedAt;

    @Column(name = "reopen_count")
    @Builder.Default
    private Integer reopenCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Ticket type enum.
     */
    public enum TicketType {
        LATE_DELIVERY,
        MISSING_ITEMS,
        WRONG_ORDER,
        COLD_FOOD,
        REFUND_REQUEST,
        PAYMENT_ISSUE,
        COURIER_BEHAVIOR,
        RESTAURANT_QUALITY,
        APP_ISSUE,
        ACCOUNT_ISSUE,
        PROMO_CODE_ISSUE,
        OTHER
    }

    /**
     * Ticket status enum.
     */
    public enum TicketStatus {
        OPEN,
        IN_PROGRESS,
        WAITING_CUSTOMER,
        WAITING_RESTAURANT,
        WAITING_COURIER,
        RESOLVED,
        CLOSED,
        REOPENED
    }

    /**
     * Ticket priority enum.
     */
    public enum TicketPriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }

    /**
     * Ticket channel enum.
     */
    public enum TicketChannel {
        IN_APP,
        EMAIL,
        PHONE,
        CHAT,
        SOCIAL_MEDIA,
        WEB
    }

    /**
     * Calculate resolution time in hours.
     */
    public Long getResolutionTimeHours() {
        if (resolvedAt == null || createdAt == null) {
            return null;
        }
        return java.time.Duration.between(createdAt, resolvedAt).toHours();
    }

    /**
     * Calculate first response time in minutes.
     */
    public Long getFirstResponseTimeMinutes() {
        if (firstResponseAt == null || createdAt == null) {
            return null;
        }
        return java.time.Duration.between(createdAt, firstResponseAt).toMinutes();
    }

    /**
     * Check if ticket is open (not resolved or closed).
     */
    public boolean isOpen() {
        return status != TicketStatus.RESOLVED && status != TicketStatus.CLOSED;
    }
}
