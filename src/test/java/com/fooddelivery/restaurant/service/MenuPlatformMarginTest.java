package com.fooddelivery.restaurant.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The markup added to a price a restaurant typed.
 *
 * <p>It was a hard-coded 10% on every item created or edited in the vendor app
 * and the admin panel. A venue entering 35 000 had 38 500 charged to the
 * customer, and its own screen went on showing 35 000 — so the one party who
 * could have noticed could not see it. It was not commission either: that is
 * 15%, taken from the venue's payout, and unaffected.
 *
 * <p>The third markup of this shape: the same 10% in the Restos importer and an
 * 8% "tax" that was a US sales-tax default. All three inherited, none chosen.
 */
@DisplayName("Menu platform margin")
class MenuPlatformMarginTest {

    private MenuService serviceWithMargin(String rate) {
        MenuService service = new MenuService(null, null, null, null, null);
        ReflectionTestUtils.setField(service, "platformMargin", new BigDecimal(rate));
        return service;
    }

    private BigDecimal charged(MenuService service, String typedPrice) {
        // Plain reflection rather than ReflectionTestUtils: the null-price case
        // matters here, and varargs dispatch cannot express it.
        BigDecimal price = typedPrice == null ? null : new BigDecimal(typedPrice);
        try {
            java.lang.reflect.Method method = MenuService.class
                    .getDeclaredMethod("calculatePriceWithMargin", BigDecimal.class);
            method.setAccessible(true);
            return (BigDecimal) method.invoke(service, price);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private void validate(MenuService service) {
        ReflectionTestUtils.invokeMethod(service, "validatePlatformMargin");
    }

    @Test
    @DisplayName("by default the customer is charged the price the venue typed")
    void defaultChargesWhatWasTyped() {
        MenuService service = serviceWithMargin("0");

        assertThat(charged(service, "35000")).isEqualByComparingTo("35000");
        assertThat(charged(service, "15000")).isEqualByComparingTo("15000");
    }

    @Test
    @DisplayName("the shipped default is zero, in both places it can be set")
    void bothDefaultsAreZero() {
        // Two places a default hides: the @Value expression and the env-var
        // fallback in application.yml, and the yml is the one that wins. An
        // earlier test of the service fee restated a literal instead of reading
        // either, and would have passed with the two out of step.
        assertThat(annotationDefault()).isEqualByComparingTo("0");
        assertThat(ymlEnvFallback()).isEqualTo(annotationDefault().toPlainString());
    }

    @Test
    @DisplayName("a configured margin is applied and rounded to the cent")
    void configuredMarginApplies() {
        // Still possible, still a decision someone has to make deliberately.
        MenuService service = serviceWithMargin("0.10");

        assertThat(charged(service, "35000")).isEqualByComparingTo("38500.00");
        assertThat(charged(service, "15000")).isEqualByComparingTo("16500.00");
    }

    @Test
    @DisplayName("a null price stays null rather than becoming zero")
    void nullPriceIsNotZero() {
        // A partial update that does not mention price must not rewrite the
        // charged price to nothing.
        assertThat(charged(serviceWithMargin("0"), null)).isNull();
    }

    @Test
    @DisplayName("a rate typed as a percentage cannot reach a bill")
    void percentageIsRefused() {
        assertThatThrownBy(() -> validate(serviceWithMargin("10")))
                .hasMessageContaining("fraction, not a percentage");
        assertThatThrownBy(() -> validate(serviceWithMargin("-0.1")))
                .hasMessageContaining("at least 0");
        assertThatCode(() -> validate(serviceWithMargin("0"))).doesNotThrowAnyException();
        assertThatCode(() -> validate(serviceWithMargin("0.15"))).doesNotThrowAnyException();
    }

    private BigDecimal annotationDefault() {
        try {
            String expression = MenuService.class.getDeclaredField("platformMargin")
                    .getAnnotation(org.springframework.beans.factory.annotation.Value.class).value();
            return new BigDecimal(expression.substring(expression.indexOf(':') + 1,
                    expression.lastIndexOf('}')));
        } catch (NoSuchFieldException e) {
            throw new AssertionError("platformMargin is gone — this test needs rewriting", e);
        }
    }

    private String ymlEnvFallback() {
        try (java.io.InputStream in = getClass().getResourceAsStream("/application.yml")) {
            String yml = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("platform-margin-rate:\\s*\\$\\{MENU_PLATFORM_MARGIN_RATE:([^}]*)}")
                    .matcher(yml);
            assertThat(matcher.find()).as("platform-margin-rate in application.yml").isTrue();
            return matcher.group(1);
        } catch (java.io.IOException e) {
            throw new AssertionError("could not read application.yml", e);
        }
    }
}
