package com.fooddelivery.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events in the system.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class DomainEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private String eventType;
    private LocalDateTime occurredAt;
    private int version;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = this.getClass().getSimpleName();
        this.occurredAt = LocalDateTime.now();
        this.version = 1;
    }

    protected DomainEvent(int version) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = this.getClass().getSimpleName();
        this.occurredAt = LocalDateTime.now();
        this.version = version;
    }

    protected DomainEvent(String eventId, String eventType, LocalDateTime occurredAt, int version) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.version = version;
    }

    /**
     * Get the aggregate ID associated with this event.
     */
    public abstract String getAggregateId();

    /**
     * Get the aggregate type (e.g., "Order", "Restaurant").
     */
    public abstract String getAggregateType();

    @Override
    public String toString() {
        return String.format("%s[id=%s, aggregate=%s:%s, occurredAt=%s]",
                eventType, eventId, getAggregateType(), getAggregateId(), occurredAt);
    }
}
