package com.fooddelivery.restaurant.service;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.restaurant.entity.MenuItem;
import com.fooddelivery.restaurant.repository.MenuItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Getting rid of a withdrawn menu item, and the ones that must stay.
 *
 * <p>Deleting an item only set {@code active = false}, and every listing — the
 * customer menu and the vendor's own item list — filters to active items. So a
 * deleted dish became invisible to the person who deleted it: nothing to
 * restore, nothing to press again, and a row that stayed forever. An imported
 * menu leaves dozens behind.
 *
 * <p>The reason it cannot simply be a hard delete: {@code order_items} has an
 * index on {@code menu_item_id} and NO foreign key, so the database allows the
 * row to go, and {@code OrderItem.menuItem} is {@code @ManyToOne(nullable =
 * false)}, so Hibernate throws the next time anyone opens that order. The
 * failure lands months later on a customer's order history, nowhere near the
 * delete.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Removing withdrawn menu items")
class MenuItemPurgeTest {

    private static final Long RESTAURANT = 3L;

    @Mock private MenuItemRepository itemRepository;
    @Mock private com.fooddelivery.restaurant.repository.MenuCategoryRepository categoryRepository;
    @Mock private RestaurantService restaurantService;
    @Mock private com.fooddelivery.restaurant.mapper.RestaurantMapper mapper;
    @Mock private com.fooddelivery.common.service.ImageStorageService imageStorageService;

    @InjectMocks
    private MenuService menuService;

    private MenuItem item(long id, String name, boolean active) {
        com.fooddelivery.restaurant.entity.Restaurant restaurant =
                com.fooddelivery.restaurant.entity.Restaurant.builder().id(RESTAURANT).build();
        com.fooddelivery.restaurant.entity.MenuCategory category =
                com.fooddelivery.restaurant.entity.MenuCategory.builder()
                        .id(11L).restaurant(restaurant).build();
        return MenuItem.builder().id(id).name(name).active(active).category(category).build();
    }

    @Test
    @DisplayName("an item nobody ever ordered is removed for good")
    void unorderedItemIsRemoved() {
        MenuItem junk = item(4417L, "Imported junk", false);
        when(itemRepository.findById(4417L)).thenReturn(Optional.of(junk));
        when(itemRepository.isReferencedByAnyOrder(4417L)).thenReturn(false);

        menuService.deleteItemPermanently(RESTAURANT, 4417L);

        verify(itemRepository).delete(junk);
    }

    @Test
    @DisplayName("an item in past orders is refused, and the message says why")
    void orderedItemIsRefused() {
        MenuItem sold = item(4418L, "Lavash", false);
        when(itemRepository.findById(4418L)).thenReturn(Optional.of(sold));
        when(itemRepository.isReferencedByAnyOrder(4418L)).thenReturn(true);

        assertThatThrownBy(() -> menuService.deleteItemPermanently(RESTAURANT, 4418L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Lavash")
                .hasMessageContaining("past orders");

        verify(itemRepository, never()).delete(any(MenuItem.class));
    }

    @Test
    @DisplayName("a purge clears what it can and keeps what it must")
    void purgeSeparatesTheTwo() {
        MenuItem junkOne = item(1L, "Imported junk 1", false);
        MenuItem sold = item(2L, "Lavash", false);
        MenuItem junkTwo = item(3L, "Imported junk 2", false);
        when(itemRepository.findInactiveByRestaurantId(RESTAURANT))
                .thenReturn(List.of(junkOne, sold, junkTwo));
        when(itemRepository.isReferencedByAnyOrder(1L)).thenReturn(false);
        when(itemRepository.isReferencedByAnyOrder(2L)).thenReturn(true);
        when(itemRepository.isReferencedByAnyOrder(3L)).thenReturn(false);

        MenuService.PurgeReport report = menuService.purgeInactiveItems(RESTAURANT);

        assertThat(report.removed()).isEqualTo(2);
        assertThat(report.keptForOrderHistory()).isEqualTo(1);
        // Named, not just counted: a vendor should be able to see the leftovers
        // are dishes people really ordered rather than a silent failure.
        assertThat(report.keptNames()).containsExactly("Lavash");

        verify(itemRepository).delete(junkOne);
        verify(itemRepository).delete(junkTwo);
        verify(itemRepository, never()).delete(sold);
    }

    @Test
    @DisplayName("a purge with nothing to purge is not an error")
    void emptyPurge() {
        when(itemRepository.findInactiveByRestaurantId(RESTAURANT)).thenReturn(List.of());

        MenuService.PurgeReport report = menuService.purgeInactiveItems(RESTAURANT);

        assertThat(report.removed()).isZero();
        assertThat(report.keptNames()).isEmpty();
    }

    @Test
    @DisplayName("the item's image goes with it")
    void imageIsCleanedUp() {
        // Nothing else would ever delete it: the row that pointed at the file is
        // gone, so the file becomes unreferenced bytes on a volume forever.
        MenuItem junk = item(4417L, "Imported junk", false);
        junk.setImagePath("/app/images/menu-items/abc.png");
        when(itemRepository.findById(4417L)).thenReturn(Optional.of(junk));
        when(itemRepository.isReferencedByAnyOrder(4417L)).thenReturn(false);

        menuService.deleteItemPermanently(RESTAURANT, 4417L);

        verify(imageStorageService).deleteImage("menu-items/abc.png");
    }
}
