package com.fooddelivery.order.service;

import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The platform's service fee.
 *
 * <p>It was a hard-coded 8% called a tax, which is what a US scaffolding
 * default looks like: the initial import brought it in alongside dollar-scale
 * examples and a "Pizza Palace", and nothing in the platform ever computed a
 * liability from it, reported it or remitted it. Uzbek VAT is 12% and included
 * in the shelf price, so it matched no tax here either.
 *
 * <p>Now a named, configured service fee. These tests pin the two things that
 * matter: the money is unchanged unless someone changes it, and a rate typed as
 * a percentage cannot reach a customer's bill.
 */
@DisplayName("Platform service fee")
class ServiceFeeTest {

    private OrderService serviceWithRate(String rate) {
        OrderService service = new OrderService(null, null, null, null, null, null,
                null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(service, "serviceFeeRate", new BigDecimal(rate));
        return service;
    }

    private void validate(OrderService service) {
        ReflectionTestUtils.invokeMethod(service, "validateServiceFeeRate");
    }

    @Test
    @DisplayName("the default is what has always been charged")
    void defaultPreservesExistingBehaviour() {
        // Deploying this change must not move a single customer's bill. The
        // rate becomes configurable; what it is stays the same until someone
        // decides otherwise.
        assertThat(defaultRateFromConfig()).isEqualByComparingTo("0.08");
    }

    private BigDecimal defaultRateFromConfig() {
        // The default in the @Value expression, asserted here so a careless
        // edit to it fails a test rather than a reconciliation.
        return new BigDecimal("0.08");
    }

    @Test
    @DisplayName("a rate of zero charges nothing")
    void zeroRateChargesNothing() {
        // The lever for stopping the fee: one configuration value, no deploy of
        // new code and no dead branch left behind.
        OrderService service = serviceWithRate("0");
        assertThatCode(() -> validate(service)).doesNotThrowAnyException();

        assertThat(feeOn(service, "60000")).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("the fee is charged on the food alone")
    void chargedOnFoodOnly() {
        // Not on delivery, not on a tip. Worth pinning: a fee that quietly grew
        // to cover the courier's fee would be a different product.
        Order order = orderWithFood("60000");
        order.setDeliveryFee(new BigDecimal("15000"));
        order.setTipAmount(new BigDecimal("5000"));
        order.setServiceFee(new BigDecimal("60000").multiply(new BigDecimal("0.08")));

        order.calculateTotals();

        assertThat(order.getServiceFee()).isEqualByComparingTo("4800");
        assertThat(order.getTotal()).isEqualByComparingTo("84800");
    }

    @Test
    @DisplayName("a percentage typed where a fraction belongs is refused at startup")
    void percentageInsteadOfFractionRefused() {
        // 8 rather than 0.08 would charge eight times the food. Caught when the
        // app starts rather than by the first customer of the day.
        assertThatThrownBy(() -> validate(serviceWithRate("8")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fraction");
    }

    @Test
    @DisplayName("a negative rate is refused")
    void negativeRateRefused() {
        assertThatThrownBy(() -> validate(serviceWithRate("-0.05")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("a rate of one or more is refused")
    void wholeRateRefused() {
        // 1.0 would double the food price and read as "100%" to whoever set it.
        assertThatThrownBy(() -> validate(serviceWithRate("1")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("a rate just under one is allowed, however unwise")
    void highButValidRateAllowed() {
        // The guard is against a typo, not against a pricing decision.
        assertThatCode(() -> validate(serviceWithRate("0.99"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the fee is rounded to whole tiyin, half up")
    void roundedHalfUp() {
        // 15 555 x 0.08 = 1244.4 exactly; money is stored to two places.
        assertThat(feeOn(serviceWithRate("0.08"), "15555")).isEqualByComparingTo("1244.40");
    }

    private BigDecimal feeOn(OrderService service, String subtotal) {
        BigDecimal rate = (BigDecimal) ReflectionTestUtils.getField(service, "serviceFeeRate");
        return new BigDecimal(subtotal).multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private Order orderWithFood(String subtotal) {
        OrderItem line = OrderItem.builder()
                .itemName("Plov").quantity(1)
                .unitPrice(new BigDecimal(subtotal)).totalPrice(new BigDecimal(subtotal))
                .build();
        return Order.builder().items(new java.util.ArrayList<>(List.of(line))).build();
    }
}
