package com.fooddelivery.analytics.financial.model;

/**
 * Types of courier bonuses.
 */
public enum BonusType {
    /**
     * Signing bonus for new couriers.
     */
    SIGN_UP,

    /**
     * Weekly completion bonus (e.g., complete 50 deliveries).
     */
    WEEKLY_TARGET,

    /**
     * Peak hour bonus.
     */
    PEAK_HOUR,

    /**
     * Rain/bad weather bonus.
     */
    WEATHER,

    /**
     * High demand area bonus.
     */
    SURGE_AREA,

    /**
     * Performance bonus (high rating).
     */
    PERFORMANCE,

    /**
     * Referral bonus (for referring new couriers).
     */
    REFERRAL,

    /**
     * Quest/challenge completion bonus.
     */
    QUEST,

    /**
     * Guaranteed minimum earnings top-up.
     */
    GUARANTEE_TOP_UP,

    /**
     * Other/miscellaneous bonus.
     */
    OTHER
}
