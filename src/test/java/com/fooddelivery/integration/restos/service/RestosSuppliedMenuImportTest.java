package com.fooddelivery.integration.restos.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.integration.restos.client.RestosMenuClient;
import com.fooddelivery.integration.restos.config.RestosProperties;
import com.fooddelivery.integration.restos.config.UrlSafetyValidator;
import com.fooddelivery.integration.restos.dto.MenuImportResult;
import com.fooddelivery.integration.restos.dto.RestosApiResponse;
import com.fooddelivery.integration.restos.dto.RestosCategory;
import com.fooddelivery.integration.restos.dto.RestosProduct;
import com.fooddelivery.integration.restos.dto.SuppliedMenuImportRequest;
import com.fooddelivery.restaurant.entity.MenuCategory;
import com.fooddelivery.restaurant.entity.MenuItem;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.repository.MenuCategoryRepository;
import com.fooddelivery.restaurant.repository.MenuItemRepository;
import com.fooddelivery.restaurant.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Importing a menu for a venue we cannot reach.
 *
 * <p>Qahvoon's POS sits on a network that drops our server's packets in both
 * directions — no firewall on either machine, and not ours to fix. Waiting for
 * two providers to agree before a single coffee can be sold is the wrong trade,
 * so a menu someone else can fetch may be handed to us directly.
 *
 * <p>The risk in an import that bypasses the network is that it also bypasses
 * everything the network import learned. Most of these tests exist to show it
 * does not.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Restos import from a supplied payload")
class RestosSuppliedMenuImportTest {

    private static final String SOURCE = "RESTOS";

    @Mock private RestosMenuClient menuClient;
    @Mock private RestaurantService restaurantService;
    @Mock private MenuCategoryRepository categoryRepository;
    @Mock private MenuItemRepository menuItemRepository;
    @Mock private UrlSafetyValidator urlSafetyValidator;
    @Mock private com.fooddelivery.integration.partner.service.PartnerOrderPushService partnerLookup;

    private final RestosProperties properties = new RestosProperties();
    private RestosMenuImportService service;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        service = new RestosMenuImportService(menuClient, restaurantService, categoryRepository,
                menuItemRepository, properties, urlSafetyValidator, partnerLookup);
        when(partnerLookup.pushableGrant(anyLong())).thenReturn(Optional.empty());

        restaurant = Restaurant.builder().id(1L).name("Qahvoon").build();
        when(restaurantService.getRestaurantEntityById(1L)).thenReturn(restaurant);

