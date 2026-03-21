package com.fooddelivery.escalation.entity;

/**
 * Escalation levels for support tickets.
 */
public enum EscalationLevel {
    /**
     * L0: Automated handling (chatbot, self-service).
     */
    L0_AUTOMATED(0, "Automated"),

    /**
     * L1: Support Agent (first line support).
     */
    L1_SUPPORT(1, "Support Agent"),

    /**
     * L2: Senior Support / Team Lead.
     */
    L2_SENIOR(2, "Senior Support"),

    /**
     * L3: Operations Manager.
     */
    L3_OPERATIONS(3, "Operations Manager"),

    /**
     * L4: Executive (VP/Director).
     */
    L4_EXECUTIVE(4, "Executive");

    private final int level;
    private final String displayName;

    EscalationLevel(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static EscalationLevel fromLevel(int level) {
        for (EscalationLevel el : values()) {
            if (el.level == level) {
                return el;
            }
        }
        return L1_SUPPORT;
    }

    public boolean canEscalateTo(EscalationLevel target) {
        return target.level > this.level;
    }
}
