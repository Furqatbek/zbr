package com.fooddelivery.analytics.financial.model;

/**
 * Entity that funds the promotion cost.
 */
public enum PromotionFunder {
    /**
     * Platform fully funds the promotion.
     */
    PLATFORM,

    /**
     * Restaurant fully funds the promotion.
     */
    RESTAURANT,

    /**
     * Cost is shared between platform and restaurant.
     */
    SHARED
}
