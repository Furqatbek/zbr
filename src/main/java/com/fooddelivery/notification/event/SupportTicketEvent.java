package com.fooddelivery.notification.event;

import com.fooddelivery.common.event.DomainEvent;
import lombok.Getter;

/**
 * Event published when a support ticket status changes.
 */
@Getter
public class SupportTicketEvent extends DomainEvent {

    public enum TicketEventType {
        CREATED,
        UPDATED,
        ASSIGNED,
        RESOLVED,
        CLOSED,
        REOPENED
    }

    private final Long ticketId;
    private final Long userId;
    private final String subject;
    private final TicketEventType ticketEventType;
    private final String message;
    private final Long assigneeId;

    public SupportTicketEvent(Long ticketId, Long userId, String subject,
                               TicketEventType ticketEventType, String message) {
        super();
        this.ticketId = ticketId;
        this.userId = userId;
        this.subject = subject;
        this.ticketEventType = ticketEventType;
        this.message = message;
        this.assigneeId = null;
    }

    public SupportTicketEvent(Long ticketId, Long userId, String subject,
                               TicketEventType ticketEventType, String message, Long assigneeId) {
        super();
        this.ticketId = ticketId;
        this.userId = userId;
        this.subject = subject;
        this.ticketEventType = ticketEventType;
        this.message = message;
        this.assigneeId = assigneeId;
    }

    @Override
    public String getAggregateId() {
        return ticketId.toString();
    }

    @Override
    public String getAggregateType() {
        return "SupportTicket";
    }
}
