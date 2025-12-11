package com.fooddelivery.notification.model;

/**
 * Priority levels for notifications.
 */
public enum NotificationPriority {
    /**
     * Low priority - informational, can be batched.
     */
    LOW(1, "Low", false),

    /**
     * Normal priority - standard notifications.
     */
    NORMAL(2, "Normal", false),

    /**
     * High priority - important, should be shown prominently.
     */
    HIGH(3, "High", true),

    /**
     * Urgent priority - critical, requires immediate attention.
     */
    URGENT(4, "Urgent", true);

    private final int weight;
    private final String displayName;
    private final boolean requiresImmediateDisplay;

    NotificationPriority(int weight, String displayName, boolean requiresImmediateDisplay) {
        this.weight = weight;
        this.displayName = displayName;
        this.requiresImmediateDisplay = requiresImmediateDisplay;
    }

    public int getWeight() {
        return weight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean requiresImmediateDisplay() {
        return requiresImmediateDisplay;
    }

    /**
     * Check if this priority is higher than another.
     */
    public boolean isHigherThan(NotificationPriority other) {
        return this.weight > other.weight;
    }
}
