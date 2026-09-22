package com.fooddelivery.mobile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The configuration behind the update prompt, and the one value that can lock
 * every customer out of the app.
 */
@DisplayName("Mobile version configuration")
class MobileVersionPropertiesTest {

    private static final String PREFIX = "app.mobile.version";

    private static MobileVersionProperties bind(Map<String, Object> properties) {
        MobileVersionProperties target = new MobileVersionProperties();
        new Binder(new MapConfigurationPropertySource(properties))
                .bind(PREFIX, Bindable.ofInstance(target));
        return target;
    }

    @Test
    @DisplayName("the shipped defaults start the application")
    void defaultsValidate() {
        assertThatCode(() -> new MobileVersionProperties().validate()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a minimum newer than the store's latest refuses to start")
    void killSwitchIsRefused() {
        // The failure with no way back: the dialog cannot be dismissed and the
        // store has nothing newer to install, so every customer on that
        // platform is stuck outside the app until a deploy fixes it. Better a
        // deployment that does not start.
        MobileVersionProperties properties = bind(Map.of(
                PREFIX + ".platforms.ANDROID.latest", "1.0.1",
                PREFIX + ".platforms.ANDROID.minimum", "1.2.0"));

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("android")
                .hasMessageContaining("locks every");
    }

    @Test
    @DisplayName("a minimum equal to the latest is allowed")
    void minimumMayEqualLatest() {
        MobileVersionProperties properties = bind(Map.of(
                PREFIX + ".platforms.IOS.latest", "1.4.0",
                PREFIX + ".platforms.IOS.minimum", "1.4.0"));

        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a version that is not a version refuses to start, naming the key")
    void unparseableVersionIsRefused() {
        MobileVersionProperties properties = bind(Map.of(
                PREFIX + ".platforms.IOS.latest", "next"));

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.mobile.version.platforms.ios.latest");
    }

    @Test
    @DisplayName("each platform is configured independently")
    void platformsDoNotShareValues() {
        // The reason this is a map and not two fields: an iOS release must not
        // move Android's number.
        MobileVersionProperties properties = bind(Map.of(
                PREFIX + ".platforms.IOS.latest", "1.4.0",
                PREFIX + ".platforms.ANDROID.latest", "1.3.0"));

        assertThat(properties.forPlatform(MobilePlatform.IOS).getLatest()).isEqualTo("1.4.0");
        assertThat(properties.forPlatform(MobilePlatform.ANDROID).getLatest()).isEqualTo("1.3.0");
    }

    @Test
    @DisplayName("a platform with no configuration at all refuses to start")
    void everyPlatformMustBeConfigured() {
        // Adding a platform to the enum without configuring it would otherwise
        // be a 500 on the first launch from that store.
        MobileVersionProperties properties = new MobileVersionProperties();
        properties.getPlatforms().remove(MobilePlatform.IOS);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ios");
    }

    @Test
    @DisplayName("a store URL that is not a URL refuses to start")
    void storeUrlMustBeAUrl() {
        MobileVersionProperties properties = bind(Map.of(
                PREFIX + ".platforms.ANDROID.store-url", "play.google.com/store"));

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("store-url");
    }

    @Test
    @DisplayName("no store URL is fine — the app has its own")
    void storeUrlIsOptional() {
        MobileVersionProperties properties = bind(Map.of(
                PREFIX + ".platforms.IOS.store-url", ""));

        assertThatCode(properties::validate).doesNotThrowAnyException();
    }
}
