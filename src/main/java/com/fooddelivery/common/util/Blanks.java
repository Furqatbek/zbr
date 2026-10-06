package com.fooddelivery.common.util;

/**
 * An empty string is not a value, and storing it as one is a lie clients pay
 * for.
 *
 * <p>Jackson is configured {@code default-property-inclusion: non_null}, so a
 * null field is absent from the JSON entirely and a client can treat "missing"
 * as "nothing here". An empty string defeats that: it serialises, it is
 * truthy-adjacent in several languages, and every consumer has to special-case
 * it. The customer app found {@code email: ""} this way; the QR landing page
 * found {@code imageUrl: ""} on every item of an imported menu and rendered a
 * broken image frame for each one.
 *
 * <p>Applied where data enters — a partner payload, a request body — rather
 * than on the way out, so the database holds one representation of "no value"
 * instead of two.
 */
public final class Blanks {

    private Blanks() {
    }

    /** Null for null, null for blank, trimmed otherwise. */
    public static String toNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
