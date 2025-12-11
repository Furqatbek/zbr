package com.fooddelivery.analytics.financial.model;

/**
 * Types of commission structures for restaurants.
 */
public enum CommissionType {
    /**
     * Percentage-based commission (e.g., 15% of order subtotal).
     */
    PERCENTAGE,

    /**
     * Fixed amount per order (e.g., $2.50 per order).
     */
    FIXED,

    /**
     * Hybrid: fixed amount plus percentage.
     */
    HYBRID
}
