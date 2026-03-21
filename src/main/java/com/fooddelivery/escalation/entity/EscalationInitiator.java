package com.fooddelivery.escalation.entity;

/**
 * Who initiated the escalation.
 */
public enum EscalationInitiator {
    /**
     * System auto-escalation.
     */
    SYSTEM,

    /**
     * Support agent escalated.
     */
    AGENT,

    /**
     * Customer requested escalation.
     */
    CUSTOMER,

    /**
     * Manager/admin escalated.
     */
    ADMIN
}
