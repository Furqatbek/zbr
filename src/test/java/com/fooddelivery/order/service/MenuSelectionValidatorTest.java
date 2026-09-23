package com.fooddelivery.order.service;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.order.dto.OrderItemRequest;
import com.fooddelivery.restaurant.entity.ItemOption;
import com.fooddelivery.restaurant.entity.ItemVariant;
import com.fooddelivery.restaurant.entity.MenuItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Whether a customer's choices are ones the kitchen can cook.
 *
 * <p>The order path used to check only that an id belonged to the dish, so an
 * order could arrive with a required choice missing, with more add-ons than the
 * group allows, or with a size that sold out at lunchtime — accepted, priced,
 * and discovered by whoever had to make it. The app checks the same rules; this
 * exists because a rule that lives only in a client is one release away from
 * not existing.
 */
@DisplayName("Menu selection validation")
class MenuSelectionValidatorTest {

    private final MenuSelectionValidator validator = new MenuSelectionValidator();

    private MenuItem lavash() {
        return MenuItem.builder()
                .id(4417L).name("Lavash").price(new BigDecimal("30000")).inStock(true)
                .variants(new java.util.HashSet<>())
                .options(new java.util.HashSet<>())
                .build();
    }

    private ItemVariant variant(long id, String name, boolean inStock, boolean active) {
        return ItemVariant.builder().id(id).name(name)
                .priceDelta(new BigDecimal("8000")).inStock(inStock).active(active).build();
    }

    private ItemOption option(long id, String group, String name, boolean required,
                              int maxSelections, boolean inStock, boolean active) {
        return ItemOption.builder().id(id).groupName(group).name(name)
                .priceDelta(new BigDecimal("5000")).required(required)
                .maxSelections(maxSelections).inStock(inStock).active(active).build();
    }

    private OrderItemRequest choosing(Long variantId, Long... optionIds) {
        OrderItemRequest request = new OrderItemRequest();
        request.setMenuItemId(4417L);
        request.setQuantity(1);
        request.setVariantId(variantId);
        request.setOptionIds(optionIds.length == 0 ? null : List.of(optionIds));
        return request;
    }

    @Nested
    @DisplayName("Sizes")
    class Sizes {

