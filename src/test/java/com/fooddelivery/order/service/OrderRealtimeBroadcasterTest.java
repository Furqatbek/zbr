package com.fooddelivery.order.service;

import com.fooddelivery.auth.entity.User;
import com.fooddelivery.order.dto.OrderDto;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.mapper.OrderMapper;
import com.fooddelivery.restaurant.entity.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Who hears about an order changing state, live.
 *
 * <p>The two halves of the lifecycle had drifted apart. Restaurant-driven
 * transitions broadcast to the order topic, the customer's queue and the
 * restaurant; courier-driven transitions broadcast nothing at all. A customer
 * watching their order saw it accepted and prepared, then silence for the
 * entire delivery — picked up, on the way, arrived — which is the part they
 * actually wait for.
 *
 * <p>Nothing asserted the destinations, which is why one path could lose them.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderRealtimeBroadcaster")
class OrderRealtimeBroadcasterTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private OrderMapper orderMapper;

    @InjectMocks
    private OrderRealtimeBroadcaster broadcaster;

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(77L)
                .status(OrderStatus.PICKED_UP)
                .consumer(User.builder().id(42L).email("asad@example.com").build())
                .restaurant(Restaurant.builder().id(3L).name("Osh Markazi").build())
                .build();

        when(orderMapper.toDto(any(Order.class))).thenReturn(new OrderDto());
    }

    @Test
    @DisplayName("reaches the order topic, the customer and the restaurant")
    void reachesAllThree() {
        broadcaster.broadcastStatusChange(order);

        verify(messagingTemplate).convertAndSend(eq("/topic/orders/77"), any(OrderDto.class));
        verify(messagingTemplate).convertAndSendToUser(
                eq("asad@example.com"), eq("/queue/orders"), any(OrderDto.class));
        verify(messagingTemplate).convertAndSend(
                eq("/topic/restaurants/3/orders"), any(OrderDto.class));
    }

    @Test
    @DisplayName("one failed destination does not stop the others")
    void oneFailureDoesNotBlockTheRest() {
        // A customer who is simply not connected must not cost the restaurant
        // its update, and vice versa.
        doThrow(new RuntimeException("no session"))
                .when(messagingTemplate).convertAndSend(eq("/topic/orders/77"), any(OrderDto.class));

        broadcaster.broadcastStatusChange(order);

        verify(messagingTemplate).convertAndSendToUser(
                eq("asad@example.com"), eq("/queue/orders"), any(OrderDto.class));
        verify(messagingTemplate).convertAndSend(
                eq("/topic/restaurants/3/orders"), any(OrderDto.class));
    }

    @Test
    @DisplayName("a consumer without an email is skipped, not fatal")
    void missingConsumerEmailIsSkipped() {
        order.setConsumer(User.builder().id(42L).phone("998901234567").build());

        broadcaster.broadcastStatusChange(order);

        verify(messagingTemplate, never()).convertAndSendToUser(
                any(), any(), any(OrderDto.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/orders/77"), any(OrderDto.class));
    }

    @Test
    @DisplayName("never throws — a status change has already happened")
    void neverThrows() {
        when(orderMapper.toDto(any(Order.class))).thenThrow(new RuntimeException("mapping blew up"));

        broadcaster.broadcastStatusChange(order);

        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(OrderDto.class));
    }
}
