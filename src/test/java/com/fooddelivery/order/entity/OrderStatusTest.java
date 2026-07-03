package com.fooddelivery.order.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderStatus State Machine Tests")
class OrderStatusTest {

    @ParameterizedTest
    @CsvSource({
            // CREATED
            "CREATED, ACCEPTED, true",
            "CREATED, PREPARING, true",
            "CREATED, CANCELLED, true",
            "CREATED, READY, false",
            // ACCEPTED
            "ACCEPTED, PREPARING, true",
            "ACCEPTED, READY, true",
            "ACCEPTED, COURIER_ASSIGNED, true",
            "ACCEPTED, CANCELLED, true",
            "ACCEPTED, CREATED, false",
            // PREPARING
            "PREPARING, READY, true",
            "PREPARING, COURIER_ASSIGNED, true",
            "PREPARING, CANCELLED, true",
            "PREPARING, ACCEPTED, false",
            // READY
            "READY, COURIER_ASSIGNED, true",
            "READY, DELIVERED, true",
            "READY, COMPLETED, true",
            "READY, CANCELLED, true",
            "READY, PREPARING, false",
            // COURIER_ASSIGNED — deadlock fix: must still allow PREPARING/READY
            "COURIER_ASSIGNED, PREPARING, true",
            "COURIER_ASSIGNED, READY, true",
            "COURIER_ASSIGNED, PICKED_UP, true",
            "COURIER_ASSIGNED, CANCELLED, true",
            "COURIER_ASSIGNED, DELIVERED, false",
            // PICKED_UP
            "PICKED_UP, IN_TRANSIT, true",
            "PICKED_UP, DELIVERED, true",
            "PICKED_UP, READY, false",
            // IN_TRANSIT
            "IN_TRANSIT, DELIVERED, true",
            "IN_TRANSIT, PICKED_UP, false",
            // DELIVERED
            "DELIVERED, COMPLETED, true",
            "DELIVERED, REFUNDED, true",
            "DELIVERED, CANCELLED, false",
            // terminal-ish
            "COMPLETED, REFUNDED, true",
            "COMPLETED, CREATED, false",
            "CANCELLED, REFUNDED, true",
            "CANCELLED, CREATED, false",
            "REFUNDED, COMPLETED, false"
    })
    @DisplayName("Should validate status transitions correctly")
    void shouldValidateStatusTransitions(String from, String to, boolean expected) {
        assertThat(OrderStatus.valueOf(from).canTransitionTo(OrderStatus.valueOf(to)))
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("CREATED allows ACCEPTED, PREPARING, CANCELLED")
    void createdStatusAllowedTransitions() {
        assertThat(OrderStatus.CREATED.getAllowedTransitions())
                .containsExactlyInAnyOrder(OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("COURIER_ASSIGNED allows PREPARING, READY, PICKED_UP, CANCELLED (no deadlock)")
    void courierAssignedAllowsRestaurantToContinue() {
        // Regression guard: a courier may accept before the kitchen finishes, so the
        // restaurant must still be able to move the order to PREPARING/READY.
        assertThat(OrderStatus.COURIER_ASSIGNED.getAllowedTransitions())
                .containsExactlyInAnyOrder(
                        OrderStatus.PREPARING, OrderStatus.READY,
                        OrderStatus.PICKED_UP, OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("REFUNDED is terminal (no allowed transitions)")
    void refundedIsTerminal() {
        assertThat(OrderStatus.REFUNDED.getAllowedTransitions()).isEmpty();
    }

    @Test
    @DisplayName("DELIVERED and COMPLETED can be refunded")
    void deliveredAndCompletedCanBeRefunded() {
        assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.REFUNDED)).isTrue();
        assertThat(OrderStatus.COMPLETED.canTransitionTo(OrderStatus.REFUNDED)).isTrue();
    }

    @Test
    @DisplayName("Full happy-path order flow is valid end to end")
    void fullHappyPathOrderFlow() {
        OrderStatus[] happyPath = {
                OrderStatus.CREATED, OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY,
                OrderStatus.COURIER_ASSIGNED, OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT,
                OrderStatus.DELIVERED, OrderStatus.COMPLETED
        };
        for (int i = 0; i < happyPath.length - 1; i++) {
            assertThat(happyPath[i].canTransitionTo(happyPath[i + 1]))
                    .withFailMessage("Expected %s -> %s", happyPath[i], happyPath[i + 1])
                    .isTrue();
        }
    }
}
