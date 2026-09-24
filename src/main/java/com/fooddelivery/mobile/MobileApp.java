package com.fooddelivery.mobile;

import com.fooddelivery.common.exception.BusinessException;

import java.util.Locale;

/**
 * Which of our three apps is asking.
 *
 * <p>The version endpoint was built when there was one app and answered by
 * platform alone. With three, that answer is wrong for two of them: a courier
 * on an old build was told the latest version was the customer app's, and the
 * store link sent them to install a different app entirely.
 *
 * <p>Defaults to {@link #CUSTOMER} when absent, because that is who the
 * endpoint answered before this existed and a shipped customer build does not
 * send the parameter.
 */
public enum MobileApp {

    CUSTOMER,
    COURIER,
    VENDOR;

    public static MobileApp from(String raw) {
        if (raw == null || raw.isBlank()) {
            return CUSTOMER;
        }
        String key = raw.trim().toUpperCase(Locale.ROOT);
        // The vendor app is called "owner" by the people who build it and
        // "restaurant" by the docs. Both mean this one.
        if ("OWNER".equals(key) || "RESTAURANT".equals(key)) {
            return VENDOR;
        }
        try {
            return valueOf(key);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "Unknown app '" + raw + "'. Use customer, courier or vendor.");
        }
    }
}
