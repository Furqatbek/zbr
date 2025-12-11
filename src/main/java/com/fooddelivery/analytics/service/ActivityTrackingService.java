package com.fooddelivery.analytics.service;

import com.fooddelivery.analytics.model.ActivityEventType;
import com.fooddelivery.analytics.model.ActivityLog;

/**
 * Service for tracking user activity events.
 * Used to record events for analytics calculation.
 */
public interface ActivityTrackingService {

    /**
     * Track a user activity event.
     *
     * @param userId      ID of the user (can be null for anonymous)
     * @param eventType   Type of activity event
     * @param restaurantId Restaurant ID if applicable
     * @param orderId     Order ID if applicable
     * @param sessionId   Session ID for journey tracking
     * @param metadata    Additional metadata as JSON
     * @return The created activity log entry
     */
    ActivityLog trackActivity(
            Long userId,
            ActivityEventType eventType,
            Long restaurantId,
            Long orderId,
            String sessionId,
            String metadata);

    /**
     * Track a simple activity event.
     *
     * @param userId    ID of the user
     * @param eventType Type of activity event
     * @return The created activity log entry
     */
    ActivityLog trackActivity(Long userId, ActivityEventType eventType);

    /**
     * Track restaurant view event.
     *
     * @param userId       ID of the user
     * @param restaurantId ID of the viewed restaurant
     * @param sessionId    Session ID
     * @return The created activity log entry
     */
    ActivityLog trackRestaurantView(Long userId, Long restaurantId, String sessionId);

    /**
     * Track add to cart event.
     *
     * @param userId       ID of the user
     * @param restaurantId ID of the restaurant
     * @param menuItemId   ID of the menu item added
     * @param sessionId    Session ID
     * @return The created activity log entry
     */
    ActivityLog trackAddToCart(Long userId, Long restaurantId, Long menuItemId, String sessionId);

    /**
     * Track checkout start event.
     *
     * @param userId       ID of the user
     * @param restaurantId ID of the restaurant
     * @param sessionId    Session ID
     * @return The created activity log entry
     */
    ActivityLog trackCheckoutStart(Long userId, Long restaurantId, String sessionId);

    /**
     * Track payment completion event.
     *
     * @param userId       ID of the user
     * @param restaurantId ID of the restaurant
     * @param orderId      ID of the order
     * @param sessionId    Session ID
     * @return The created activity log entry
     */
    ActivityLog trackPaymentCompleted(Long userId, Long restaurantId, Long orderId, String sessionId);

    /**
     * Track user login event.
     *
     * @param userId      ID of the user
     * @param deviceType  Type of device (MOBILE, DESKTOP, TABLET)
     * @param ipAddress   User's IP address
     * @return The created activity log entry
     */
    ActivityLog trackLogin(Long userId, String deviceType, String ipAddress);

    /**
     * Track user registration event.
     *
     * @param userId         ID of the new user
     * @param referralSource Source of the registration
     * @return The created activity log entry
     */
    ActivityLog trackRegistration(Long userId, String referralSource);
}
