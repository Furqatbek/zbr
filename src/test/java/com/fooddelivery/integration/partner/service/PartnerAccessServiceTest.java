package com.fooddelivery.integration.partner.service;

import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.integration.partner.entity.Partner;
import com.fooddelivery.integration.partner.entity.PartnerCapability;
import com.fooddelivery.integration.partner.entity.PartnerVenueGrant;
import com.fooddelivery.integration.partner.repository.PartnerRepository;
import com.fooddelivery.integration.partner.repository.PartnerVenueGrantRepository;
import com.fooddelivery.integration.partner.security.PartnerPrincipal;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * The boundary between one partner's reach and another's.
 *
 * <p>A partner key carries authority over many restaurants, which makes this
 * the one place in the codebase where a missing check does not leak one
 * account's data but one business's — repricing a competitor's menu, or
 * cancelling their orders.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Partner venue access")
class PartnerAccessServiceTest {

    @Mock private PartnerRepository partnerRepository;
    @Mock private PartnerVenueGrantRepository grantRepository;
    @Mock private RestaurantService restaurantService;

    /** A real cipher rather than a mock: it is 30 lines and the real one round-trips. */
    private static final String SECRET_KEY =
            java.util.Base64.getEncoder().encodeToString(new byte[32]);

    private PartnerAccessService service;
    private PartnerPrincipal restos;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        service = new PartnerAccessService(partnerRepository, grantRepository, restaurantService,
                new com.fooddelivery.common.security.SecretCipher(SECRET_KEY));
        // The apps do not set a payment mode yet, so live order push is barred.
        // Individual tests turn it on where that is what they are testing.
        org.springframework.test.util.ReflectionTestUtils.setField(
                service, "paymentModeAuthoritative", true);
        Partner partner = Partner.builder().id(7L).code("RESTOS").active(true).build();
        restos = new PartnerPrincipal(partner, 1L);
        restaurant = Restaurant.builder().id(100L).name("Osh Markazi").build();

