package com.fooddelivery.analytics.model;

/**
 * Enumeration of all tracked activity event types for analytics.
 * Used to build conversion funnels and track user behavior.
 */
public enum ActivityEventType {

    // ============ Authentication Events ============
    /**
     * User logged in successfully.
     */
    USER_LOGIN,

    /**
     * User logged out.
     */
    USER_LOGOUT,

    /**
     * New user registered.
     */
    USER_REGISTER,

    /**
     * User requested OTP.
     */
    OTP_REQUESTED,

    /**
     * User verified OTP.
     */
    OTP_VERIFIED,

    // ============ Browse Events ============
    /**
     * User viewed restaurant list/search results.
     */
    RESTAURANT_LIST_VIEW,

    /**
     * User viewed a specific restaurant page.
     */
    RESTAURANT_VIEW,

    /**
     * User viewed menu item details.
     */
    MENU_ITEM_VIEW,

    /**
     * User searched for restaurants/items.
     */
    SEARCH,

    // ============ Cart Events (Conversion Funnel) ============
    /**
     * User added item to cart.
     */
    ADD_TO_CART,

    /**
     * User removed item from cart.
     */
    REMOVE_FROM_CART,

    /**
     * User updated cart quantity.
     */
    CART_UPDATE,

    /**
     * User viewed cart.
     */
    CART_VIEW,

    // ============ Checkout Events (Conversion Funnel) ============
    /**
     * User started checkout process.
     */
    CHECKOUT_START,

    /**
     * User entered delivery address.
     */
    CHECKOUT_ADDRESS,

    /**
     * User selected payment method.
     */
    CHECKOUT_PAYMENT_METHOD,

    /**
     * User initiated payment.
     */
    PAYMENT_INITIATED,

    /**
     * Payment completed successfully.
     */
    PAYMENT_COMPLETED,

    /**
     * Payment failed.
     */
    PAYMENT_FAILED,

    // ============ Order Events ============
    /**
     * Order was created.
     */
    ORDER_CREATED,

    /**
     * User cancelled order.
     */
    ORDER_CANCELLED,

    /**
     * Order was delivered.
     */
    ORDER_DELIVERED,

    /**
     * User rated order.
     */
    ORDER_RATED,

    // ============ Engagement Events ============
    /**
     * User used a referral code.
     */
    REFERRAL_USED,

    /**
     * User generated a referral code.
     */
    REFERRAL_GENERATED,

    /**
     * User clicked on a promotion.
     */
    PROMOTION_CLICK,

    /**
     * App opened/session started.
     */
    APP_OPEN,

    /**
     * App closed/session ended.
     */
    APP_CLOSE;

    /**
     * Check if this event type is part of the conversion funnel.
     *
     * @return true if this event is a funnel step
     */
    public boolean isFunnelEvent() {
        return this == RESTAURANT_VIEW
                || this == ADD_TO_CART
                || this == CHECKOUT_START
                || this == PAYMENT_COMPLETED;
    }

    /**
     * Check if this event represents active user engagement.
     *
     * @return true if this event counts as active usage
     */
    public boolean isActiveEvent() {
        return this != APP_CLOSE && this != USER_LOGOUT;
    }

    /**
     * Get the funnel step order for this event type.
     *
     * @return funnel step number (1-4), or 0 if not a funnel event
     */
    public int getFunnelStep() {
        return switch (this) {
            case RESTAURANT_VIEW -> 1;
            case ADD_TO_CART -> 2;
            case CHECKOUT_START -> 3;
            case PAYMENT_COMPLETED -> 4;
            default -> 0;
        };
    }
}
