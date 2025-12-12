package com.fooddelivery.notification.model;

/**
 * Types of notifications corresponding to business events.
 */
public enum NotificationType {
    // ===== Order Lifecycle Events =====
    ORDER_CREATED("Order Created", NotificationCategory.ORDER, NotificationPriority.NORMAL),
    ORDER_CONFIRMED("Order Confirmed", NotificationCategory.ORDER, NotificationPriority.NORMAL),
    ORDER_ACCEPTED("Order Accepted by Restaurant", NotificationCategory.ORDER, NotificationPriority.NORMAL),
    ORDER_REJECTED("Order Rejected", NotificationCategory.ORDER, NotificationPriority.HIGH),
    ORDER_PREPARING("Order Being Prepared", NotificationCategory.ORDER, NotificationPriority.NORMAL),
    ORDER_READY("Order Ready for Pickup", NotificationCategory.ORDER, NotificationPriority.HIGH),
    ORDER_PICKED_UP("Order Picked Up", NotificationCategory.ORDER, NotificationPriority.NORMAL),
    ORDER_IN_TRANSIT("Order In Transit", NotificationCategory.ORDER, NotificationPriority.NORMAL),
    ORDER_ARRIVING("Order Arriving Soon", NotificationCategory.ORDER, NotificationPriority.NORMAL),
    ORDER_DELIVERED("Order Delivered", NotificationCategory.ORDER, NotificationPriority.NORMAL),
    ORDER_COMPLETED("Order Completed", NotificationCategory.ORDER, NotificationPriority.NORMAL),
    ORDER_CANCELLED("Order Cancelled", NotificationCategory.ORDER, NotificationPriority.HIGH),
    ORDER_DELAYED("Order Delayed", NotificationCategory.ORDER, NotificationPriority.HIGH),
    ORDER_MODIFIED("Order Modified", NotificationCategory.ORDER, NotificationPriority.NORMAL),

    // ===== Courier Events =====
    COURIER_ASSIGNED("Courier Assigned", NotificationCategory.DELIVERY, NotificationPriority.HIGH),
    COURIER_REASSIGNED("Courier Reassigned", NotificationCategory.DELIVERY, NotificationPriority.HIGH),
    COURIER_APPROACHING_RESTAURANT("Courier Approaching Restaurant", NotificationCategory.DELIVERY, NotificationPriority.NORMAL),
    COURIER_ARRIVED_RESTAURANT("Courier Arrived at Restaurant", NotificationCategory.DELIVERY, NotificationPriority.NORMAL),
    COURIER_APPROACHING_CUSTOMER("Courier Approaching", NotificationCategory.DELIVERY, NotificationPriority.NORMAL),
    NEW_DELIVERY_AVAILABLE("New Delivery Available", NotificationCategory.DELIVERY, NotificationPriority.HIGH),
    DELIVERY_COMPLETED("Delivery Completed", NotificationCategory.DELIVERY, NotificationPriority.NORMAL),

    // ===== Payment Events =====
    PAYMENT_RECEIVED("Payment Received", NotificationCategory.FINANCE, NotificationPriority.NORMAL),
    PAYMENT_FAILED("Payment Failed", NotificationCategory.FINANCE, NotificationPriority.URGENT),
    PAYMENT_REFUNDED("Payment Refunded", NotificationCategory.FINANCE, NotificationPriority.HIGH),
    REFUND_PROCESSED("Refund Processed", NotificationCategory.FINANCE, NotificationPriority.HIGH),
    PAYMENT_PENDING("Payment Pending", NotificationCategory.FINANCE, NotificationPriority.NORMAL),

    // ===== Payout Events =====
    PAYOUT_INITIATED("Payout Initiated", NotificationCategory.FINANCE, NotificationPriority.NORMAL),
    PAYOUT_COMPLETED("Payout Completed", NotificationCategory.FINANCE, NotificationPriority.NORMAL),
    PAYOUT_FAILED("Payout Failed", NotificationCategory.FINANCE, NotificationPriority.URGENT),
    WEEKLY_EARNINGS_SUMMARY("Weekly Earnings Summary", NotificationCategory.FINANCE, NotificationPriority.LOW),

