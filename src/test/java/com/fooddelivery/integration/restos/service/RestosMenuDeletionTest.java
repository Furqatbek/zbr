package com.fooddelivery.integration.restos.service;

import com.fooddelivery.integration.restos.client.RestosMenuClient;
import com.fooddelivery.integration.restos.config.RestosProperties;
import com.fooddelivery.integration.restos.config.UrlSafetyValidator;
import com.fooddelivery.integration.restos.dto.MenuImportRequest;
import com.fooddelivery.integration.restos.dto.MenuImportResult;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * What a sync does with dishes that are no longer on the upstream menu.
 *
 * <p>The import only ever created and updated, so an item deleted in Restos
 * stayed orderable here forever — a customer could buy a dish the kitchen had
 * retired. Adding deletion is easy; adding it safely is the whole job, because
 * "deactivate everything I did not see" is indistinguishable from "deactivate
 * the entire menu" when the upstream answer is incomplete. These tests are
 * mostly about the cases where deletion must NOT happen.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Restos menu deletion")
class RestosMenuDeletionTest {

    private static final String SOURCE = "RESTOS";
    private static final String BASE_URL = "https://pos.example.com";

    @Mock private RestosMenuClient menuClient;
    @Mock private RestaurantService restaurantService;
    @Mock private MenuCategoryRepository categoryRepository;
    @Mock private MenuItemRepository menuItemRepository;
    @Mock private UrlSafetyValidator urlSafetyValidator;

    private final RestosProperties properties = new RestosProperties();
    private RestosMenuImportService service;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        service = new RestosMenuImportService(menuClient, restaurantService, categoryRepository,
                menuItemRepository, properties, urlSafetyValidator);

        restaurant = Restaurant.builder().id(1L).name("Osh Markazi").build();
        when(restaurantService.getRestaurantEntityById(1L)).thenReturn(restaurant);

        // Categories and items resolve to themselves on save, and nothing
        // pre-exists unless a test says so.
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

    private RestosProduct product(Long id, String name) {
        return RestosProduct.builder().id(id).name(name).price(new BigDecimal("15000")).available(true).build();
    }

    private RestosCategory category(Long id, String name, RestosProduct... products) {
        return RestosCategory.builder().id(id).name(name)
                .products(new ArrayList<>(Arrays.asList(products))).build();
    }

    /** An item already in our catalogue, imported from Restos earlier. */
    private MenuItem liveItem(Long id, Long externalId, String name) {
        return MenuItem.builder().id(id).name(name).price(new BigDecimal("15000"))
                .externalId(externalId).externalSource(SOURCE).active(true).inStock(true).build();
    }

    private MenuImportResult sync(List<RestosCategory> upstream) {
        when(menuClient.fetchFullMenu(anyString(), anyLong(), any())).thenReturn(upstream);
        return service.importFullMenu(1L, MenuImportRequest.builder()
                .baseUrl(BASE_URL).externalRestaurantId(99L).overwriteExisting(true).build());
    }

    // --- tests -------------------------------------------------------------

    @Nested
    @DisplayName("retires what is gone")
    class Retires {

        @Test
        @DisplayName("an item missing from the snapshot is deactivated and taken out of stock")
        void vanishedItemDeactivated() {
            MenuItem gone = liveItem(10L, 500L, "Retired plov");
            when(menuItemRepository.findActiveExternalItems(1L, SOURCE)).thenReturn(List.of(gone));

            // Upstream still lists product 501, but not 500.
            MenuImportResult result = sync(List.of(category(1L, "Main", product(501L, "Lagman"))));

            assertThat(gone.getActive()).isFalse();
            // active=false hides it from the menu; inStock=false is what any
            // path holding the item directly reads.
            assertThat(gone.getInStock()).isFalse();
            assertThat(result.getProductsDeactivated()).isEqualTo(1);
        }

        @Test
        @DisplayName("an item still on the upstream menu is left alone")
        void survivingItemUntouched() {
            MenuItem alive = liveItem(10L, 500L, "Plov");
            when(menuItemRepository.findActiveExternalItems(1L, SOURCE)).thenReturn(List.of(alive));

            MenuImportResult result = sync(List.of(category(1L, "Main", product(500L, "Plov"))));

            assertThat(alive.getActive()).isTrue();
            assertThat(result.getProductsDeactivated()).isZero();
        }

