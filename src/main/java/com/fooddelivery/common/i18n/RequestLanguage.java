package com.fooddelivery.common.i18n;

import java.util.Locale;
import java.util.Set;

/**
 * Which language to answer a request in.
 *
 * <p>The customer app sends {@code Accept-Language: en | ru | uz} as a bare
 * code, but browsers and other clients send things like {@code ru-RU},
 * {@code uz-Latn-UZ} or a whole weighted list, so this takes the first tag it
 * understands rather than requiring the exact three.
 *
 * <p>Uzbek is the default when nothing is asked for, because the platform
 * serves Uzbekistan and an unlabelled request is more likely to be a local
 * customer than an English-speaking one. It is one line to change if that turns
 * out to be wrong.
 */
public final class RequestLanguage {

    public static final String DEFAULT = "uz";

    private static final Set<String> SUPPORTED = Set.of("uz", "ru", "en");

    private RequestLanguage() {
    }

    /**
     * @param acceptLanguage the raw header, which may be null, a bare code, a
     *                       region-tagged one, or a q-weighted list
     */
    public static String from(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return DEFAULT;
        }
        for (String candidate : acceptLanguage.split(",")) {
            // Drop any q-weight, then any region or script subtag: "ru-RU;q=0.9"
            // is a request for Russian, and refusing it over punctuation would
            // hand a Russian speaker Uzbek.
            String tag = candidate.split(";")[0].trim().toLowerCase(Locale.ROOT);
            int dash = tag.indexOf('-');
            if (dash > 0) {
                tag = tag.substring(0, dash);
            }
            if (SUPPORTED.contains(tag)) {
                return tag;
            }
        }
        return DEFAULT;
    }
}
