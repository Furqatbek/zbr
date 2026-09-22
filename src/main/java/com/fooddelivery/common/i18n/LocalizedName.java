package com.fooddelivery.common.i18n;

/**
 * Pick the name to render, falling back rather than rendering blank.
 *
 * <p>One implementation, because the same rule is applied in two places — on
 * the entity when the admin panel reads it, and on the DTO when a customer
 * request resolves it after a cache read. Two copies of a fallback chain drift
 * quietly, and the symptom is a chip with no label.
 *
 * <p>Uzbek is the backstop because it is the only name the schema requires.
 */
public final class LocalizedName {

    private LocalizedName() {
    }

    public static String pick(String language, String uz, String ru, String en) {
        String requested = switch (language == null ? RequestLanguage.DEFAULT : language) {
            case "ru" -> ru;
            case "en" -> en;
            default -> uz;
        };
        if (isPresent(requested)) {
            return requested;
        }
        if (isPresent(uz)) {
            return uz;
        }
        if (isPresent(ru)) {
            return ru;
        }
        return isPresent(en) ? en : null;
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
