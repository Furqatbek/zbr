package com.fooddelivery.restaurant.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A menu item costs what the restaurant set it to cost.
 *
 * <p>It did not. {@code MenuService} added a hard-coded 10% to every price
 * created or edited in the vendor app or the admin panel, and
 * {@code priceWithMargin} — the field it writes — is what
 * {@code MenuItem.getEffectivePrice()} returns and what the customer pays. A
 * venue entering 35 000 had 38 500 charged while its own screens kept showing
 * 35 000, so the one party able to notice could not see it.
 *
 * <p>Two siblings turned up the same week: the same 10% in the Restos importer,
 * and an 8% "tax" that was a US sales-tax default. All three were inherited
 * with the scaffolding and none was ever chosen.
 *
 * <p>So this does not test that a rate defaults to zero — it tests that there
 * is no rate. A configurable markup would be the fourth one of these, waiting
 * for an environment variable.
 */
@DisplayName("Menu item pricing")
class MenuItemPricingTest {

    @Test
    @DisplayName("the charged price is the price that was set, to the cent")
    void chargedPriceIsTheSetPrice() {
        MenuService service = new MenuService(null, null, null, null, null);

        assertThat(charged(service, "35000")).isEqualByComparingTo("35000");
        assertThat(charged(service, "15000")).isEqualByComparingTo("15000");
        assertThat(charged(service, "0.01")).isEqualByComparingTo("0.01");
    }

    @Test
    @DisplayName("a null price stays null rather than becoming zero")
    void nullPriceIsNotZero() {
        // A partial update that does not mention price must not rewrite the
        // charged price to nothing.
        assertThat(charged(new MenuService(null, null, null, null, null), null)).isNull();
    }

    @Test
    @DisplayName("there is no markup rate to configure, anywhere in this service")
    void noMarkupRateExists() {
        // The point of the whole exercise. A rate defaulting to zero is one
        // environment variable away from being the fourth silent markup on this
        // platform; a rate that does not exist is not.
        for (Field field : MenuService.class.getDeclaredFields()) {
            assertThat(field.getName().toLowerCase())
                    .as("field %s looks like a pricing rate", field.getName())
                    .doesNotContain("margin")
                    .doesNotContain("markup");
        }
    }

    @Test
    @DisplayName("nothing in the pricing path multiplies a price")
    void nothingMultipliesThePrice() {
        // Read as: whatever comes in, comes out. Asserted across a range so a
        // percentage reintroduced for "just large orders" fails here too.
        MenuService service = new MenuService(null, null, null, null, null);
        for (String price : new String[]{"1", "999", "15000", "35000", "1250000"}) {
            assertThat(charged(service, price))
                    .as("price %s", price)
                    .isEqualByComparingTo(price);
        }
    }

    private BigDecimal charged(MenuService service, String setPrice) {
        BigDecimal price = setPrice == null ? null : new BigDecimal(setPrice);
        try {
            java.lang.reflect.Method method = MenuService.class
                    .getDeclaredMethod("chargedPriceFor", BigDecimal.class);
            method.setAccessible(true);
            return (BigDecimal) method.invoke(service, price);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
