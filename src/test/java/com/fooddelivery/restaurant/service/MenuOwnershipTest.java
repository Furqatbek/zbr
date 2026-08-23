package com.fooddelivery.restaurant.service;

import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.restaurant.entity.MenuCategory;
import com.fooddelivery.restaurant.entity.MenuItem;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.repository.MenuCategoryRepository;
import com.fooddelivery.restaurant.repository.MenuItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The menu endpoints are nested: /restaurants/{restaurantId}/menu/items/{itemId}.
 * The controller proves the caller owns {restaurantId}; these tests cover the
 * other half — that the item actually belongs to that restaurant. Without it an
 * owner passes their own restaurant id alongside a competitor's item id and the
 * ownership check happily passes.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MenuOwnershipTest {

    private static final Long MINE = 1L;
    private static final Long THEIRS = 2L;

    @Mock
    private MenuItemRepository itemRepository;

    @Mock
    private MenuCategoryRepository categoryRepository;

    @InjectMocks
    private MenuService menuService;

    private MenuItem itemOwnedBy(Long restaurantId) {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        MenuCategory category = new MenuCategory();
        category.setId(50L);
        category.setRestaurant(restaurant);
        MenuItem item = new MenuItem();
        item.setId(99L);
        item.setCategory(category);
        return item;
    }

    @Test
    @DisplayName("cannot edit a menu item belonging to another restaurant")
    void cannotEditForeignItem() {
        when(itemRepository.findById(99L)).thenReturn(Optional.of(itemOwnedBy(THEIRS)));

        assertThatThrownBy(() -> menuService.updateItemStock(MINE, 99L, false))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("cannot delete a menu item belonging to another restaurant")
    void cannotDeleteForeignItem() {
        when(itemRepository.findById(99L)).thenReturn(Optional.of(itemOwnedBy(THEIRS)));

        assertThatThrownBy(() -> menuService.deleteItem(MINE, 99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("cannot delete a category belonging to another restaurant")
    void cannotDeleteForeignCategory() {
        Restaurant theirs = new Restaurant();
        theirs.setId(THEIRS);
        MenuCategory category = new MenuCategory();
        category.setId(50L);
        category.setRestaurant(theirs);
        when(categoryRepository.findById(50L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> menuService.deleteCategory(MINE, 50L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("own items are still reachable")
    void ownItemIsEditable() {
        MenuItem mine = itemOwnedBy(MINE);
        when(itemRepository.findById(99L)).thenReturn(Optional.of(mine));
        when(itemRepository.save(mine)).thenReturn(mine);

        menuService.deleteItem(MINE, 99L);

        verify(itemRepository).save(mine);
    }
}
