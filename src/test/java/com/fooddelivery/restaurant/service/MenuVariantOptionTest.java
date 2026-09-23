package com.fooddelivery.restaurant.service;

import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.restaurant.dto.SaveItemOptionRequest;
import com.fooddelivery.restaurant.dto.SaveItemVariantRequest;
import com.fooddelivery.restaurant.entity.ItemOption;
import com.fooddelivery.restaurant.entity.ItemVariant;
import com.fooddelivery.restaurant.entity.MenuCategory;
import com.fooddelivery.restaurant.entity.MenuItem;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.mapper.RestaurantMapper;
import com.fooddelivery.restaurant.mapper.RestaurantMapperImpl;
import com.fooddelivery.restaurant.repository.MenuCategoryRepository;
import com.fooddelivery.restaurant.repository.MenuItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Adding a size or an add-on to a dish that already exists.
 *
 * <p>There was no way to. Both could only be supplied nested inside the item at
 * creation, and {@code PUT /items/{id}} accepted them in the body and dropped
 * them — so a restaurant wanting one add-on had to delete the dish and build it
 * again, losing its id, its image and its history.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Menu sizes and add-ons")
class MenuVariantOptionTest {

    @Mock private MenuCategoryRepository categoryRepository;
    @Mock private MenuItemRepository itemRepository;
    @Mock private RestaurantService restaurantService;
    @Mock private com.fooddelivery.common.service.ImageStorageService imageStorageService;

    private final RestaurantMapper mapper = new RestaurantMapperImpl();
    private MenuService service;
    private MenuItem item;

    @BeforeEach
    void setUp() {
        service = new MenuService(categoryRepository, itemRepository, restaurantService,
                mapper, imageStorageService);

        Restaurant restaurant = Restaurant.builder().id(3L).name("Qahvoon").build();
        MenuCategory category = MenuCategory.builder().id(6L).name("Cofe").restaurant(restaurant).build();
        item = MenuItem.builder()
                .id(4417L).name("Lavash").category(category)
                .price(new BigDecimal("30000"))
                .variants(new HashSet<>()).options(new HashSet<>())
                .build();

        when(itemRepository.findById(4417L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(MenuItem.class))).thenAnswer(i -> i.getArgument(0));
    }

    // --- sizes -------------------------------------------------------------

    @Test
    @DisplayName("a size can be added to an existing dish")
    void addVariant() {
        var added = service.addVariant(3L, 4417L, SaveItemVariantRequest.builder()
                .name("Large").priceDelta(new BigDecimal("8000")).build());

        assertThat(added.getName()).isEqualTo("Large");
        assertThat(item.getVariants()).hasSize(1);
        assertThat(item.getVariants().iterator().next().getPriceDelta()).isEqualByComparingTo("8000");
    }

