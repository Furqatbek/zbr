package com.fooddelivery.analytics.financial.model;

/**
 * Types of promotions/discounts.
 */
public enum PromotionType {
    /**
     * Percentage discount (e.g., 20% off).
     */
    PERCENTAGE_DISCOUNT,

    /**
     * Fixed amount discount (e.g., $5 off).
     */
    FIXED_DISCOUNT,

    /**
     * Free delivery promotion.
     */
    FREE_DELIVERY,

    /**
     * Buy one get one (BOGO).
     */
    BOGO,

    /**
     * First order discount.
     */
    FIRST_ORDER,

    /**
     * Referral reward.
     */
    REFERRAL,

    /**
     * Gift card redemption.
     */
    GIFT_CARD,

    /**
     * Loyalty points redemption.
     */
    LOYALTY_POINTS,

    /**
     * Flash sale discount.
     */
    FLASH_SALE,

    /**
     * Restaurant-specific promotion.
     */
    RESTAURANT_PROMO,

    /**
     * Cashback offer.
     */
    CASHBACK
}
