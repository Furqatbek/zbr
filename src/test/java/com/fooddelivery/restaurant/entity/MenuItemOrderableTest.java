package com.fooddelivery.restaurant.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Whether a dish can be ordered, as the menu reports it.
 *
 * <p>{@code inStock} is the venue's switch for the dish, and it is not the
 * whole answer: a dish sold in sizes with every size gone cannot be ordered
 * however that switch is set. Reporting it as available and then refusing the
 * order is the dead end this closes — the customer was asked to choose a size
 * and then told every size was unavailable.
 */
@DisplayName("Menu item orderability")
class MenuItemOrderableTest {

    private MenuItem dish(boolean inStock, boolean active) {
        return MenuItem.builder()
                .id(1L).name("Lavash").price(new BigDecimal("30000"))
                .inStock(inStock).active(active)
                .variants(new HashSet<>()).options(new HashSet<>())
                .build();
    }

    private ItemVariant size(String name, boolean inStock, boolean active) {
        return ItemVariant.builder().name(name).priceDelta(BigDecimal.ZERO)
                .inStock(inStock).active(active).build();
    }

    @Test
    @DisplayName("a plain dish follows its own stock flag")
    void plainDish() {
        assertThat(dish(true, true).isOrderable()).isTrue();
        assertThat(dish(false, true).isOrderable()).isFalse();
        assertThat(dish(true, false).isOrderable()).isFalse();
    }

    @Test
    @DisplayName("a dish with one size left is orderable")
    void oneSizeLeft() {
        MenuItem item = dish(true, true);
        item.getVariants().add(size("Regular", false, true));
        item.getVariants().add(size("Large", true, true));

        assertThat(item.isOrderable()).isTrue();
    }

    @Test
    @DisplayName("a dish whose every size is sold out is not orderable")
    void everySizeGone() {
        MenuItem item = dish(true, true);
        item.getVariants().add(size("Regular", false, true));
        item.getVariants().add(size("Large", false, true));

        assertThat(item.isOrderable()).isFalse();
    }

    @Test
    @DisplayName("a withdrawn size does not keep a dish alive")
    void inactiveSizesDoNotCount() {
        // active=false means the venue took the size off the menu, so it is not
        // something a customer could choose however its stock flag reads.
        MenuItem item = dish(true, true);
        item.getVariants().add(size("Regular", false, true));
        item.getVariants().add(size("Discontinued", true, false));

        assertThat(item.isOrderable()).isFalse();
    }

    @Test
    @DisplayName("a dish the venue switched off stays off, sizes or not")
    void venueSwitchWins() {
        MenuItem item = dish(false, true);
        item.getVariants().add(size("Large", true, true));

        assertThat(item.isOrderable()).isFalse();
    }

    @Test
    @DisplayName("a sold-out add-on does not take the dish off the menu")
    void addOnsDoNotAffectIt() {
        // A sauce being out is a reason to pick another sauce. Sizes are
        // different because they carry the price.
        MenuItem item = dish(true, true);
        item.getOptions().add(ItemOption.builder().name("Garlic").groupName("Sauce")
                .priceDelta(BigDecimal.ZERO).required(true).inStock(false).active(true).build());

        assertThat(item.isOrderable()).isTrue();
    }
}
