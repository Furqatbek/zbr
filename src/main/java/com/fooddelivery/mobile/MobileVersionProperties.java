package com.fooddelivery.mobile;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

/**
 * What version of the customer app each store is serving.
 *
 * <p>Configuration rather than a table, because these change when a release
 * reaches a store — a handful of times a month — and because
 * {@code minimum-version} is a kill switch that should take a deliberate
 * deployment action rather than an API call anyone can make. Both are
 * environment-overridable, so a store release needs a restart and not a
 * rebuild.
 *
 * <h2>The dangerous value</h2>
 *
 * <p>{@code minimum-version} above what is actually live in the store locks
 * <em>every</em> customer out of the app: the dialog cannot be dismissed and
 * the store has nothing newer to install. There is no recovery from the app's
 * side. {@link #validate()} therefore refuses to start when a minimum is newer
 * than that platform's latest — the one mistake with no way back.
 */
@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "app.mobile.version")
public class MobileVersionProperties {

    /** Per platform, because the two stores are never in lockstep. */
    private Map<MobilePlatform, Release> platforms = new EnumMap<>(Map.of(
            MobilePlatform.IOS, new Release(),
            MobilePlatform.ANDROID, new Release()
    ));

    /** How long a client may cache the answer, in seconds. */
    private long cacheSeconds = 300;

    @Data
    public static class Release {
        /** Newest version live in that platform's store. */
        private String latest = "1.0.1";
        /** Oldest version still allowed to run. */
        private String minimum = "1.0.0";
        /** That platform's store link. Null is fine — the app falls back to its own. */
        private String storeUrl;
    }

    public Release forPlatform(MobilePlatform platform) {
        Release release = platforms.get(platform);
        if (release == null) {
            // Cannot happen after validate(), which is the point of validate().
            throw new IllegalStateException("No app version configured for " + platform);
        }
        return release;
    }

    @PostConstruct
    void validate() {
        for (MobilePlatform platform : MobilePlatform.values()) {
            Release release = platforms.get(platform);
            if (release == null) {
                throw new IllegalStateException(
                        "app.mobile.version.platforms." + name(platform) + " is not configured. "
                                + "Every platform the app runs on needs a latest and a minimum.");
            }

            SemanticVersion latest = parse(platform, "latest", release.getLatest());
            SemanticVersion minimum = parse(platform, "minimum", release.getMinimum());

            if (minimum.compareTo(latest) > 0) {
                throw new IllegalStateException(
                        "app.mobile.version.platforms." + name(platform) + ": minimum "
                                + release.getMinimum() + " is newer than latest " + release.getLatest()
                                + ". That locks every " + name(platform) + " customer out of the app "
                                + "with nothing in the store to upgrade to.");
            }

            String storeUrl = release.getStoreUrl();
            if (storeUrl != null && !storeUrl.isBlank()
                    && !(storeUrl.startsWith("https://") || storeUrl.startsWith("http://"))) {
                throw new IllegalStateException(
                        "app.mobile.version.platforms." + name(platform)
                                + ".store-url must be an http(s) URL, got '" + storeUrl + "'");
            }

            log.info("App version for {}: latest {}, minimum {}",
                    name(platform), release.getLatest(), release.getMinimum());
        }
    }

    private static SemanticVersion parse(MobilePlatform platform, String field, String value) {
        try {
            return SemanticVersion.parse(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.mobile.version.platforms." + name(platform) + "."
                    + field + ": " + e.getMessage(), e);
        }
    }

    private static String name(MobilePlatform platform) {
        return platform.name().toLowerCase(java.util.Locale.ROOT);
    }
}