    @Test
    @DisplayName("a new size is in stock and active unless told otherwise")
    void sensibleDefaults() {
        service.addVariant(3L, 4417L, SaveItemVariantRequest.builder().name("Large").build());

        ItemVariant variant = item.getVariants().iterator().next();
        assertThat(variant.getInStock()).isTrue();
        assertThat(variant.getActive()).isTrue();
        // A price delta left out means "same price as the dish", not null.
        assertThat(variant.getPriceDelta()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("updating a size leaves out what it does not mention")
    void updateVariantIsPartial() {
        ItemVariant existing = ItemVariant.builder().id(12L).menuItem(item).name("Large")
                .priceDelta(new BigDecimal("8000")).inStock(true).active(true).sortOrder(2).build();
        item.getVariants().add(existing);

        // The daily operation: this size is sold out, nothing else changes.
        service.updateVariant(3L, 4417L, 12L, SaveItemVariantRequest.builder().inStock(false).build());

        assertThat(existing.getInStock()).isFalse();
        assertThat(existing.getName()).isEqualTo("Large");
        assertThat(existing.getPriceDelta()).isEqualByComparingTo("8000");
    }

    @Test
    @DisplayName("a size can be deleted")
    void deleteVariant() {
        ItemVariant existing = ItemVariant.builder().id(12L).menuItem(item).name("Large").build();
        item.getVariants().add(existing);

        service.deleteVariant(3L, 4417L, 12L);

        assertThat(item.getVariants()).isEmpty();
    }

    @Test
    @DisplayName("a size belonging to another dish is not found")
    void unknownVariant() {
        assertThatThrownBy(() -> service.deleteVariant(3L, 4417L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- add-ons -----------------------------------------------------------

    @Test
    @DisplayName("an add-on can be added to an existing dish")
    void addOption() {
        var added = service.addOption(3L, 4417L, SaveItemOptionRequest.builder()
                .groupName("Extras").name("Cheese").priceDelta(new BigDecimal("5000"))
                .maxSelections(3).build());

        assertThat(added.getName()).isEqualTo("Cheese");
        assertThat(added.getGroupName()).isEqualTo("Extras");
        assertThat(item.getOptions()).hasSize(1);
    }

    @Test
    @DisplayName("an add-on is optional, single-select and in stock unless told otherwise")
    void optionDefaults() {
        service.addOption(3L, 4417L, SaveItemOptionRequest.builder().name("Cheese").build());

        ItemOption option = item.getOptions().iterator().next();
        assertThat(option.getRequired()).isFalse();
        assertThat(option.getMaxSelections()).isEqualTo(1);
        assertThat(option.getInStock()).isTrue();
        assertThat(option.getActive()).isTrue();
        assertThat(option.getPriceDelta()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("updating an add-on leaves out what it does not mention")
    void updateOptionIsPartial() {
        ItemOption existing = ItemOption.builder().id(60L).menuItem(item).groupName("Extras")
                .name("Cheese").priceDelta(new BigDecimal("5000")).required(false)
                .maxSelections(3).inStock(true).active(true).build();
        item.getOptions().add(existing);

        service.updateOption(3L, 4417L, 60L, SaveItemOptionRequest.builder()
                .priceDelta(new BigDecimal("6000")).build());

        assertThat(existing.getPriceDelta()).isEqualByComparingTo("6000");
        assertThat(existing.getName()).isEqualTo("Cheese");
        assertThat(existing.getMaxSelections()).isEqualTo(3);
    }

    @Test
    @DisplayName("an add-on can be deleted")
    void deleteOption() {
        item.getOptions().add(ItemOption.builder().id(60L).menuItem(item).name("Cheese").build());

        service.deleteOption(3L, 4417L, 60L);

        assertThat(item.getOptions()).isEmpty();
    }

    // --- ownership ---------------------------------------------------------

    @Test
    @DisplayName("another restaurant cannot touch this dish's sizes")
    void ownershipIsChecked() {
        // Answering "not found" rather than "not yours": an id that is not
        // yours does not exist as far as you are concerned, and the difference
        // is a way to enumerate other restaurants' menus.
        assertThatThrownBy(() -> service.addVariant(99L, 4417L,
                SaveItemVariantRequest.builder().name("Large").build()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> service.addOption(99L, 4417L,
                SaveItemOptionRequest.builder().name("Cheese").build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("sort order follows what is already there")
    void sortOrderIsAppended() {
        item.getVariants().add(ItemVariant.builder().id(11L).menuItem(item)
                .name("Regular").sortOrder(0).build());
        item.getVariants().add(ItemVariant.builder().id(12L).menuItem(item)
                .name("Large").sortOrder(1).build());

        service.addVariant(3L, 4417L, SaveItemVariantRequest.builder().name("Family").build());

        ItemVariant added = item.getVariants().stream()
                .filter(v -> "Family".equals(v.getName())).findFirst().orElseThrow();
        assertThat(added.getSortOrder()).isEqualTo(2);
    }
}
