package com.fooddelivery.notification.service;

import com.fooddelivery.notification.entity.UserDeviceToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A courier is the same {@code users} row as a consumer, so push aimed at one
 * app reaches the other. This narrows it — but the failure modes are asymmetric
 * and that shapes every case below: sending a notification to one extra app is
 * an annoyance, while filtering one out silently stops push for a whole
 * audience and looks exactly like a broken app. Everything here fails open.
 */
@DisplayName("PushAppIdResolver")
class PushAppIdResolverTest {

    private static final String CUSTOMER_APP = "app.zbr.customer";
    private static final String COURIER_APP = "app.zbr.courier";

    private PushAppIdResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PushAppIdResolver();
        resolver.setAppIds(Map.of(
                "consumer", CUSTOMER_APP,
                "courier", COURIER_APP));
    }

    private UserDeviceToken device(String appId) {
        return UserDeviceToken.builder().deviceToken("tok-" + appId).appId(appId).build();
    }

    @Nested
    @DisplayName("when configured")
    class Configured {

        @Test
        @DisplayName("a courier alert skips the same person's customer app")
        void narrowsToTheTargetApp() {
            List<UserDeviceToken> devices = List.of(device(CUSTOMER_APP), device(COURIER_APP));

            List<UserDeviceToken> kept = resolver.filter(devices, "COURIER");

            assertThat(kept).extracting(UserDeviceToken::getAppId)
                    .containsExactly(COURIER_APP);
        }

        @Test
        @DisplayName("CUSTOMER and CONSUMER are one audience")
        void deprecatedRoleSpellingResolves() {
            // NotificationRole still emits both; an operator should not have to
            // configure the same bundle id under two keys.
            assertThat(resolver.appIdFor("CUSTOMER")).isEqualTo(CUSTOMER_APP);
            assertThat(resolver.appIdFor("CONSUMER")).isEqualTo(CUSTOMER_APP);
        }

        @Test
        @DisplayName("role matching ignores case")
        void caseInsensitive() {
            assertThat(resolver.appIdFor("courier")).isEqualTo(COURIER_APP);
            assertThat(resolver.appIdFor("Courier")).isEqualTo(COURIER_APP);
        }
    }

    @Nested
    @DisplayName("fails open")
    class FailsOpen {

        @Test
        @DisplayName("a token with no appId still receives")
        void nullAppIdAlwaysReceives() {
            // Most rows predate the column (V37) or come from clients that never
            // sent it. Excluding these would stop push for nearly every device
            // already registered.
            List<UserDeviceToken> devices = List.of(device(null), device(CUSTOMER_APP));

            assertThat(resolver.filter(devices, "COURIER"))
                    .extracting(UserDeviceToken::getAppId)
                    .containsExactly((String) null);
        }

        @Test
        @DisplayName("a blank appId is treated as absent, not as a non-match")
        void blankAppIdAlwaysReceives() {
            assertThat(resolver.filter(List.of(device("")), "COURIER")).hasSize(1);
        }

        @Test
        @DisplayName("an unconfigured role filters nothing")
        void unconfiguredRolePassesEverything() {
            List<UserDeviceToken> devices = List.of(device(CUSTOMER_APP), device(COURIER_APP));

            // RESTAURANT is absent from the map in this test.
            assertThat(resolver.filter(devices, "RESTAURANT")).isEqualTo(devices);
        }

        @Test
        @DisplayName("no configuration at all filters nothing")
        void emptyConfigPassesEverything() {
            // The shipped default. This whole feature is inert until an operator
            // supplies real bundle ids, because a wrong id here is worse than
            // the duplicate delivery it prevents.
            PushAppIdResolver unconfigured = new PushAppIdResolver();
            List<UserDeviceToken> devices = List.of(device(CUSTOMER_APP), device(COURIER_APP));

            assertThat(unconfigured.filter(devices, "COURIER")).isEqualTo(devices);
        }

        @Test
        @DisplayName("a null role filters nothing")
        void nullRolePassesEverything() {
            // Messages already in the queue when this deploys carry no role.
            List<UserDeviceToken> devices = List.of(device(CUSTOMER_APP));

            assertThat(resolver.filter(devices, null)).isEqualTo(devices);
        }

        @Test
        @DisplayName("a system-wide broadcast reaches every app")
        void allRoleIsNotNarrowed() {
            List<UserDeviceToken> devices = List.of(device(CUSTOMER_APP), device(COURIER_APP));

            assertThat(resolver.appIdFor("ALL")).isNull();
            assertThat(resolver.filter(devices, "ALL")).isEqualTo(devices);
        }

        @Test
        @DisplayName("a role with no app of its own reaches every app")
        void staffRolesAreNotNarrowed() {
            // ADMIN, FINANCE, SUPPORT and OPERATIONS have no mobile app; their
            // notifications should not be filtered to nothing.
            assertThat(resolver.appIdFor("ADMIN")).isNull();
            assertThat(resolver.appIdFor("FINANCE")).isNull();
        }

        @Test
        @DisplayName("an unknown role name filters nothing")
        void unknownRolePassesEverything() {
            assertThat(resolver.appIdFor("NOT_A_ROLE")).isNull();
        }
    }
}