        @Test
        @DisplayName("a dish sold in sizes cannot be ordered without one")
        void sizeIsRequiredWhenOffered() {
            // Without it the customer pays the base price and the kitchen has
            // to guess which one to make.
            MenuItem item = lavash();
            item.getVariants().add(variant(11L, "Regular", true, true));
            item.getVariants().add(variant(12L, "Large", true, true));

            assertThatThrownBy(() -> validator.validate(item, choosing(null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Choose a size")
                    .hasMessageContaining("Lavash")
                    // Names the choices, because a customer reads this.
                    .hasMessageContaining("Regular");
        }

        @Test
        @DisplayName("a dish with no sizes is fine without one")
        void noSizesNoProblem() {
            assertThatCode(() -> validator.validate(lavash(), choosing(null)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("a sold-out size is refused, and says so")
        void soldOutSizeIsRefused() {
            MenuItem item = lavash();
            item.getVariants().add(variant(12L, "Large", false, true));

            assertThatThrownBy(() -> validator.validate(item, choosing(12L)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("sold out")
                    .hasMessageContaining("Large");
        }

        @Test
        @DisplayName("a hidden size cannot be ordered by id")
        void inactiveSizeIsRefused() {
            // The client may be holding a menu from before the venue withdrew
            // it. Nothing stops an id being replayed.
            MenuItem item = lavash();
            item.getVariants().add(variant(12L, "Large", true, false));

            assertThatThrownBy(() -> validator.validate(item, choosing(12L)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no longer available");
        }

        @Test
        @DisplayName("an available size passes")
        void goodSizePasses() {
            MenuItem item = lavash();
            item.getVariants().add(variant(12L, "Large", true, true));

            assertThatCode(() -> validator.validate(item, choosing(12L))).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Add-ons")
    class AddOns {

        @Test
        @DisplayName("a required group must be answered")
        void requiredGroupMustBeChosen() {
            MenuItem item = lavash();
            item.getOptions().add(option(51L, "Sauce", "Garlic", true, 1, true, true));
            item.getOptions().add(option(52L, "Sauce", "Spicy", true, 1, true, true));

            assertThatThrownBy(() -> validator.validate(item, choosing(null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Sauce");
        }

        @Test
        @DisplayName("an optional group may be skipped")
        void optionalGroupMayBeSkipped() {
            MenuItem item = lavash();
            item.getOptions().add(option(60L, "Extras", "Cheese", false, 3, true, true));

            assertThatCode(() -> validator.validate(item, choosing(null))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("more than the group allows is refused")
        void capIsEnforced() {
            MenuItem item = lavash();
            item.getOptions().add(option(60L, "Extras", "Cheese", false, 2, true, true));
            item.getOptions().add(option(61L, "Extras", "Bacon", false, 2, true, true));
            item.getOptions().add(option(62L, "Extras", "Egg", false, 2, true, true));

            assertThatThrownBy(() -> validator.validate(item, choosing(null, 60L, 61L, 62L)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("at most 2");
        }

        @Test
        @DisplayName("the strictest cap in a group wins")
        void strictestCapWins() {
            // maxSelections is stored on every option although it describes the
            // group. A cap is not a cap if one sloppy row can lift it.
            MenuItem item = lavash();
            item.getOptions().add(option(60L, "Extras", "Cheese", false, 1, true, true));
            item.getOptions().add(option(61L, "Extras", "Bacon", false, 5, true, true));

            assertThatThrownBy(() -> validator.validate(item, choosing(null, 60L, 61L)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("at most 1");
        }

        @Test
        @DisplayName("caps apply per group, not across the dish")
        void capsArePerGroup() {
            MenuItem item = lavash();
            item.getOptions().add(option(51L, "Sauce", "Garlic", true, 1, true, true));
            item.getOptions().add(option(60L, "Extras", "Cheese", false, 1, true, true));

            assertThatCode(() -> validator.validate(item, choosing(null, 51L, 60L)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("a sold-out add-on is refused")
        void soldOutAddOnIsRefused() {
            MenuItem item = lavash();
            item.getOptions().add(option(60L, "Extras", "Cheese", false, 3, false, true));

            assertThatThrownBy(() -> validator.validate(item, choosing(null, 60L)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Cheese")
                    .hasMessageContaining("sold out");
        }

        @Test
        @DisplayName("an add-on from another dish is refused")
        void foreignAddOnIsRefused() {
            MenuItem item = lavash();
            item.getOptions().add(option(60L, "Extras", "Cheese", false, 3, true, true));

            assertThatThrownBy(() -> validator.validate(item, choosing(null, 999L)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no longer available");
        }

        @Test
        @DisplayName("the same add-on twice is refused, not charged twice")
        void duplicatesAreRefused() {
            // Nothing in the model carries a quantity per add-on, so a repeat
            // would be billed twice and cooked once.
            MenuItem item = lavash();
            item.getOptions().add(option(60L, "Extras", "Cheese", false, 3, true, true));

            assertThatThrownBy(() -> validator.validate(item, choosing(null, 60L, 60L)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("twice");
        }

        @Test
        @DisplayName("ungrouped options are each their own question")
        void ungroupedOptions() {
            MenuItem item = lavash();
            item.getOptions().add(option(70L, null, "Extra napkins", false, 1, true, true));

            assertThatCode(() -> validator.validate(item, choosing(null, 70L)))
                    .doesNotThrowAnyException();
            assertThatThrownBy(() -> validator.validate(item, choosing(null, 70L, 70L)))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Test
    @DisplayName("a dish with neither sizes nor add-ons is untouched by any of this")
    void plainItemPasses() {
        assertThatCode(() -> validator.validate(lavash(), choosing(null)))
                .doesNotThrowAnyException();
    }
}
