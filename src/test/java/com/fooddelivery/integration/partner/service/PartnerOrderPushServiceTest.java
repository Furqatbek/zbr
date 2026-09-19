package com.fooddelivery.integration.partner.service;

import com.fooddelivery.integration.partner.client.PartnerOrderPushClient;
import com.fooddelivery.integration.partner.dto.OutboundOrder;
import com.fooddelivery.integration.partner.entity.Partner;
import com.fooddelivery.integration.partner.entity.PartnerOrderPush;
import com.fooddelivery.integration.partner.entity.PartnerPushStatus;
import com.fooddelivery.integration.partner.entity.PartnerVenueGrant;
import com.fooddelivery.integration.partner.repository.PartnerOrderPushRepository;
import com.fooddelivery.integration.partner.repository.PartnerVenueGrantRepository;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderItem;
import com.fooddelivery.order.entity.OrderType;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.restaurant.entity.MenuItem;
import com.fooddelivery.restaurant.entity.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Getting an order onto a partner's till.
 *
 * <p>Two things here are worth more than the happy path. A second ticket means
 * a second cooked meal, so the duplicate guard has to hold across retries and
 * redeliveries. And a rejection retried forever hides an order nobody will cook
 * behind a queue that looks merely busy.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Partner order push")
class PartnerOrderPushServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private PartnerVenueGrantRepository grantRepository;
    @Mock private PartnerOrderPushRepository pushRepository;
    @Mock private PartnerOrderPushClient client;

    private PartnerOrderPushService service;
    private Partner partner;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        service = new PartnerOrderPushService(orderRepository, grantRepository, pushRepository, client);

        partner = Partner.builder().id(7L).code("RESTOS").active(true)
                .outboundBaseUrl("https://pos.example.com").build();
        restaurant = Restaurant.builder().id(100L).name("Osh Markazi").build();

        when(pushRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(pushRepository.findByOrderIdAndPartnerId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(grantRepository.findByRestaurantIdAndPushOrdersTrue(anyLong())).thenReturn(Optional.empty());
    }

    private Order order() {
        MenuItem menuItem = MenuItem.builder().id(1L).name("Plov")
                .externalId(4417L).externalSource("RESTOS").build();
        OrderItem line = OrderItem.builder()
                .menuItem(menuItem).itemName("Plov").quantity(2)
                .unitPrice(new BigDecimal("30000")).totalPrice(new BigDecimal("60000"))
                .build();

        Order order = Order.builder()
                .id(5L).externalOrderNo("FD-20260919-A7K2M9")
                .restaurant(restaurant).orderType(OrderType.DELIVERY)
                .items(new java.util.ArrayList<>(List.of(line)))
                .subtotal(new BigDecimal("60000")).total(new BigDecimal("75000"))
                .customerName("Anvar").customerPhone("998901234567")
                .deliveryAddress("Mustaqillik 15")
                .createdAt(LocalDateTime.of(2026, 9, 19, 10, 2, 11))
                .build();
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        return order;
    }

    private void pushEnabled() {
        when(grantRepository.findByRestaurantIdAndPushOrdersTrue(100L))
                .thenReturn(Optional.of(PartnerVenueGrant.builder()
                        .id(1L).partner(partner).restaurant(restaurant)
                        .externalVenueId("55").pushOrders(true).capabilities(Set.of())
                        .build()));
    }

    @Nested
    @DisplayName("when the venue is not on a partner till")
    class NotAPartnerVenue {

        @Test
        @DisplayName("nothing is sent and nothing is recorded")
        void noGrantIsANoOp() {
            order();

            assertThat(service.push(5L)).isTrue();

            // Most orders on the platform take this path. A row per order here
            // would be a table the size of orders, recording nothing.
            verify(client, never()).push(any(), any());
            verify(pushRepository, never()).save(any());
        }

        @Test
        @DisplayName("a venue switched on against an unreachable partner sends nothing")
        void misconfiguredPartnerSendsNothing() {
            order();
            partner.setOutboundBaseUrl(null);
            pushEnabled();

            // Switched on but with nowhere to send. The symptom would otherwise
            // be orders quietly not printing, so the service logs an error and
            // refuses to pretend.
            assertThat(service.push(5L)).isTrue();
            verify(client, never()).push(any(), any());
        }
    }

    @Nested
    @DisplayName("delivery")
    class Delivery {

        @Test
        @DisplayName("an accepted push is recorded as delivered")
        void accepted() {
            order();
            pushEnabled();
            when(client.push(any(), any()))
                    .thenReturn(new PartnerOrderPushClient.Result.Accepted("POS-9912", false));

            assertThat(service.push(5L)).isTrue();

            verify(pushRepository).save(org.mockito.ArgumentMatchers.argThat(record ->
                    record.getStatus() == PartnerPushStatus.DELIVERED
                            && "POS-9912".equals(record.getPartnerOrderId())
                            && record.getDeliveredAt() != null
                            && record.getAttempts() == 1));
        }

        @Test
        @DisplayName("their duplicate:true is a success, not a failure")
        void duplicateIsSuccess() {
            // Their contract for a replay. Treating it as a failure would have
            // us retry forever against an order that is already cooking.
            order();
            pushEnabled();
            when(client.push(any(), any()))
                    .thenReturn(new PartnerOrderPushClient.Result.Accepted("POS-9912", true));

            assertThat(service.push(5L)).isTrue();
            verify(pushRepository).save(org.mockito.ArgumentMatchers.argThat(
                    record -> record.getStatus() == PartnerPushStatus.DELIVERED));
        }

        @Test
        @DisplayName("an order already delivered is never pushed a second time")
        void deliveredOrderIsNotPushedAgain() {
            // THE guard. A redelivered message, a retry after a timeout and a
            // manual replay all arrive here, and a second ticket is a second
            // cooked meal.
            order();
            pushEnabled();
            when(pushRepository.findByOrderIdAndPartnerId(5L, 7L))
                    .thenReturn(Optional.of(PartnerOrderPush.builder()
                            .orderId(5L).partnerId(7L).externalOrderNo("FD-20260919-A7K2M9")
                            .status(PartnerPushStatus.DELIVERED).attempts(1)
                            .build()));

            assertThat(service.push(5L)).isTrue();
            verify(client, never()).push(any(), any());
        }

        @Test
        @DisplayName("a rejected order is not pushed again either")
        void rejectedOrderIsNotRetried() {
            order();
            pushEnabled();
            when(pushRepository.findByOrderIdAndPartnerId(5L, 7L))
                    .thenReturn(Optional.of(PartnerOrderPush.builder()
                            .orderId(5L).partnerId(7L).externalOrderNo("FD-20260919-A7K2M9")
                            .status(PartnerPushStatus.REJECTED).attempts(1)
                            .build()));

            assertThat(service.push(5L)).isTrue();
            verify(client, never()).push(any(), any());
        }
    }

    @Nested
    @DisplayName("failure")
    class Failure {

        @Test
        @DisplayName("a transient failure asks to be retried")
        void retryableAsksForRedelivery() {
            order();
            pushEnabled();
            when(client.push(any(), any()))
                    .thenReturn(new PartnerOrderPushClient.Result.Retryable("connection timed out"));

            // false tells the consumer to leave the message for redelivery.
            assertThat(service.push(5L)).isFalse();
            verify(pushRepository).save(org.mockito.ArgumentMatchers.argThat(record ->
                    record.getStatus() == PartnerPushStatus.FAILED
                            && record.getLastError().contains("timed out")));
        }

        @Test
        @DisplayName("a rejection is settled, not retried")
        void rejectionIsSettled() {
            order();
            pushEnabled();
            when(client.push(any(), any()))
                    .thenReturn(new PartnerOrderPushClient.Result.Rejected("422 UNKNOWN_ITEMS"));

            // true, so the message is acknowledged. Retrying a rejection hides
            // an order nobody will cook behind a queue that looks busy.
            assertThat(service.push(5L)).isTrue();
            verify(pushRepository).save(org.mockito.ArgumentMatchers.argThat(record ->
                    record.getStatus() == PartnerPushStatus.REJECTED));
        }

        @Test
        @DisplayName("attempts accumulate across retries on the same record")
        void attemptsAccumulate() {
            order();
            pushEnabled();
            PartnerOrderPush existing = PartnerOrderPush.builder()
                    .orderId(5L).partnerId(7L).externalOrderNo("FD-20260919-A7K2M9")
                    .status(PartnerPushStatus.FAILED).attempts(3)
                    .build();
            when(pushRepository.findByOrderIdAndPartnerId(5L, 7L)).thenReturn(Optional.of(existing));
            when(client.push(any(), any()))
                    .thenReturn(new PartnerOrderPushClient.Result.Retryable("still down"));

            service.push(5L);

            assertThat(existing.getAttempts()).isEqualTo(4);
        }

        @Test
        @DisplayName("an order that no longer exists is settled rather than retried forever")
        void missingOrderIsSettled() {
            when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThat(service.push(999L)).isTrue();
            verify(client, never()).push(any(), any());
        }
    }

    @Nested
    @DisplayName("the payload")
    class Payload {

        @Test
        @DisplayName("carries our order reference as the idempotency key")
        void carriesOurReference() {
            Order order = order();

            OutboundOrder payload = service.build(order, "55");

            // Assigned once at creation and never changed, which is exactly
            // what an idempotency key has to be.
            assertThat(payload.getExternalOrderId()).isEqualTo("FD-20260919-A7K2M9");
            // Their field name, and a number — they read restaurantId.
            assertThat(payload.getRestaurantId()).isEqualTo(55L);
        }

        @Test
        @DisplayName("names items by the partner's product id, not ours")
        void usesPartnerProductIds() {
            Order order = order();

            OutboundOrder payload = service.build(order, "55");

            // Our id means nothing to their kitchen, and an order whose lines
            // they cannot resolve is one they refuse outright.
            assertThat(payload.getItems()).singleElement()
                    .satisfies(item -> {
                        assertThat(item.getProductId()).isEqualTo(4417L);
                        assertThat(item.getQuantity()).isEqualTo(2);
                        assertThat(item.getName()).isEqualTo("Plov");
                    });
        }

        @Test
        @DisplayName("expectedTotal is the food at their prices plus the delivery fee")
        void expectedTotalReconciles() {
            // Food 60 000 + delivery 15 000. Their formula exactly, and the
            // only figure both sides can compute from the same menu.
            Order order = order();
            order.setDeliveryFee(new BigDecimal("15000"));
            order.setTax(new BigDecimal("4800"));
            order.setTipAmount(new BigDecimal("5000"));

            assertThat(service.build(order, "55").getExpectedTotal()).isEqualByComparingTo("75000");
        }

        @Test
        @DisplayName("our tax and tip are excluded — they cannot see them")
        void taxAndTipExcluded() {
            // The first reading of their mapping sent our order total. Every
            // delivery order would have been refused for a price mismatch while
            // the prices agreed to the so'm, and re-pulling the menu — their
            // documented remedy — would have fixed nothing.
            Order order = order();
            order.setDeliveryFee(new BigDecimal("15000"));
            order.setTax(new BigDecimal("4800"));
            order.setTipAmount(new BigDecimal("5000"));
            order.setTotal(new BigDecimal("84800"));

            assertThat(service.build(order, "55").getExpectedTotal())
                    .isNotEqualByComparingTo(order.getTotal())
                    .isEqualByComparingTo("75000");
        }

        @Test
        @DisplayName("a discount we fund does not reduce what the venue is owed")
        void ourDiscountDoesNotReduceIt() {
            // A promotion of ours is not the venue's to absorb: they cooked the
            // food at their price either way.
            Order order = order();
            order.setDeliveryFee(new BigDecimal("15000"));
            order.setDiscount(new BigDecimal("10000"));

            assertThat(service.build(order, "55").getExpectedTotal()).isEqualByComparingTo("75000");
        }

        @Test
        @DisplayName("a collection order is just the food")
        void collectionOrderHasNoDeliveryFee() {
            Order order = order();
            order.setDeliveryFee(BigDecimal.ZERO);

            assertThat(service.build(order, "55").getExpectedTotal()).isEqualByComparingTo("60000");
        }

        @Test
        @DisplayName("the customer and delivery blocks are nested, as they read them")
        void nestedBlocks() {
            OutboundOrder payload = service.build(order(), "55");

            assertThat(payload.getCustomer().getName()).isEqualTo("Anvar");
            assertThat(payload.getCustomer().getPhone()).isEqualTo("998901234567");
            assertThat(payload.getDelivery().getAddress()).isEqualTo("Mustaqillik 15");
        }

        @Test
        @DisplayName("a collection order carries no delivery block at all")
        void noDeliveryBlockWhenThereIsNoAddress() {
            Order order = order();
            order.setDeliveryAddress(null);

            // Omitted rather than sent empty: they should not have to tell a
            // null we meant from one we sent by accident.
            assertThat(service.build(order, "55").getDelivery()).isNull();
        }

        @Test
        @DisplayName("paymentMode is always sent — they refuse to guess it")
        void paymentModeIsSent() {
            // It decides whether the venue hands over food already paid for or
            // money still to collect. Their API rejects an order without it.
            assertThat(service.build(order(), "55").getPaymentMode()).isEqualTo("PREPAID");
        }

        @Test
        @DisplayName("a chosen size is named by THEIR variant id, not ours")
        void variantIdIsTheirs() {
            Order order = order();
            MenuItem menuItem = order.getItems().get(0).getMenuItem();
            menuItem.setVariants(new java.util.HashSet<>(List.of(
                    com.fooddelivery.restaurant.entity.ItemVariant.builder()
                            .id(50L).externalId(11L).externalSource("RESTOS").active(true).build())));
            order.getItems().get(0).setVariantId(50L);

            // 50 is our id and means nothing to their kitchen; 11 is theirs.
            assertThat(service.build(order, "55").getItems().get(0).getVariantId()).isEqualTo(11L);
        }

        @Test
        @DisplayName("an unparseable venue id is left null rather than guessed")
        void badVenueIdIsNull() {
            // Their API refuses the order, which is the visible failure we
            // want — a wrong venue id would print in someone else's kitchen.
            assertThat(service.build(order(), "not-a-number").getRestaurantId()).isNull();
        }

        @Test
        @DisplayName("an order with no items does not produce a null list")
        void emptyItemsIsEmptyNotNull() {
            Order order = order();
            order.setItems(null);

            assertThat(service.build(order, "55").getItems()).isEmpty();
        }
    }
}
