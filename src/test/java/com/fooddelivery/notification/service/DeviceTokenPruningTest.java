package com.fooddelivery.notification.service;

import com.fooddelivery.notification.entity.UserDeviceToken;
import com.fooddelivery.notification.repository.UserDeviceTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Retiring a token the push provider has rejected.
 *
 * <p>This ran for months without ever working. The repository call is
 * {@code @Modifying}, every caller is a push sender running outside a
 * transaction, and the resulting
 * {@code TransactionRequiredException: Executing an update/delete query} was
 * swallowed by each sender's catch-all. Dead tokens therefore stayed active and
 * every subsequent push retried them — visible in production only as the same
 * APNs rejection repeating forever.
 *
 * <p>The test runs with {@code NOT_SUPPORTED} so it calls from outside a
 * transaction exactly as the senders do. Without that it would inherit the
 * test's own transaction and pass against the broken code.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(DeviceTokenService.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:tokenprune;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("Rejected device token pruning")
class DeviceTokenPruningTest {

    @Autowired
    private DeviceTokenService deviceTokenService;

    @Autowired
    private UserDeviceTokenRepository repository;

    private UserDeviceToken activeToken(String token) {
        return repository.saveAndFlush(UserDeviceToken.builder()
                .userId(42L)
                .deviceToken(token)
                .deviceType(UserDeviceToken.DeviceType.IOS)
                .active(true)
                .build());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("commits when called from outside a transaction, as the senders do")
    void prunesFromANonTransactionalCaller() {
        UserDeviceToken token = activeToken("dead-apns-token");

        deviceTokenService.deactivateRejectedToken("dead-apns-token", "APNs 410 Unregistered");

        assertThat(repository.findById(token.getId()))
                .get()
                .extracting(UserDeviceToken::getActive)
                .isEqualTo(false);

        repository.deleteAll();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("leaves other devices alone")
    void doesNotTouchOtherTokens() {
        UserDeviceToken dead = activeToken("dead-token");
        UserDeviceToken alive = activeToken("good-token");

        deviceTokenService.deactivateRejectedToken("dead-token", "APNs BadDeviceToken");

        assertThat(repository.findById(dead.getId()).orElseThrow().getActive()).isFalse();
        assertThat(repository.findById(alive.getId()).orElseThrow().getActive()).isTrue();

        repository.deleteAll();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("an unknown token is a no-op, not an error")
    void unknownTokenDoesNotThrow() {
        // The provider can reject a token we have already retired; that must not
        // surface as a failure in the send that discovered it.
        deviceTokenService.deactivateRejectedToken("never-seen", "FCM rejected the token");
    }
}
