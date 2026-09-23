package com.fooddelivery.notification.service;

import com.fooddelivery.notification.dto.DeviceTokenRequest;
import com.fooddelivery.notification.entity.UserDeviceToken;
import com.fooddelivery.notification.repository.UserDeviceTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two of our apps on one phone are two devices as far as push is concerned.
 *
 * <p>Registration upserted on (user, deviceId), which looks right and is not:
 * the apps report the SAME deviceId on one phone, because Android's ANDROID_ID
 * is scoped to the signing key and iOS's identifierForVendor to the vendor, and
 * all three of ours come from one developer. So the second app's registration
 * found the first app's row and overwrote it. A courier who also orders food —
 * most of them — ended up with one token, and every push went to whichever app
 * had launched last: job alerts on the customer app, order updates on the
 * courier app.
 *
 * <p>The database enforces this too, in V53. These tests use ddl-auto with
 * Flyway off, so they pin the service's behaviour rather than the index; both
 * exist because either alone would let the other drift.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(DeviceTokenService.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:tokenreg;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("Device token registration")
class DeviceTokenRegistrationTest {

    private static final Long USER = 42L;
    private static final String PHONE = "same-android-id";
    private static final String CUSTOMER_APP = "app.zbr.customer";
    private static final String COURIER_APP = "app.zbr.courier";

    @Autowired
    private DeviceTokenService deviceTokenService;

    @Autowired
    private UserDeviceTokenRepository repository;

    private DeviceTokenRequest registration(String token, String appId) {
        return DeviceTokenRequest.builder()
                .token(token)
                .platform(UserDeviceToken.DeviceType.ANDROID)
                .deviceId(PHONE)
                .appId(appId)
                .build();
    }

    @Test
    @DisplayName("two apps on one phone keep one token each")
    void twoAppsOneDevice() {
        deviceTokenService.registerToken(USER, registration("tok-customer", CUSTOMER_APP));
        deviceTokenService.registerToken(USER, registration("tok-courier", COURIER_APP));

        List<UserDeviceToken> tokens = repository.findByUserIdAndActiveTrue(USER);

        assertThat(tokens).hasSize(2);
        assertThat(tokens).extracting(UserDeviceToken::getAppId)
                .containsExactlyInAnyOrder(CUSTOMER_APP, COURIER_APP);
        // The customer app's token must still be the customer app's token. It
        // was this value the courier registration used to overwrite.
        assertThat(tokens).extracting(UserDeviceToken::getDeviceToken)
                .containsExactlyInAnyOrder("tok-customer", "tok-courier");
    }

    @Test
    @DisplayName("the same app re-registering replaces its own row, not another app's")
    void tokenRotationStaysInPlace() {
        // Why the key includes deviceId at all: the OS rotates push tokens, and
        // keying on the token alone leaves the old row behind, so the device
        // gets every push twice.
        deviceTokenService.registerToken(USER, registration("tok-customer", CUSTOMER_APP));
        deviceTokenService.registerToken(USER, registration("tok-courier", COURIER_APP));

        deviceTokenService.registerToken(USER, registration("tok-customer-rotated", CUSTOMER_APP));

        List<UserDeviceToken> tokens = repository.findByUserIdAndActiveTrue(USER);
        assertThat(tokens).hasSize(2);
        assertThat(tokens).extracting(UserDeviceToken::getDeviceToken)
                .containsExactlyInAnyOrder("tok-customer-rotated", "tok-courier");
    }

    @Test
    @DisplayName("an app that starts sending an appId adopts its own legacy row")
    void legacyRowIsAdopted() {
        // Every token registered before the apps sent an appId has none. Left
        // alone, that row keeps receiving EVERY push for the user, because
        // targeting fails open for a device whose app is unknown — so the wrong
        // app would go on buzzing after the fix shipped.
        deviceTokenService.registerToken(USER, registration("tok-legacy", null));

        deviceTokenService.registerToken(USER, registration("tok-legacy", CUSTOMER_APP));

        List<UserDeviceToken> tokens = repository.findByUserIdAndActiveTrue(USER);
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getAppId()).isEqualTo(CUSTOMER_APP);
    }

    @Test
    @DisplayName("only the first app to claim the legacy row gets it")
    void legacyRowIsClaimedOnce() {
        deviceTokenService.registerToken(USER, registration("tok-legacy", null));

        deviceTokenService.registerToken(USER, registration("tok-customer", CUSTOMER_APP));
        deviceTokenService.registerToken(USER, registration("tok-courier", COURIER_APP));

        List<UserDeviceToken> tokens = repository.findByUserIdAndActiveTrue(USER);
        assertThat(tokens).hasSize(2);
        assertThat(tokens).extracting(UserDeviceToken::getAppId)
                .containsExactlyInAnyOrder(CUSTOMER_APP, COURIER_APP);
    }

    @Test
    @DisplayName("two people on one phone stay separate")
    void differentUsersAreNotMerged() {
        deviceTokenService.registerToken(USER, registration("tok-customer", CUSTOMER_APP));
        deviceTokenService.registerToken(99L, registration("tok-other-person", CUSTOMER_APP));

        assertThat(repository.findByUserIdAndActiveTrue(USER)).hasSize(1);
        assertThat(repository.findByUserIdAndActiveTrue(99L)).hasSize(1);
    }
}
