package com.fooddelivery.integration.partner.entity;

/**
 * What a partner may do to one venue.
 *
 * <p>Held per venue grant rather than per partner, so authorising Restos to
 * write into one restaurant's menu says nothing about the next restaurant. A
 * grant carrying none of these can read nothing and write nothing, which is
 * what makes a freshly created grant inert until someone decides otherwise.
 */
public enum PartnerCapability {

    /** Change price and availability of menu items in this venue. */
    MENU_WRITE,

    /** Report an order's progress — accepted, declined, ready. */
    ORDER_STATUS_WRITE
}
