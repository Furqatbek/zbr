package com.fooddelivery.notification.util;

import lombok.experimental.UtilityClass;

/**
 * Constants for notification module.
 */
@UtilityClass
public class NotificationConstants {

    // ===== Cache Keys =====
    public static final String CACHE_UNREAD_COUNT = "notification:unread:";
    public static final String CACHE_NOTIFICATIONS = "notification:list:";
    public static final String CACHE_NOTIFICATION_DETAIL = "notification:detail:";

    // ===== Cache TTL (seconds) =====
    public static final long CACHE_TTL_UNREAD_COUNT = 60;
    public static final long CACHE_TTL_NOTIFICATIONS = 30;
    public static final long CACHE_TTL_DETAIL = 300;

    // ===== Default Values =====
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_TTL_HOURS = 720; // 30 days

    // ===== Cleanup Thresholds =====
    public static final int DISMISSED_CLEANUP_DAYS = 7;
    public static final int READ_CLEANUP_DAYS = 90;
    public static final int UNREAD_CLEANUP_DAYS = 180;

    // ===== API Endpoints =====
    public static final String API_BASE_PATH = "/api/v1/notifications";

    // ===== Related Entity Types =====
    public static final String ENTITY_TYPE_ORDER = "ORDER";
    public static final String ENTITY_TYPE_RESTAURANT = "RESTAURANT";
    public static final String ENTITY_TYPE_COURIER = "COURIER";
    public static final String ENTITY_TYPE_CUSTOMER = "CUSTOMER";
    public static final String ENTITY_TYPE_TICKET = "SUPPORT_TICKET";
    public static final String ENTITY_TYPE_PAYMENT = "PAYMENT";
    public static final String ENTITY_TYPE_PAYOUT = "PAYOUT";

    // ===== Icons =====
    public static final String ICON_ORDER = "shopping-bag";
    public static final String ICON_DELIVERY = "truck";
    public static final String ICON_PAYMENT = "credit-card";
    public static final String ICON_ALERT = "alert-triangle";
    public static final String ICON_SUCCESS = "check-circle";
    public static final String ICON_ERROR = "x-circle";
    public static final String ICON_INFO = "info";
    public static final String ICON_SUPPORT = "headphones";
    public static final String ICON_PROMO = "tag";
    public static final String ICON_ACCOUNT = "user";
    public static final String ICON_SYSTEM = "settings";

    // ===== Action URL Templates =====
    public static final String ACTION_ORDER_DETAIL = "/orders/{orderId}";
    public static final String ACTION_DELIVERY_TRACKING = "/tracking/{orderId}";
    public static final String ACTION_SUPPORT_TICKET = "/support/tickets/{ticketId}";
    public static final String ACTION_PAYMENT_DETAIL = "/payments/{paymentId}";
    public static final String ACTION_RESTAURANT_ORDERS = "/restaurant/orders";
    public static final String ACTION_COURIER_DELIVERIES = "/courier/deliveries";
}
