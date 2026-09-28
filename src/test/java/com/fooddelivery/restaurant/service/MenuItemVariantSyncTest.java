package com.fooddelivery.restaurant.service;

import com.fooddelivery.restaurant.dto.CreateItemOptionRequest;
import com.fooddelivery.restaurant.dto.CreateItemVariantRequest;
import com.fooddelivery.restaurant.dto.CreateMenuItemRequest;
import com.fooddelivery.restaurant.entity.ItemOption;
import com.fooddelivery.restaurant.entity.ItemVariant;
import com.fooddelivery.restaurant.entity.MenuCategory;
import com.fooddelivery.restaurant.entity.MenuItem;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.repository.MenuItemRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Editing a dish that has sizes, from a client that sends the whole dish back.
 *
 * <p>The vendor app reads an item, changes one field and PUTs the entire thing —
 * which is what PUT means. This endpoint dropped the nested sizes silently, and
 * the fix for that was to refuse any body containing them. That was the wrong
 * remedy: changing a price started failing in order to guard against a rarer
 * mistake, and a vendor could not edit their menu at all.
 *
 * <p>The delicate part is not accepting them, it is not churning their ids. A
 * customer's basket holds a variant id; rebuilding the rows whenever a vendor
 * corrects a typo would tell that customer their size no longer exists.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Updating an item with sizes and add-ons")
class MenuItemVariantSyncTest {

    private static final Long RESTAURANT = 3L;
    private static final Long ITEM = 4417L;

    @Mock private MenuItemRepository itemRepository;
    @Mock private com.fooddelivery.restaurant.repository.MenuCategoryRepository categoryRepository;
    @Mock private RestaurantService restaurantService;
    @Mock private com.fooddelivery.restaurant.mapper.RestaurantMapper mapper;
    @Mock private com.fooddelivery.common.service.ImageStorageService imageStorageService;

    @InjectMocks
    private MenuService menuService;

