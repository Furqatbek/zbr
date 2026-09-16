package com.fooddelivery.notification.repository;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserStatus;
import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.courier.entity.CourierStatus;
import com.fooddelivery.courier.entity.VehicleType;
import com.fooddelivery.notification.entity.UserDeviceToken;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Who a broadcast push actually reaches.
 *
 * <p>The role query matched only {@code User.role}, the singular column. Both
 * paths that make someone a courier or a restaurant owner add to the
 * {@code user_roles} COLLECTION and leave that column at its CONSUMER default —
 * so every role broadcast selected nobody, silently, while the code read as
 * though it worked.
 *
 * <p>Only a real database can catch that: the query is a string, and a mocked
 * repository returns whatever the test says it returns.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:broadcast;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("Broadcast push audience")
class DeviceTokenBroadcastTest {

    @Autowired
    private UserDeviceTokenRepository repository;

    @Autowired
    private EntityManager em;

    private User user(Set<Role> roles) {
        User user = User.builder()
                .phone("99890" + System.nanoTime() % 10_000_000)
                .role(Role.CONSUMER)          // the default every OTP signup gets
                .roles(new java.util.HashSet<>(roles))
                .status(UserStatus.ACTIVE)
                .build();
        em.persist(user);
        return user;
    }

    private UserDeviceToken tokenFor(User user) {
        UserDeviceToken token = UserDeviceToken.builder()
                .userId(user.getId())
                .deviceToken("tok-" + user.getId())
                .deviceType(UserDeviceToken.DeviceType.ANDROID)
                .active(true)
                .build();
        em.persist(token);
        return token;
    }

    private void courier(User user, CourierStatus status, boolean verified) {
        Courier courier = Courier.builder()
                .user(user)
                .status(status)
                .verified(verified)
                .vehicleType(VehicleType.MOTORCYCLE)
                .build();
        em.persist(courier);
    }

    @Test
    @DisplayName("a role held only in the collection is still found")
    void findsRolesHeldInTheCollection() {
        // THE bug. A real courier looks exactly like this: role column CONSUMER,
        // COURIER in the collection.
        User courier = user(Set.of(Role.COURIER));
        tokenFor(courier);
        em.flush();

        List<UserDeviceToken> found = repository.findActiveTokensByUserRoles(List.of(Role.COURIER));

        assertThat(found).extracting(UserDeviceToken::getUserId).containsExactly(courier.getId());
    }

    @Test
    @DisplayName("a user holding several roles is returned once")
    void noDuplicatesForMultiRoleUsers() {
        User both = user(Set.of(Role.COURIER, Role.RESTAURANT_OWNER));
        tokenFor(both);
        em.flush();

        assertThat(repository.findActiveTokensByUserRoles(List.of(Role.COURIER, Role.RESTAURANT_OWNER)))
                .hasSize(1);
    }

    @Test
    @DisplayName("a delivery offer reaches only couriers who are online and approved")
    void offersGoOnlyToAvailableCouriers() {
        User available = user(Set.of(Role.COURIER));
        tokenFor(available);
        courier(available, CourierStatus.AVAILABLE, true);

        User offline = user(Set.of(Role.COURIER));
        tokenFor(offline);
        courier(offline, CourierStatus.OFFLINE, true);

        User unverified = user(Set.of(Role.COURIER));
        tokenFor(unverified);
        courier(unverified, CourierStatus.AVAILABLE, false);

        User notACourier = user(Set.of(Role.CONSUMER));
        tokenFor(notACourier);

        em.flush();

        assertThat(repository.findActiveTokensForAvailableCouriers())
                .extracting(UserDeviceToken::getUserId)
                .containsExactly(available.getId());
    }

    @Test
    @DisplayName("an inactive token is never broadcast to")
    void inactiveTokensExcluded() {
        User courier = user(Set.of(Role.COURIER));
        UserDeviceToken token = tokenFor(courier);
        token.setActive(false);
        courier(courier, CourierStatus.AVAILABLE, true);
        em.flush();

        assertThat(repository.findActiveTokensForAvailableCouriers()).isEmpty();
        assertThat(repository.findActiveTokensByUserRoles(List.of(Role.COURIER))).isEmpty();
    }

    @Test
    @DisplayName("a suspended account is never broadcast to")
    void suspendedUsersExcluded() {
        User courier = user(Set.of(Role.COURIER));
        courier.setStatus(UserStatus.SUSPENDED);
        tokenFor(courier);
        courier(courier, CourierStatus.AVAILABLE, true);
        em.flush();

        assertThat(repository.findActiveTokensForAvailableCouriers()).isEmpty();
    }
}
