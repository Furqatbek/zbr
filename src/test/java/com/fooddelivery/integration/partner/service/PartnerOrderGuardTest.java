package com.fooddelivery.integration.partner.service;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.integration.partner.entity.Partner;
import com.fooddelivery.integration.partner.entity.PartnerVenueGrant;
import com.fooddelivery.restaurant.entity.MenuItem;
import com.fooddelivery.restaurant.entity.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Refusing a basket the partner's kitchen could not read.
 *
 * <p>The point is where the refusal happens. A partner rejects the whole order
 * if any line names a product they do not have, and without this check that
 * rejection lands after checkout — on a customer who has paid for food nobody
 * will make.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Partner order guard")
class PartnerOrderGuardTest {

    @Mock private PartnerOrderPushService pushService;

    private PartnerOrderGuard guard;

    @BeforeEach
    void setUp() {
        guard = new PartnerOrderGuard(pushService);
        when(pushService.pushableGrant(anyLong())).thenReturn(Optional.empty());
    }

    private void venueOn(String partnerCode) {
        when(pushService.pushableGrant(100L)).thenReturn(Optional.of(PartnerVenueGrant.builder()
                .partner(Partner.builder().id(7L).code(partnerCode).active(true).build())
                .restaurant(Restaurant.builder().id(100L).build())
                .externalVenueId("55").pushOrders(true)
                .build()));
    }

    private MenuItem item(String name, Long externalId, String source) {
        return MenuItem.builder().id(1L).name(name)
                .externalId(externalId).externalSource(source).build();
    }

    @Test
    @DisplayName("an ordinary restaurant is not checked at all")
    void nonPartnerVenueIsUnaffected() {
        // Most restaurants. A hand-made menu is exactly what they have, and
        // checking it would refuse every order on the platform.
        assertThatCode(() -> guard.checkOrderable(100L,
                List.of(item("House special", null, null))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a basket of the partner's own items is allowed")
    void partnerItemsAllowed() {
        venueOn("RESTOS");

        assertThatCode(() -> guard.checkOrderable(100L, List.of(
                item("Plov", 4417L, "RESTOS"),
                item("Lagman", 4418L, "RESTOS"))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("an item the partner has never heard of is refused, by name")
    void handMadeItemRefused() {
        // THE case. Our vendor app lets a restaurant add items without going
        // through the till, so this happens the first time someone adds a
        // lunch special.
        venueOn("RESTOS");

        assertThatThrownBy(() -> guard.checkOrderable(100L, List.of(
                item("Plov", 4417L, "RESTOS"),
                item("Lunch special", null, null))))
                .isInstanceOf(BusinessException.class)
                // Named, so the customer knows what to remove rather than
                // being told their basket is simply wrong.
                .hasMessageContaining("Lunch special");
    }

    @Test
    @DisplayName("an item from a different partner is refused too")
    void otherPartnersItemRefused() {
        venueOn("RESTOS");

        assertThatThrownBy(() -> guard.checkOrderable(100L,
                List.of(item("Imported elsewhere", 9L, "OTHER_POS"))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("several bad items are listed together, not one per attempt")
    void allOffendingItemsNamedAtOnce() {
        // One round trip per bad item turns removing three dishes into three
        // failed checkouts.
        venueOn("RESTOS");

        assertThatThrownBy(() -> guard.checkOrderable(100L, List.of(
                item("Lunch special", null, null),
                item("Soup of the day", null, null))))
                .hasMessageContaining("Lunch special")
                .hasMessageContaining("Soup of the day");
    }

    @Test
    @DisplayName("the same dish twice is named once")
    void duplicatesNamedOnce() {
        venueOn("RESTOS");

        assertThatThrownBy(() -> guard.checkOrderable(100L, List.of(
                item("Lunch special", null, null),
                item("Lunch special", null, null))))
                .hasMessageNotContaining("Lunch special, Lunch special");
    }
}
