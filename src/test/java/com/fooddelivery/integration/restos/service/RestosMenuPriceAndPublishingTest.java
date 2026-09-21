package com.fooddelivery.integration.restos.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.integration.restos.client.RestosMenuClient;
import com.fooddelivery.integration.restos.config.RestosProperties;
import com.fooddelivery.integration.restos.config.UrlSafetyValidator;
import com.fooddelivery.integration.restos.dto.MenuImportRequest;
import com.fooddelivery.integration.restos.dto.MenuImportResult;
import com.fooddelivery.integration.restos.dto.RestosApiResponse;
import com.fooddelivery.integration.restos.dto.RestosCategory;
import com.fooddelivery.integration.restos.dto.RestosProduct;
import com.fooddelivery.restaurant.entity.MenuCategory;
import com.fooddelivery.restaurant.entity.MenuItem;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.repository.MenuCategoryRepository;
import com.fooddelivery.restaurant.repository.MenuItemRepository;
import com.fooddelivery.restaurant.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * What we charge for an imported dish, and which dishes we are allowed to sell.
 *
 * <p>Both rules were settled by a real payload rather than by reading the code:
 * Qahvoon runs an older Restos build whose public menu sends no channel price
 * on any product and serves unfinished ones alongside finished ones. The
 * fixture here is that menu, captured verbatim from their server — so the next
 * partner whose build differs fails a test here rather than a reconciliation
 * later.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Restos import: price and publishing")
class RestosMenuPriceAndPublishingTest {

    private static final String SOURCE = "RESTOS";
    private static final String BASE_URL = "https://pos.example.com";

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

    // --- fixtures ----------------------------------------------------------

    private RestosCategory category(RestosProduct... products) {
        return RestosCategory.builder().id(3L).name("Cofe")
                .products(new ArrayList<>(Arrays.asList(products))).build();
    }

    private MenuImportResult sync(List<RestosCategory> upstream) {
        when(menuClient.fetchFullMenu(anyString(), anyLong(), any())).thenReturn(upstream);
        return service.importFullMenu(1L, MenuImportRequest.builder()
                .baseUrl(BASE_URL).externalRestaurantId(1L).overwriteExisting(true).build());
    }

