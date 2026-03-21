package com.fooddelivery.escalation.entity;

/**
 * Types of events that can trigger escalation.
 */
public enum EscalationTriggerType {
    /**
     * SLA breach detected.
     */
    SLA_BREACH,

    /**
     * High value refund requested.
     */
    REFUND_AMOUNT,

    /**
     * VIP customer ticket.
     */
    CUSTOMER_VIP,

    /**
     * Repeat complaints from same customer.
     */
    REPEAT_COMPLAINT,

    /**
     * Legal threat keywords detected.
     */
    LEGAL_THREAT,

    /**
     * Critical priority ticket.
     */
    CRITICAL_PRIORITY,

    /**
     * Ticket open for extended time.
     */
    TIME_OPEN,

    /**
     * Manual escalation by agent.
     */
    MANUAL,

    /**
     * Customer requested escalation.
     */
    CUSTOMER_REQUEST
}
