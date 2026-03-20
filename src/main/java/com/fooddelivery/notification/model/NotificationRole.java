package com.fooddelivery.notification.model;

/**
 * Enumeration of roles that can receive notifications.
 * Note: CONSUMER and CUSTOMER are aliases - CONSUMER is preferred as it matches the auth system.
 */
public enum NotificationRole {
    /**
     * Consumer role - receives order updates, promotions.
     * This matches the Role.CONSUMER from the auth system.
     */
    CONSUMER("Consumer"),

    /**
     * @deprecated Use CONSUMER instead. Kept for backward compatibility.
     */
    @Deprecated
    CUSTOMER("Customer"),

    /**
     * Courier role - receives delivery assignments, updates.
     */
    COURIER("Courier"),

    /**
     * Restaurant role - receives order notifications.
     */
    RESTAURANT("Restaurant"),

    /**
     * Admin role - receives system and operational alerts.
     */
    ADMIN("Admin"),

    /**
     * Support agent role - receives support ticket updates.
     */
    SUPPORT("Support Agent"),

    /**
     * Finance role - receives payment and payout notifications.
     */
    FINANCE("Finance"),

    /**
     * Operations manager role - receives operational alerts.
     */
    OPERATIONS("Operations Manager"),

    /**
     * All roles - for system-wide broadcasts.
     */
    ALL("All Users");

    private final String displayName;

    NotificationRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
