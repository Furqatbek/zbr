package com.fooddelivery.mobile;

import com.fooddelivery.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The answer the customer app gets on launch.
 */
@DisplayName("App version endpoint")
class AppVersionControllerTest {

    private MobileVersionProperties properties;
    private AppVersionController controller;

    @BeforeEach
    void setUp() {
        properties = new MobileVersionProperties();
        properties.forPlatform(MobilePlatform.IOS).setLatest("1.4.0");
        properties.forPlatform(MobilePlatform.IOS).setMinimum("1.2.0");
        properties.forPlatform(MobilePlatform.IOS).setStoreUrl("https://apps.apple.com/app/id123");
        properties.forPlatform(MobilePlatform.ANDROID).setLatest("1.3.0");
        properties.forPlatform(MobilePlatform.ANDROID).setMinimum("1.2.0");
        properties.forPlatform(MobilePlatform.ANDROID)
                .setStoreUrl("https://play.google.com/store/apps/details?id=app.zbr.customer");

        controller = new AppVersionController(properties);
    }

    private AppVersionResponse get(String platform) {
        return controller.version(platform).getBody().getData();
    }

    @Test
    @DisplayName("each platform is told about its own store")
    void answersPerPlatform() {
        // The whole reason platform is required. A shared answer would tell
        // every Android customer to install 1.4.0, which does not exist for
        // them — on every check, forever, because their version never catches
        // up.
        assertThat(get("ios").getLatestVersion()).isEqualTo("1.4.0");
        assertThat(get("android").getLatestVersion()).isEqualTo("1.3.0");
        assertThat(get("ios").getStoreUrl()).contains("apps.apple.com");
        assertThat(get("android").getStoreUrl()).contains("play.google.com");
    }

    @Test
    @DisplayName("the minimum is answered too")
    void answersTheMinimum() {
        assertThat(get("ios").getMinimumVersion()).isEqualTo("1.2.0");
        assertThat(get("android").getMinimumVersion()).isEqualTo("1.2.0");
    }

    @Test
    @DisplayName("case does not matter")
    void platformIsCaseInsensitive() {
        assertThat(get("iOS").getLatestVersion()).isEqualTo("1.4.0");
        assertThat(get("ANDROID").getLatestVersion()).isEqualTo("1.3.0");
        assertThat(get(" android ").getLatestVersion()).isEqualTo("1.3.0");
    }

    @Test
    @DisplayName("a missing platform is refused rather than defaulted")
    void platformIsRequired() {
        // Defaulting would answer an iPhone with Android's store link, which
        // the app discards — leaving a prompt with nowhere to go.
        assertThatThrownBy(() -> controller.version(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("platform is required");
    }

    @Test
    @DisplayName("an unknown platform says what is accepted")
    void unknownPlatformIsNamed() {
        assertThatThrownBy(() -> controller.version("huawei"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ios or android");
    }

    @Test
    @DisplayName("an unset store URL is omitted, not sent empty")
    void blankStoreUrlIsOmitted() {
        // The app falls back to its own link when the field is absent. An
        // empty string is a URL it would have to decide about.
        properties.forPlatform(MobilePlatform.IOS).setStoreUrl("");

        assertThat(get("ios").getStoreUrl()).isNull();
    }

    @Test
    @DisplayName("the answer is cacheable, since every caller gets the same one")
    void isCacheable() {
        String cacheControl = controller.version("ios").getHeaders().getCacheControl();

        assertThat(cacheControl).contains("max-age=300").contains("public");
    }
}