        when(categoryRepository.save(any(MenuCategory.class))).thenAnswer(i -> i.getArgument(0));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(i -> i.getArgument(0));
        when(categoryRepository.findByRestaurantIdAndExternalSourceAndExternalId(anyLong(), anyString(), any()))
                .thenReturn(Optional.empty());
        when(menuItemRepository.findByCategoryIdAndExternalSourceAndExternalId(any(), anyString(), any()))
                .thenReturn(Optional.empty());
        when(menuItemRepository.findByCategoryIdAndActiveOrderBySortOrderAsc(any(), anyBoolean()))
                .thenReturn(List.of());
        when(menuItemRepository.findActiveExternalItems(anyLong(), anyString())).thenReturn(List.of());
        when(categoryRepository.findByRestaurantIdAndExternalSource(anyLong(), anyString()))
                .thenReturn(List.of());
    }

    private RestosApiResponse<List<RestosCategory>> qahvoonResponse() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/restos/qahvoon-public-menu.json")) {
            return new ObjectMapper().readValue(in, new TypeReference<>() {});
        }
    }

    private MenuImportResult importPayload(SuppliedMenuImportRequest request) {
        return service.importSuppliedMenu(1L, request);
    }

    private SuppliedMenuImportRequest.SuppliedMenuImportRequestBuilder request() {
        return SuppliedMenuImportRequest.builder().externalRestaurantId(1L).overwriteExisting(true);
    }

    @Test
    @DisplayName("Qahvoon's menu imports without a single outbound call")
    void importsWithoutTheNetwork() throws Exception {
        MenuImportResult result = importPayload(request().payload(qahvoonResponse()).build());

        // The whole point: no fetch, so nothing to time out.
        verifyNoInteractions(menuClient);
        assertThat(result.getCategoriesCreated()).isEqualTo(10);
        assertThat(result.getProductsCreated()).isEqualTo(57);
    }

    @Test
    @DisplayName("the same rules apply: drafts stay off the menu")
    void publishingRulesStillApply() throws Exception {
        MenuImportResult result = importPayload(request().payload(qahvoonResponse()).build());

        // Not a second code path with its own opinions — the same one.
        assertThat(result.getProductsSkipped()).isEqualTo(14);
    }

    @Test
    @DisplayName("the array on its own is accepted too")
    void categoriesArrayIsAccepted() throws Exception {
        MenuImportResult result = importPayload(
                request().categories(qahvoonResponse().getData()).build());

        assertThat(result.getProductsCreated()).isEqualTo(57);
    }

    @Test
    @DisplayName("a payload that reports failure upstream is refused")
    void failedPayloadIsRefused() {
        RestosApiResponse<List<RestosCategory>> failed = RestosApiResponse.<List<RestosCategory>>builder()
                .success(false).message("Restaurant not found").data(null).build();

        // Importing the contents of an error response is how a venue's whole
        // catalogue gets replaced by nothing.
        assertThatThrownBy(() -> importPayload(request().payload(failed).build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Restaurant not found");
    }

    @Test
    @DisplayName("sending both forms is a mistake, not a preference")
    void bothFormsAreRefused() throws Exception {
        RestosApiResponse<List<RestosCategory>> response = qahvoonResponse();

        assertThatThrownBy(() -> importPayload(request()
                .payload(response).categories(response.getData()).build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not both");
    }

    @Test
    @DisplayName("an empty request says what is missing")
    void emptyRequestIsRefused() {
        assertThatThrownBy(() -> importPayload(request().build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No menu in the request");

        assertThatThrownBy(() -> importPayload(request().categories(new ArrayList<>()).build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No menu in the request");
    }

    @Test
    @DisplayName("a truncated paste cannot wipe the menu")
    void deactivationCeilingStillApplies() {
        // The failure mode this path adds: a payload copied from a terminal
        // that scrolled. Nine of ten dishes missing looks exactly like nine
        // dishes withdrawn, so the same ceiling has to hold here.
        List<MenuItem> live = new ArrayList<>();
        for (long id = 80; id <= 89; id++) {
            live.add(MenuItem.builder().id(id).name("Dish " + id).price(new BigDecimal("10000"))
                    .externalId(id).externalSource(SOURCE).active(true).inStock(true).build());
        }
        when(menuItemRepository.findActiveExternalItems(anyLong(), anyString())).thenReturn(live);

        MenuImportResult result = importPayload(request().categories(List.of(
                RestosCategory.builder().id(3L).name("Cofe").products(new ArrayList<>(List.of(
                        RestosProduct.builder().id(80L).name("Dish 80")
                                .price(new BigDecimal("10000")).status("LIVE").inStock(true).build())))
                        .build())).build());

        assertThat(result.getWarnings()).anyMatch(w -> w.contains("Refused to deactivate"));
        assertThat(live).allSatisfy(item -> assertThat(item.getActive()).isTrue());
    }

    @Test
    @DisplayName("the result says the catalogue came from a paste")
    void provenanceIsRecorded() throws Exception {
        MenuImportResult result = importPayload(request().payload(qahvoonResponse()).build());

        // Every later question — why is this price stale, why did nothing sync —
        // reads differently once you know it was never fetched.
        assertThat(result.getWarnings()).anyMatch(w -> w.contains("supplied payload"));
    }

    @Test
    @DisplayName("the venue is marked synced but not marked reachable")
    void noSourceUrlIsInvented() throws Exception {
        importPayload(request().payload(qahvoonResponse()).build());

        // externalSystemUrl is where the next fetch would go. A paste is not a
        // link, and writing one here would claim a connection that does not work.
        assertThat(restaurant.getExternalSystemUrl()).isNull();
        assertThat(restaurant.getLastMenuSyncAt()).isNotNull();
    }
}