    private List<MenuItem> savedItems() {
        ArgumentCaptor<MenuItem> captor = ArgumentCaptor.forClass(MenuItem.class);
        verify(menuItemRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    // --- price -------------------------------------------------------------

    @Nested
    @DisplayName("Price")
    class Price {

        @Test
        @DisplayName("a venue's price is charged exactly as sent")
        void noInventedMarkup() {
            // Qahvoon's build sends no channel price on any product. We used to
            // fill the gap with price x 1.10, and getEffectivePrice() is what
            // the customer pays — so a 15 000 coffee sold at 16 500 under a
            // markup nobody agreed and nothing displayed.
            sync(List.of(category(RestosProduct.builder()
                    .id(88L).name("Americano").price(new BigDecimal("15000"))
                    .priceWithMargin(null).status("LIVE").inStock(true).build())));

            MenuItem saved = savedItems().get(0);
            assertThat(saved.getPrice()).isEqualByComparingTo("15000");
            assertThat(saved.getEffectivePrice()).isEqualByComparingTo("15000");
        }

        @Test
        @DisplayName("a channel price, when they publish one, is used instead")
        void channelPriceWins() {
            // The markup a venue sets FOR this channel is theirs to set, and is
            // the number they intend us to charge.
            sync(List.of(category(RestosProduct.builder()
                    .id(88L).name("Americano").price(new BigDecimal("15000"))
                    .priceWithMargin(new BigDecimal("17250")).status("LIVE").inStock(true).build())));

            MenuItem saved = savedItems().get(0);
            assertThat(saved.getPrice()).isEqualByComparingTo("15000");
            assertThat(saved.getEffectivePrice()).isEqualByComparingTo("17250");
        }

        @Test
        @DisplayName("a margin invented by the old behaviour is cleared by the next sync")
        void staleMarkupIsCleared() {
            // Otherwise every dish imported before the fix keeps charging the
            // 10% forever, because nothing would ever overwrite the field.
            MenuItem stale = MenuItem.builder().id(5L).name("Americano")
                    .price(new BigDecimal("15000"))
                    .priceWithMargin(new BigDecimal("16500"))
                    .externalId(88L).externalSource(SOURCE).active(true).build();
            when(menuItemRepository.findByCategoryIdAndExternalSourceAndExternalId(any(), anyString(), any()))
                    .thenReturn(Optional.of(stale));

            sync(List.of(category(RestosProduct.builder()
                    .id(88L).name("Americano").price(new BigDecimal("15000"))
                    .priceWithMargin(null).status("LIVE").inStock(true).build())));

            assertThat(stale.getEffectivePrice()).isEqualByComparingTo("15000");
        }
    }

    // --- publishing --------------------------------------------------------

    @Nested
    @DisplayName("Publishing")
    class Publishing {

        @Test
        @DisplayName("a draft dish is not put on sale")
        void draftIsSkipped() {
            MenuImportResult result = sync(List.of(category(
                    RestosProduct.builder().id(88L).name("Ice Latte")
                            .price(new BigDecimal("32000")).status("LIVE").inStock(true).build(),
                    RestosProduct.builder().id(89L).name("San Sebastyan")
                            .price(new BigDecimal("45000")).status("DRAFT").inStock(true).build())));

            assertThat(result.getProductsCreated()).isEqualTo(1);
            assertThat(result.getProductsSkipped()).isEqualTo(1);
            assertThat(savedItems()).extracting(MenuItem::getName).containsExactly("Ice Latte");
            assertThat(result.getWarnings()).anyMatch(w -> w.contains("San Sebastyan") && w.contains("DRAFT"));
        }

        @Test
        @DisplayName("a retired dish is still skipped")
        void archivedIsStillSkipped() {
            MenuImportResult result = sync(List.of(category(
                    RestosProduct.builder().id(90L).name("Old Special")
                            .price(new BigDecimal("20000")).status("ARCHIVED").inStock(true).build())));

            assertThat(result.getProductsCreated()).isZero();
            assertThat(result.getProductsSkipped()).isEqualTo(1);
        }

        @Test
        @DisplayName("a partner that tracks no status at all still gets its menu")
        void missingStatusMeansPublished() {
            // The opposite failure: treating a blank status as unfinished would
            // import an empty menu from any partner that does not have the
            // concept.
            MenuImportResult result = sync(List.of(category(
                    RestosProduct.builder().id(91L).name("Plov")
                            .price(new BigDecimal("30000")).status(null).inStock(true).build())));

            assertThat(result.getProductsCreated()).isEqualTo(1);
        }

        @Test
        @DisplayName("a dish that goes back to draft is retired here, not left on sale")
        void unpublishedUpstreamIsDeactivatedHere() {
            // Not recorded as seen, so it reaches the deactivation pass. Sold
            // out is a flag; unpublished is a removal.
            List<MenuItem> live = new ArrayList<>();
            for (long id = 80; id <= 89; id++) {
                live.add(MenuItem.builder().id(id).name("Dish " + id).price(new BigDecimal("10000"))
                        .externalId(id).externalSource(SOURCE).active(true).inStock(true).build());
            }
            when(menuItemRepository.findActiveExternalItems(anyLong(), anyString())).thenReturn(live);

            List<RestosProduct> upstream = new ArrayList<>();
            for (long id = 80; id <= 88; id++) {
                upstream.add(RestosProduct.builder().id(id).name("Dish " + id)
                        .price(new BigDecimal("10000")).status("LIVE").inStock(true).build());
            }
            upstream.add(RestosProduct.builder().id(89L).name("Dish 89")
                    .price(new BigDecimal("10000")).status("DRAFT").inStock(true).build());

            sync(List.of(category(upstream.toArray(new RestosProduct[0]))));

            MenuItem unpublished = live.get(live.size() - 1);
            assertThat(unpublished.getActive()).isFalse();
            assertThat(unpublished.getInStock()).isFalse();
            assertThat(live.get(0).getActive()).isTrue();
        }
    }

    // --- the real payload --------------------------------------------------

    @Nested
    @DisplayName("Qahvoon's live menu, captured from their server")
    class RealPayload {

        private List<RestosCategory> qahvoonMenu() throws Exception {
            try (InputStream in = getClass().getResourceAsStream("/restos/qahvoon-public-menu.json")) {
                assertThat(in).as("fixture on the classpath").isNotNull();
                RestosApiResponse<List<RestosCategory>> response = new ObjectMapper()
                        .readValue(in, new TypeReference<>() {});
                assertThat(response.isSuccess()).isTrue();
                return response.getData();
            }
        }

        @Test
        @DisplayName("parses as their envelope: 10 categories, 71 products")
        void fixtureParses() throws Exception {
            List<RestosCategory> menu = qahvoonMenu();

            assertThat(menu).hasSize(10);
            assertThat(menu.stream().mapToInt(c -> c.getProducts().size()).sum()).isEqualTo(71);
        }

        @Test
        @DisplayName("57 published dishes are imported and 14 drafts are not")
        void draftsStayOff() throws Exception {
            MenuImportResult result = sync(qahvoonMenu());

            assertThat(result.getProductsCreated()).isEqualTo(57);
            assertThat(result.getProductsSkipped()).isEqualTo(14);
            assertThat(savedItems()).extracting(MenuItem::getName)
                    .doesNotContain("San Sebastyan", "Redbull Moxito", "Muzqaymoq");
        }

        @Test
        @DisplayName("not one of the 71 prices is changed on the way in")
        void everyPriceIsTheirs() throws Exception {
            List<RestosCategory> menu = qahvoonMenu();
            Map<Long, BigDecimal> theirs = menu.stream()
                    .flatMap(c -> c.getProducts().stream())
                    .collect(Collectors.toMap(RestosProduct::getId, RestosProduct::getPrice));

            sync(menu);

            // Every product in this payload has priceWithMargin: null, which is
            // exactly the case the old code filled with a 10% markup. 57 dishes,
            // every one of them sold above the venue's own price.
            assertThat(savedItems()).isNotEmpty().allSatisfy(item ->
                    assertThat(item.getEffectivePrice())
                            .as("charged price of '%s'", item.getName())
                            .isEqualByComparingTo(theirs.get(item.getExternalId())));
        }
    }
}
