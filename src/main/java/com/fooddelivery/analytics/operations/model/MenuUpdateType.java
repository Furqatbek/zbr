package com.fooddelivery.analytics.operations.model;

/**
 * Types of menu updates for tracking menu update history.
 */
public enum MenuUpdateType {
    /**
     * New item added to menu.
     */
    ITEM_ADDED,

    /**
     * Item removed from menu.
     */
    ITEM_REMOVED,

    /**
     * Item details updated (name, description, etc.).
     */
    ITEM_MODIFIED,

    /**
     * Item price changed.
     */
    PRICE_CHANGED,

    /**
     * Item availability changed.
     */
    AVAILABILITY_CHANGED,

    /**
     * Category added or modified.
     */
    CATEGORY_CHANGED,

    /**
     * Bulk menu update (multiple changes).
     */
    BULK_UPDATE,

    /**
     * Menu imported from external source.
     */
    MENU_IMPORTED;

    /**
     * Check if this is a significant update.
     */
    public boolean isSignificantUpdate() {
        return this == ITEM_ADDED || this == ITEM_REMOVED ||
               this == BULK_UPDATE || this == MENU_IMPORTED;
    }
}