        @Test
        @DisplayName("a product archived upstream is retired, not merely skipped")
        void archivedProductRetired() {
            MenuItem archived = liveItem(10L, 500L, "Seasonal dish");
            when(menuItemRepository.findActiveExternalItems(1L, SOURCE)).thenReturn(List.of(archived));

            RestosProduct upstream = product(500L, "Seasonal dish");
            upstream.setStatus("ARCHIVED");

            // Previously this was skipped with a warning and stayed live here
            // indefinitely — archived upstream but on sale with us.
            MenuImportResult result = sync(List.of(category(1L, "Main", upstream)));

            assertThat(archived.getActive()).isFalse();
            assertThat(result.getProductsDeactivated()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("refuses to retire when the snapshot cannot be trusted")
    class Refuses {

        @Test
        @DisplayName("nothing is deactivated when a category failed to import")
        void incompleteSnapshotBlocksDeletion() {
            MenuItem item = liveItem(10L, 500L, "Plov");
            when(menuItemRepository.findActiveExternalItems(1L, SOURCE)).thenReturn(List.of(item));
            when(categoryRepository.save(any(MenuCategory.class)))
                    .thenThrow(new RuntimeException("database went away"));

            MenuImportResult result = sync(List.of(category(1L, "Main", product(501L, "Lagman"))));

            // 500 was not in the snapshot — but neither was anything else, and
            // the reason is our own failure, not a deletion upstream.
            assertThat(item.getActive()).isTrue();
            assertThat(result.getProductsDeactivated()).isZero();
            assertThat(result.getWarnings()).anyMatch(w -> w.contains("incomplete"));
        }

        @Test
        @DisplayName("nothing is deactivated when a category arrives with no product list")
        void nullProductListBlocksDeletion() {
            MenuItem item = liveItem(10L, 500L, "Plov");
            when(menuItemRepository.findActiveExternalItems(1L, SOURCE)).thenReturn(List.of(item));

            RestosCategory withoutProducts = RestosCategory.builder().id(1L).name("Main").products(null).build();

            assertThat(sync(List.of(withoutProducts)).getProductsDeactivated()).isZero();
            assertThat(item.getActive()).isTrue();
        }

        @Test
        @DisplayName("a mass disappearance is reported, not applied")
        void massDeletionRefused() {
            // 20 live dishes, upstream suddenly reports one. That is an outage
            // on their side far more often than a restaurant deleting its menu,
            // and the two mistakes do not cost the same: a stale dish is a
            // nuisance, an empty menu is a restaurant taking no orders.
            List<MenuItem> live = IntStream.range(0, 20)
                    .mapToObj(i -> liveItem((long) i, 500L + i, "Dish " + i))
                    .toList();
            when(menuItemRepository.findActiveExternalItems(1L, SOURCE)).thenReturn(live);

            MenuImportResult result = sync(List.of(category(1L, "Main", product(500L, "Dish 0"))));

            assertThat(live).allMatch(MenuItem::getActive);
            assertThat(result.getProductsDeactivated()).isZero();
            assertThat(result.getWarnings()).anyMatch(w -> w.contains("Refused to deactivate"));
        }

        @Test
        @DisplayName("a small menu can still lose a few dishes")
        void smallMenuNotBlockedByTheRatio() {
            // 4 of 6 gone is over the 30% ratio, but well within the floor —
            // otherwise a short menu could never drop anything.
            List<MenuItem> live = IntStream.range(0, 6)
                    .mapToObj(i -> liveItem((long) i, 500L + i, "Dish " + i))
                    .toList();
            when(menuItemRepository.findActiveExternalItems(1L, SOURCE)).thenReturn(live);

            MenuImportResult result = sync(List.of(category(1L, "Main",
                    product(500L, "Dish 0"), product(501L, "Dish 1"))));

            assertThat(result.getProductsDeactivated()).isEqualTo(4);
        }

        @Test
        @DisplayName("an import never retires anything — only a sync does")
        void importModeNeverDeletes() {
            MenuItem item = liveItem(10L, 500L, "Plov");
            when(menuItemRepository.findActiveExternalItems(1L, SOURCE)).thenReturn(List.of(item));
            when(menuClient.fetchFullMenu(anyString(), anyLong(), any()))
                    .thenReturn(List.of(category(1L, "Main", product(501L, "Lagman"))));

            // overwriteExisting = false is the "import, don't touch what's here"
            // mode. It leaves existing products alone, so it has no business
            // retiring them either.
            MenuImportResult result = service.importFullMenu(1L, MenuImportRequest.builder()
                    .baseUrl(BASE_URL).externalRestaurantId(99L).overwriteExisting(false).build());

            assertThat(item.getActive()).isTrue();
            assertThat(result.getProductsDeactivated()).isZero();
        }
    }

    @Nested
    @DisplayName("products Restos sent without an id")
    class Unkeyed {

        /**
         * A null id is not a lookup miss. Spring Data renders it as
         * {@code external_id IS NULL}, which matches the first unkeyed row in
         * the category — so before the fix every id-less product overwrote the
         * same row, and two dishes became one whose identity depended on the
         * order Restos happened to send them in.
         */
        private MenuItem theOneUnkeyedRow() {
            MenuItem collided = MenuItem.builder().id(77L).name("First one imported")
                    .price(new BigDecimal("1000")).externalSource(SOURCE).active(true).build();
            when(menuItemRepository.findByCategoryIdAndExternalSourceAndExternalId(any(), anyString(), any()))
                    .thenReturn(Optional.of(collided));
            return collided;
        }

        @Test
        @DisplayName("is skipped rather than written over an unrelated dish")
        void unkeyedProductSkipped() {
            MenuItem collided = theOneUnkeyedRow();

            RestosProduct unkeyed = product(null, "Soup of the day");
            MenuImportResult result = sync(List.of(category(1L, "Main", unkeyed)));

            // Before the fix this row took the incoming product's name.
            assertThat(collided.getName()).isEqualTo("First one imported");
            assertThat(result.getProductsSkipped()).isEqualTo(1);
            assertThat(result.getWarnings()).anyMatch(w -> w.contains("Soup of the day")
                    && w.contains("no id"));
        }

        @Test
        @DisplayName("two of them do not collapse into one row")
        void twoUnkeyedProductsDoNotMerge() {
            MenuItem collided = theOneUnkeyedRow();

            sync(List.of(category(1L, "Main", product(null, "Soup"), product(null, "Salad"))));

            // The original bug in one line: both landed on the same row, so the
            // second silently replaced the first.
            assertThat(collided.getName()).isEqualTo("First one imported");
        }

        @Test
        @DisplayName("a category without an id is skipped, and its products with it")
        void unkeyedCategorySkipped() {
            RestosCategory unkeyed = RestosCategory.builder().id(null).name("Specials")
                    .products(new ArrayList<>(List.of(product(500L, "Plov")))).build();

            MenuImportResult result = sync(List.of(unkeyed));

            assertThat(result.getCategoriesCreated()).isZero();
            assertThat(result.getProductsCreated()).isZero();
            assertThat(result.getWarnings()).anyMatch(w -> w.contains("Specials") && w.contains("no id"));
        }

        @Test
        @DisplayName("an unkeyed product stops the sync from retiring anything")
        void unkeyedProductBlocksDeletion() {
            MenuItem live = liveItem(10L, 500L, "Plov");
            when(menuItemRepository.findActiveExternalItems(1L, SOURCE)).thenReturn(List.of(live));

            // We could not import it, so we do not know what it was — and an
            // item we failed to recognise is not evidence that anything was
            // deleted upstream.
            MenuImportResult result = sync(List.of(category(1L, "Main", product(null, "Mystery dish"))));

            assertThat(live.getActive()).isTrue();
            assertThat(result.getProductsDeactivated()).isZero();
        }

        @Test
        @DisplayName("rows already stranded by the old behaviour are reported")
        void strandedRowsReported() {
            when(menuItemRepository.countUnkeyedExternalItems(1L, SOURCE)).thenReturn(3L);

            MenuImportResult result = sync(List.of(category(1L, "Main", product(500L, "Plov"))));

            // No sync can match or retire these any more, so the only useful
            // thing left is to say they exist.
            assertThat(result.getWarnings()).anyMatch(w -> w.contains("3 item(s)") && w.contains("by hand"));
        }
    }

    @Nested
    @DisplayName("categories")
    class Categories {

        private MenuCategory liveCategory(Long id, Long externalId, String name) {
            return MenuCategory.builder().id(id).name(name).restaurant(restaurant)
                    .externalId(externalId).externalSource(SOURCE).active(true).build();
        }

        @Test
        @DisplayName("a category gone from Restos is deactivated")
        void vanishedCategoryDeactivated() {
            MenuCategory gone = liveCategory(70L, 9L, "Winter menu");
            when(categoryRepository.findByRestaurantIdAndExternalSource(1L, SOURCE)).thenReturn(List.of(gone));

            MenuImportResult result = sync(List.of(category(1L, "Main", product(500L, "Plov"))));

            assertThat(gone.getActive()).isFalse();
            assertThat(result.getCategoriesDeactivated()).isEqualTo(1);
        }

        @Test
        @DisplayName("a category still holding the restaurant's own dishes is left active")
        void categoryWithOwnItemsKept() {
            MenuCategory gone = liveCategory(70L, 9L, "Winter menu");
            when(categoryRepository.findByRestaurantIdAndExternalSource(1L, SOURCE)).thenReturn(List.of(gone));

            // Added by hand in our panel: no external id, so Restos never knew
            // about it and deleting its category would hide it without warning.
            MenuItem ours = MenuItem.builder().id(88L).name("House special")
                    .price(new BigDecimal("20000")).active(true).build();
            when(menuItemRepository.findByCategoryIdAndActiveOrderBySortOrderAsc(70L, true))
                    .thenReturn(List.of(ours));

            MenuImportResult result = sync(List.of(category(1L, "Main", product(500L, "Plov"))));

            assertThat(gone.getActive()).isTrue();
            assertThat(result.getCategoriesDeactivated()).isZero();
            assertThat(result.getWarnings()).anyMatch(w -> w.contains("left active"));
        }
    }
}
