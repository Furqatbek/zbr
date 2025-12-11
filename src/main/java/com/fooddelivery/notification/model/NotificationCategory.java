package com.fooddelivery.notification.model;

/**
 * Categories for grouping and filtering notifications.
 */
public enum NotificationCategory {
    /**
     * Order-related notifications (status updates, assignments).
     */
    ORDER("Order Updates", "order"),

    /**
     * Finance-related notifications (payments, payouts, refunds).
     */
    FINANCE("Financial", "finance"),

    /**
     * Support-related notifications (tickets, responses).
     */
    SUPPORT("Customer Support", "support"),

    /**
     * System notifications (maintenance, announcements).
     */
    SYSTEM("System", "system"),

    /**
     * Promotional notifications (offers, discounts).
     */
    PROMOTION("Promotions", "promotion"),

    /**
     * Account-related notifications (profile, security).
     */
    ACCOUNT("Account", "account"),

    /**
     * Delivery-related notifications (courier-specific).
     */
    DELIVERY("Delivery", "delivery"),

    /**
     * Restaurant operational notifications.
     */
    RESTAURANT_OPS("Restaurant Operations", "restaurant_ops"),

    /**
     * Alert notifications (urgent/critical).
     */
    ALERT("Alerts", "alert");

    private final String displayName;
    private final String code;

    NotificationCategory(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }
}
