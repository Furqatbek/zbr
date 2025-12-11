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
            "CREATED, ACCEPTED, true",
            "CREATED, CANCELLED, true",
            "CREATED, PREPARING, false",
            "ACCEPTED, PREPARING, true",
            "ACCEPTED, CANCELLED, true",
            "ACCEPTED, CREATED, false",
            "PREPARING, READY, true",
            "PREPARING, CANCELLED, true",
            "PREPARING, ACCEPTED, false",
            "READY, PICKED_UP, true",
            "READY, PREPARING, false",
            "PICKED_UP, IN_TRANSIT, true",
            "PICKED_UP, DELIVERED, false",
            "IN_TRANSIT, DELIVERED, true",
            "IN_TRANSIT, PICKED_UP, false",
            "DELIVERED, COMPLETED, true",
            "DELIVERED, CANCELLED, false",
            "COMPLETED, CREATED, false",
            "CANCELLED, CREATED, false"
    })
    @DisplayName("Should validate status transitions correctly")
    void shouldValidateStatusTransitions(String from, String to, boolean expected) {
        OrderStatus fromStatus = OrderStatus.valueOf(from);
        OrderStatus toStatus = OrderStatus.valueOf(to);

        assertThat(fromStatus.canTransitionTo(toStatus)).isEqualTo(expected);
    }

    @Test
    @DisplayName("CREATED status should allow transitions to ACCEPTED and CANCELLED only")
    void createdStatusAllowedTransitions() {
        OrderStatus status = OrderStatus.CREATED;

        assertThat(status.getAllowedTransitions())
                .containsExactlyInAnyOrder(OrderStatus.ACCEPTED, OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("COMPLETED status should have no allowed transitions")
    void completedStatusNoTransitions() {
        OrderStatus status = OrderStatus.COMPLETED;

        assertThat(status.getAllowedTransitions()).isEmpty();
    }

    @Test
    @DisplayName("CANCELLED status should have no allowed transitions")
    void cancelledStatusNoTransitions() {
        OrderStatus status = OrderStatus.CANCELLED;

        assertThat(status.getAllowedTransitions()).isEmpty();
    }

    @Test
    @DisplayName("Full happy path order flow should be valid")
    void fullHappyPathOrderFlow() {
        OrderStatus[] happyPath = {
                OrderStatus.CREATED,
                OrderStatus.ACCEPTED,
                OrderStatus.PREPARING,
                OrderStatus.READY,
                OrderStatus.PICKED_UP,
                OrderStatus.IN_TRANSIT,
                OrderStatus.DELIVERED,
                OrderStatus.COMPLETED
        };

        for (int i = 0; i < happyPath.length - 1; i++) {
            assertThat(happyPath[i].canTransitionTo(happyPath[i + 1]))
                    .withFailMessage("Expected %s to transition to %s", happyPath[i], happyPath[i + 1])
                    .isTrue();
        }
    }
}