    private MenuItem lavash() {
        MenuItem item = MenuItem.builder()
                .id(ITEM).name("Lavash").price(new BigDecimal("30000"))
                .category(MenuCategory.builder().id(12L)
                        .restaurant(Restaurant.builder().id(RESTAURANT).build()).build())
                .variants(new java.util.HashSet<>())
                .options(new java.util.HashSet<>())
                .build();
        when(itemRepository.findById(ITEM)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(MenuItem.class))).thenAnswer(c -> c.getArgument(0));
        return item;
    }

    private ItemVariant storedVariant(long id, String name, String delta) {
        return ItemVariant.builder().id(id).name(name)
                .priceDelta(new BigDecimal(delta)).inStock(true).active(true).build();
    }

    private CreateItemVariantRequest wanted(String name, String delta) {
        CreateItemVariantRequest v = new CreateItemVariantRequest();
        v.setName(name);
        v.setPriceDelta(new BigDecimal(delta));
        return v;
    }

    private CreateMenuItemRequest edit(List<CreateItemVariantRequest> variants) {
        CreateMenuItemRequest r = new CreateMenuItemRequest();
        r.setPrice(new BigDecimal("35000"));
        r.setVariants(variants);
        return r;
    }

    @Test
    @DisplayName("editing a price while sending the sizes back does not fail")
    void priceEditWithVariantsSucceeds() {
        MenuItem item = lavash();
        item.getVariants().add(storedVariant(11L, "Large", "5000"));

        menuService.updateItem(RESTAURANT, ITEM, edit(List.of(wanted("Large", "5000"))));

        assertThat(item.getPrice()).isEqualByComparingTo("35000");
        assertThat(item.getVariants()).hasSize(1);
    }

    @Test
    @DisplayName("an unchanged size keeps its id")
    void unchangedVariantKeepsItsId() {
        // The reason this matches rather than replaces. A customer's basket
        // holds variant 11; rebuilding it as variant 12 because a vendor fixed
        // a price would tell them their size no longer exists.
        MenuItem item = lavash();
        item.getVariants().add(storedVariant(11L, "Large", "5000"));

        menuService.updateItem(RESTAURANT, ITEM, edit(List.of(wanted("Large", "5000"))));

        assertThat(item.getVariants()).singleElement()
                .satisfies(v -> assertThat(v.getId()).isEqualTo(11L));
    }

    @Test
    @DisplayName("a changed price on an existing size updates it in place")
    void priceDeltaIsUpdated() {
        MenuItem item = lavash();
        item.getVariants().add(storedVariant(11L, "Large", "5000"));

        menuService.updateItem(RESTAURANT, ITEM, edit(List.of(wanted("Large", "8000"))));

        assertThat(item.getVariants()).singleElement().satisfies(v -> {
            assertThat(v.getId()).isEqualTo(11L);
            assertThat(v.getPriceDelta()).isEqualByComparingTo("8000");
        });
    }

    @Test
    @DisplayName("a new size is added")
    void newVariantIsAdded() {
        MenuItem item = lavash();
        item.getVariants().add(storedVariant(11L, "Large", "5000"));

        menuService.updateItem(RESTAURANT, ITEM,
                edit(List.of(wanted("Large", "5000"), wanted("Small", "0"))));

        assertThat(item.getVariants()).extracting(ItemVariant::getName)
                .containsExactlyInAnyOrder("Large", "Small");
    }

    @Test
    @DisplayName("a size left out of the list is removed")
    void omittedVariantIsRemoved() {
        // The list is the complete set, which is what PUT means. Safe because an
        // order line snapshots the name and price, and order_items.variant_id
        // carries no foreign key.
        MenuItem item = lavash();
        item.getVariants().add(storedVariant(11L, "Large", "5000"));
        item.getVariants().add(storedVariant(12L, "Small", "0"));

        menuService.updateItem(RESTAURANT, ITEM, edit(List.of(wanted("Large", "5000"))));

        assertThat(item.getVariants()).extracting(ItemVariant::getName).containsExactly("Large");
    }

    @Test
    @DisplayName("omitting the field entirely leaves the sizes alone")
    void absentVariantsAreUntouched() {
        // A client that has never heard of sizes must not delete them by
        // editing a description.
        MenuItem item = lavash();
        item.getVariants().add(storedVariant(11L, "Large", "5000"));

        CreateMenuItemRequest r = new CreateMenuItemRequest();
        r.setPrice(new BigDecimal("35000"));
        menuService.updateItem(RESTAURANT, ITEM, r);

        assertThat(item.getVariants()).hasSize(1);
    }

    @Test
    @DisplayName("an empty list removes them all, because that was said deliberately")
    void emptyListClearsVariants() {
        MenuItem item = lavash();
        item.getVariants().add(storedVariant(11L, "Large", "5000"));

        menuService.updateItem(RESTAURANT, ITEM, edit(List.of()));

        assertThat(item.getVariants()).isEmpty();
    }

    @Test
    @DisplayName("case and stray whitespace do not create a second size")
    void namesAreMatchedLoosely() {
        MenuItem item = lavash();
        item.getVariants().add(storedVariant(11L, "Large", "5000"));

        menuService.updateItem(RESTAURANT, ITEM, edit(List.of(wanted(" large ", "5000"))));

        assertThat(item.getVariants()).singleElement()
                .satisfies(v -> assertThat(v.getId()).isEqualTo(11L));
    }

    @Test
    @DisplayName("add-ons are matched by group and name together")
    void optionsAreKeyedByGroupAndName() {
        // "Garlic" in Sauce and "Garlic" in Extras are two different add-ons.
        MenuItem item = lavash();
        item.getOptions().add(ItemOption.builder().id(51L).groupName("Sauce").name("Garlic")
                .priceDelta(BigDecimal.ZERO).maxSelections(1).inStock(true).active(true).build());

        CreateItemOptionRequest sauce = new CreateItemOptionRequest();
        sauce.setGroupName("Sauce");
        sauce.setName("Garlic");
        sauce.setPriceDelta(BigDecimal.ZERO);
        CreateItemOptionRequest extra = new CreateItemOptionRequest();
        extra.setGroupName("Extras");
        extra.setName("Garlic");
        extra.setPriceDelta(new BigDecimal("2000"));

        CreateMenuItemRequest r = new CreateMenuItemRequest();
        r.setOptions(List.of(sauce, extra));
        menuService.updateItem(RESTAURANT, ITEM, r);

        assertThat(item.getOptions()).hasSize(2);
        assertThat(item.getOptions()).filteredOn(o -> "Sauce".equals(o.getGroupName()))
                .singleElement().satisfies(o -> assertThat(o.getId()).isEqualTo(51L));
    }
}
