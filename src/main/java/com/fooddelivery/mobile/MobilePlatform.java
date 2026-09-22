package com.fooddelivery.mobile;

import com.fooddelivery.common.exception.BusinessException;

import java.util.Locale;

/**
 * The stores an app can be installed from.
 *
 * <p>Separate answers per platform, because the two stores are never in
 * lockstep: a release approved on one while the other is still rolling out
 * means a shared "latest version" tells half the customers to install
 * something that does not exist for them — and since their installed version
 * never catches up, the prompt returns on every check, forever.
 */
public enum MobilePlatform {

    IOS,
    ANDROID;

    /**
     * @throws BusinessException naming what was accepted, because this arrives
     *         from a query string and a silent default would answer an
     *         iPhone with Android's store link
     */
    public static MobilePlatform from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("platform is required: ios or android");
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Unknown platform '" + raw + "'. Use ios or android.");
        }
    }
}