    // ===== Support Events =====
    SUPPORT_TICKET_CREATED("Support Ticket Created", NotificationCategory.SUPPORT, NotificationPriority.NORMAL),
    SUPPORT_TICKET_UPDATED("Support Ticket Updated", NotificationCategory.SUPPORT, NotificationPriority.NORMAL),
    SUPPORT_TICKET_RESOLVED("Support Ticket Resolved", NotificationCategory.SUPPORT, NotificationPriority.NORMAL),
    SUPPORT_TICKET_ESCALATED("Support Ticket Escalated", NotificationCategory.SUPPORT, NotificationPriority.HIGH),
    SUPPORT_RESPONSE_RECEIVED("Support Response Received", NotificationCategory.SUPPORT, NotificationPriority.NORMAL),
    REFUND_REQUEST_RECEIVED("Refund Request Received", NotificationCategory.SUPPORT, NotificationPriority.HIGH),
    REFUND_APPROVED("Refund Approved", NotificationCategory.SUPPORT, NotificationPriority.NORMAL),
    REFUND_REJECTED("Refund Rejected", NotificationCategory.SUPPORT, NotificationPriority.HIGH),

    // ===== Restaurant Events =====
    NEW_ORDER_RECEIVED("New Order Received", NotificationCategory.RESTAURANT_OPS, NotificationPriority.URGENT),
    ORDER_REMINDER("Order Reminder", NotificationCategory.RESTAURANT_OPS, NotificationPriority.HIGH),
    MENU_ITEM_OUT_OF_STOCK("Menu Item Out of Stock", NotificationCategory.RESTAURANT_OPS, NotificationPriority.HIGH),
    RESTAURANT_STATUS_CHANGED("Restaurant Status Changed", NotificationCategory.RESTAURANT_OPS, NotificationPriority.NORMAL),
    DAILY_SUMMARY("Daily Summary", NotificationCategory.RESTAURANT_OPS, NotificationPriority.LOW),

    // ===== Promotion Events =====
    NEW_PROMOTION("New Promotion Available", NotificationCategory.PROMOTION, NotificationPriority.LOW),
    PROMO_CODE_APPLIED("Promo Code Applied", NotificationCategory.PROMOTION, NotificationPriority.NORMAL),
    PROMO_EXPIRING("Promotion Expiring Soon", NotificationCategory.PROMOTION, NotificationPriority.LOW),

    // ===== Account Events =====
    WELCOME("Welcome", NotificationCategory.ACCOUNT, NotificationPriority.NORMAL),
    PASSWORD_CHANGED("Password Changed", NotificationCategory.ACCOUNT, NotificationPriority.HIGH),
    PROFILE_UPDATED("Profile Updated", NotificationCategory.ACCOUNT, NotificationPriority.LOW),
    ACCOUNT_VERIFIED("Account Verified", NotificationCategory.ACCOUNT, NotificationPriority.NORMAL),
    ACCOUNT_SUSPENDED("Account Suspended", NotificationCategory.ACCOUNT, NotificationPriority.URGENT),

    // ===== System Events =====
    SYSTEM_MAINTENANCE("System Maintenance", NotificationCategory.SYSTEM, NotificationPriority.HIGH),
    SYSTEM_UPDATE("System Update", NotificationCategory.SYSTEM, NotificationPriority.NORMAL),
    TERMS_UPDATED("Terms & Conditions Updated", NotificationCategory.SYSTEM, NotificationPriority.LOW),
    NEW_FEATURE("New Feature Available", NotificationCategory.SYSTEM, NotificationPriority.LOW),

    // ===== Alert Events =====
    FRAUD_ALERT("Fraud Alert", NotificationCategory.ALERT, NotificationPriority.URGENT),
    UNUSUAL_ACTIVITY("Unusual Activity Detected", NotificationCategory.ALERT, NotificationPriority.HIGH),
    HIGH_DEMAND_ALERT("High Demand Alert", NotificationCategory.ALERT, NotificationPriority.NORMAL),
    SERVICE_DISRUPTION("Service Disruption", NotificationCategory.ALERT, NotificationPriority.URGENT);

    private final String displayName;
    private final NotificationCategory defaultCategory;
    private final NotificationPriority defaultPriority;

    NotificationType(String displayName, NotificationCategory defaultCategory, NotificationPriority defaultPriority) {
        this.displayName = displayName;
        this.defaultCategory = defaultCategory;
        this.defaultPriority = defaultPriority;
    }

    public String getDisplayName() {
        return displayName;
    }

    public NotificationCategory getDefaultCategory() {
        return defaultCategory;
    }

    public NotificationPriority getDefaultPriority() {
        return defaultPriority;
    }
}
