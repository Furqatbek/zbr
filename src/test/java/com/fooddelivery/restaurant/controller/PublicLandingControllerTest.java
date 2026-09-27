package com.fooddelivery.restaurant.controller;

import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.restaurant.dto.MenuCategoryDto;
import com.fooddelivery.restaurant.dto.RestaurantDto;
import com.fooddelivery.restaurant.dto.RestaurantLandingDto;
import com.fooddelivery.restaurant.service.MenuService;
import com.fooddelivery.restaurant.service.RestaurantEtaEnricher;
import com.fooddelivery.restaurant.service.RestaurantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Resolving whatever is printed on a poster.
 *
 * <p>A QR code is printed once and lives on a takeaway bag for months. It has
 * to keep working after the venue is renamed, and it has to work whether
 * marketing put a slug or an id on it — which is why this accepts both and why
 * "5" being a slug is a case worth having a test for rather than a surprise on
 * a thousand stickers.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("QR landing endpoint")
class PublicLandingControllerTest {

    @Mock
    private RestaurantService restaurantService;

    @Mock
    private MenuService menuService;

    @Mock
    private RestaurantEtaEnricher etaEnricher;

    @InjectMocks
    private PublicLandingController controller;

    private RestaurantDto qahvoon() {
        return RestaurantDto.builder().id(3L).name("Qahvoon").slug("qahvoon").build();
    }

    private void passThroughEnricher() {
        lenient().when(etaEnricher.forRequest(any(RestaurantDto.class), any(), any(), anyString()))
                .thenAnswer(call -> call.getArgument(0));
    }

    private RestaurantLandingDto call(String slugOrId) {
        return controller.landing(slugOrId, null, null, null).getBody().getData();
    }

    @Test
    @DisplayName("a slug resolves to the restaurant and its menu, in one response")
    void slugResolves() {
        passThroughEnricher();
        when(restaurantService.getRestaurantBySlug("qahvoon")).thenReturn(qahvoon());
        when(menuService.getFullMenu(3L)).thenReturn(List.of(
                MenuCategoryDto.builder().id(11L).name("Coffee").build()));

        RestaurantLandingDto body = call("qahvoon");

        assertThat(body.getRestaurant().getName()).isEqualTo("Qahvoon");
        assertThat(body.getMenu()).hasSize(1);
    }

    @Test
    @DisplayName("a numeric segment is tried as an id")
    void numericResolvesAsId() {
        passThroughEnricher();
        when(restaurantService.getRestaurantById(3L)).thenReturn(qahvoon());
        when(menuService.getFullMenu(3L)).thenReturn(List.of());

        assertThat(call("3").getRestaurant().getId()).isEqualTo(3L);
        verify(restaurantService, never()).getRestaurantBySlug(anyString());
    }

    @Test
    @DisplayName("a numeric slug still works when no restaurant has that id")
    void numericFallsBackToSlug() {
        // A venue slugged "5" is unlikely and entirely possible. Printed on a
        // poster, it has to resolve — the alternative is a 404 discovered after
        // the stickers are on the bags.
        passThroughEnricher();
        when(restaurantService.getRestaurantById(5L))
                .thenThrow(new ResourceNotFoundException("Restaurant", "id", 5L));
        when(restaurantService.getRestaurantBySlug("5")).thenReturn(qahvoon());
        when(menuService.getFullMenu(3L)).thenReturn(List.of());

        assertThat(call("5").getRestaurant().getName()).isEqualTo("Qahvoon");
    }

    @Test
    @DisplayName("an unknown code is a 404, not an empty page")
    void unknownIsNotFound() {
        when(restaurantService.getRestaurantBySlug("nosuchplace"))
                .thenThrow(new ResourceNotFoundException("Restaurant", "slug", "nosuchplace"));

        assertThatThrownBy(() -> call("nosuchplace"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(menuService, never()).getFullMenu(anyLong());
    }

    @Test
    @DisplayName("coordinates reach the same enrichment the app's cards use")
    void coordinatesAreEnriched() {
        // Otherwise the web page and the app quote different arrival times for
        // the same venue, and the customer believes the one that is worse.
        when(restaurantService.getRestaurantBySlug("qahvoon")).thenReturn(qahvoon());
        when(menuService.getFullMenu(3L)).thenReturn(List.of());
        when(etaEnricher.forRequest(any(RestaurantDto.class), any(), any(), any()))
                .thenAnswer(c -> qahvoon().toBuilder().distanceKm(2.4).build());

        RestaurantLandingDto body = controller.landing(
                "qahvoon", new BigDecimal("41.3113"), new BigDecimal("61.0867"), "uz").getBody().getData();

        assertThat(body.getRestaurant().getDistanceKm()).isEqualTo(2.4);
    }

    @Test
    @DisplayName("the answer may be cached briefly")
    void cacheHeaderIsSet() {
        // A poster is scanned in bursts — a table of six, a queue at a till.
        passThroughEnricher();
        when(restaurantService.getRestaurantBySlug("qahvoon")).thenReturn(qahvoon());
        when(menuService.getFullMenu(3L)).thenReturn(List.of());

        assertThat(controller.landing("qahvoon", null, null, null)
                .getHeaders().getCacheControl()).contains("max-age=60");
    }
}