        when(grantRepository.findByPartnerIdAndExternalVenueId(any(), anyString()))
                .thenReturn(Optional.empty());
        when(grantRepository.findByPartnerIdAndRestaurantId(any(), any()))
                .thenReturn(Optional.empty());
    }

    private PartnerVenueGrant grant(PartnerCapability... capabilities) {
        return PartnerVenueGrant.builder()
                .id(1L).restaurant(restaurant).externalVenueId("venue-55")
                .capabilities(Set.of(capabilities))
                .build();
    }

    @Test
    @DisplayName("a granted venue resolves to our restaurant")
    void resolvesGrantedVenue() {
        when(grantRepository.findByPartnerIdAndExternalVenueId(7L, "venue-55"))
                .thenReturn(Optional.of(grant(PartnerCapability.MENU_WRITE)));

        assertThat(service.resolveVenue(restos, "venue-55", PartnerCapability.MENU_WRITE))
                .isSameAs(restaurant);
    }

    @Test
    @DisplayName("a venue nobody granted them is not found")
    void ungrantedVenueIsNotFound() {
        // 404 rather than 403, and the same answer as a venue id that belongs
        // to a different partner — otherwise the endpoint tells a caller which
        // venue ids exist.
        assertThatThrownBy(() -> service.resolveVenue(restos, "venue-999", PartnerCapability.MENU_WRITE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("a grant without the capability is refused, not silently allowed")
    void missingCapabilityRefused() {
        // A fresh grant carries nothing: mapping a venue and authorising writes
        // into its kitchen are separate decisions.
        when(grantRepository.findByPartnerIdAndExternalVenueId(7L, "venue-55"))
                .thenReturn(Optional.of(grant()));

        assertThatThrownBy(() -> service.resolveVenue(restos, "venue-55", PartnerCapability.MENU_WRITE))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("one capability does not imply the other")
    void capabilitiesAreIndependent() {
        when(grantRepository.findByPartnerIdAndExternalVenueId(7L, "venue-55"))
                .thenReturn(Optional.of(grant(PartnerCapability.ORDER_STATUS_WRITE)));

        // Being allowed to say an order is ready is not being allowed to change
        // what things cost.
        assertThatThrownBy(() -> service.resolveVenue(restos, "venue-55", PartnerCapability.MENU_WRITE))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(service.resolveVenue(restos, "venue-55", PartnerCapability.ORDER_STATUS_WRITE))
                .isSameAs(restaurant);
    }

    @Test
    @DisplayName("an order in an ungranted restaurant is not found")
    void orderInUngrantedRestaurantIsNotFound() {
        // Order references are ours and predictable in shape, so this check is
        // what stops a partner walking other restaurants' orders.
        assertThatThrownBy(() -> service.requireCapabilityOnRestaurant(
                restos, 100L, PartnerCapability.ORDER_STATUS_WRITE))
                .isInstanceOf(ResourceNotFoundException.class)
                // The message must not confirm the order exists.
                .hasMessageContaining("Order not found");
    }

    @Test
    @DisplayName("a grant is resolved for the partner who holds it, not by venue id alone")
    void grantsAreScopedToThePartner() {
        // The same venue id string may exist in two partners' systems. The
        // lookup is keyed by both, so one partner's id can never resolve to
        // another partner's restaurant.
        when(grantRepository.findByPartnerIdAndExternalVenueId(7L, "venue-55"))
                .thenReturn(Optional.of(grant(PartnerCapability.MENU_WRITE)));

        Partner other = Partner.builder().id(8L).code("OTHER").active(true).build();
        PartnerPrincipal otherPartner = new PartnerPrincipal(other, 2L);

        assertThatThrownBy(() -> service.resolveVenue(otherPartner, "venue-55", PartnerCapability.MENU_WRITE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("re-granting a venue updates it instead of failing on the unique index")
    void regrantUpdatesInPlace() {
        Partner partner = Partner.builder().id(7L).code("RESTOS").active(true).build();
        when(partnerRepository.findById(7L)).thenReturn(Optional.of(partner));
        when(restaurantService.getRestaurantEntityById(100L)).thenReturn(restaurant);
        when(grantRepository.findByPartnerIdAndRestaurantId(7L, 100L))
                .thenReturn(Optional.of(grant(PartnerCapability.MENU_WRITE)));
        when(grantRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PartnerVenueGrant updated = service.grantVenue(7L, 100L, "venue-56",
                Set.of(PartnerCapability.ORDER_STATUS_WRITE), null);

        assertThat(updated.getExternalVenueId()).isEqualTo("venue-56");
        assertThat(updated.getCapabilities()).containsExactly(PartnerCapability.ORDER_STATUS_WRITE);
    }

    @Test
    @DisplayName("granting with no capabilities leaves the mapping inert")
    void grantWithoutCapabilitiesIsInert() {
        Partner partner = Partner.builder().id(7L).code("RESTOS").active(true).build();
        when(partnerRepository.findById(7L)).thenReturn(Optional.of(partner));
        when(restaurantService.getRestaurantEntityById(100L)).thenReturn(restaurant);
        when(grantRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PartnerVenueGrant created = service.grantVenue(7L, 100L, "venue-55", null, null);

        assertThat(created.getCapabilities()).isEmpty();
        assertThat(created.allows(PartnerCapability.MENU_WRITE)).isFalse();
        // And orders keep printing where they always did. Switching a kitchen
        // over is its own decision, never a side effect of mapping a venue.
        assertThat(created.isPushOrders()).isFalse();
    }

    @Test
    @DisplayName("order push cannot be switched on while every order says PREPAID")
    void pushRefusedWhilePaymentModeIsAConstant() {
        // To a partner's till PREPAID is not decoration: the ticket prints as a
        // paid order and a counter hand gives a bag to a courier who owes
        // nothing. Restos asked us not to flip this early and said they cannot
        // detect it from their side, so it is a switch rather than a promise
        // someone has to remember.
        org.springframework.test.util.ReflectionTestUtils.setField(
                service, "paymentModeAuthoritative", false);
        Partner partner = Partner.builder().id(7L).code("RESTOS").active(true).build();
        when(partnerRepository.findById(7L)).thenReturn(Optional.of(partner));
        when(restaurantService.getRestaurantEntityById(100L)).thenReturn(restaurant);

        assertThatThrownBy(() -> service.grantVenue(7L, 100L, "venue-55",
                Set.of(PartnerCapability.MENU_WRITE), true))
                .isInstanceOf(com.fooddelivery.common.exception.BusinessException.class)
                .hasMessageContaining("paymentMode");
    }

    @Test
    @DisplayName("menu access is still grantable while order push is barred")
    void menuAccessUnaffectedByThePaymentModeGate() {
        // Menu writes and status reports are safe; it is only order push that
        // carries the risk, and blocking the whole grant would stop staging
        // getting started at all.
        org.springframework.test.util.ReflectionTestUtils.setField(
                service, "paymentModeAuthoritative", false);
        Partner partner = Partner.builder().id(7L).code("RESTOS").active(true).build();
        when(partnerRepository.findById(7L)).thenReturn(Optional.of(partner));
        when(restaurantService.getRestaurantEntityById(100L)).thenReturn(restaurant);
        when(grantRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PartnerVenueGrant granted = service.grantVenue(7L, 100L, "venue-55",
                Set.of(PartnerCapability.MENU_WRITE, PartnerCapability.ORDER_STATUS_WRITE), false);

        assertThat(granted.getCapabilities()).hasSize(2);
        assertThat(granted.isPushOrders()).isFalse();
    }

    @Test
    @DisplayName("omitting pushOrders leaves a switched-on venue switched on")
    void omittingPushOrdersDoesNotSwitchAKitchenBack() {
        Partner partner = Partner.builder().id(7L).code("RESTOS").active(true).build();
        when(partnerRepository.findById(7L)).thenReturn(Optional.of(partner));
        when(restaurantService.getRestaurantEntityById(100L)).thenReturn(restaurant);
        PartnerVenueGrant existing = grant(PartnerCapability.MENU_WRITE);
        existing.setPushOrders(true);
        when(grantRepository.findByPartnerIdAndRestaurantId(7L, 100L)).thenReturn(Optional.of(existing));
        when(grantRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Editing capabilities must not silently stop a restaurant's orders
        // reaching the till they are cooked from.
        PartnerVenueGrant updated = service.grantVenue(7L, 100L, "venue-55",
                Set.of(PartnerCapability.ORDER_STATUS_WRITE), null);

        assertThat(updated.isPushOrders()).isTrue();
    }
}
