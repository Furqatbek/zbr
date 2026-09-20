package com.fooddelivery.integration.partner.service;

import com.fooddelivery.integration.partner.client.PartnerOrderPushClient;
import com.fooddelivery.integration.partner.entity.Partner;
import com.fooddelivery.integration.partner.entity.PartnerOrderPush;
import com.fooddelivery.integration.partner.entity.PartnerPushStatus;
import com.fooddelivery.integration.partner.entity.PartnerVenueGrant;
import com.fooddelivery.integration.partner.repository.PartnerOrderPushRepository;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.restaurant.entity.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * What a venue's till hears about an order it is cooking.
 *
 * <p>Most of this is about what NOT to send, and the rule is ORIGIN rather than
 * state. A status the partner reported is theirs and goes nowhere; the same
 * status set on our own vendor app is news their till needs. Getting that
 * backwards — excluding kitchen states outright, as this did — leaves a
 * restaurant that accepts on our tablet with a till still showing the order
 * waiting.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Partner status reports")
class PartnerStatusReportServiceTest {

    private static final String REF = "FD-20260919-A7K2M9";

    @Mock private PartnerOrderPushService pushService;
    @Mock private PartnerOrderPushRepository pushRepository;
    @Mock private PartnerOrderPushClient client;

    private PartnerStatusReportService service;
    private Partner partner;

    @BeforeEach
    void setUp() {
        service = new PartnerStatusReportService(pushService, pushRepository, client);
        partner = Partner.builder().id(7L).code("RESTOS").active(true)
                .outboundBaseUrl("https://pos.example.com").build();

        when(pushService.pushableGrant(anyLong())).thenReturn(Optional.of(PartnerVenueGrant.builder()
                .partner(partner).restaurant(Restaurant.builder().id(100L).build())
                .externalVenueId("55").pushOrders(true).build()));
        when(pushRepository.findByOrderIdAndPartnerId(anyLong(), anyLong()))
                .thenReturn(Optional.of(PartnerOrderPush.builder()
                        .orderId(5L).partnerId(7L).externalOrderNo(REF)
                        .status(PartnerPushStatus.DELIVERED).build()));
        when(client.reportStatus(any(), anyString(), any(), anyString(), any(), anyString()))
                .thenReturn(new PartnerOrderPushClient.Result.Accepted(null, false));
    }

    @Test
    @DisplayName("the delivery states are reported")
    void deliveryStatesReported() {
        // What they cannot see for themselves: whether a courier came, took it,
        // and arrived. Without these a counter is left wondering.
        for (OrderStatus status : new OrderStatus[]{
                OrderStatus.COURIER_ASSIGNED, OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT,
                OrderStatus.DELIVERED, OrderStatus.COMPLETED, OrderStatus.CANCELLED}) {

            assertThat(service.report(5L, 100L, REF, status, null))
                    .as("reporting %s", status).isTrue();
        }
        verify(client, org.mockito.Mockito.times(6))
                .reportStatus(any(), anyString(), any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("a kitchen state they reported is not sent back to them")
    void theirOwnStatesNotEchoed() {
        // Telling a kitchen what it just did. Partner id 7 is the one that
        // reported it, and the one that would receive it.
        for (OrderStatus status : new OrderStatus[]{
                OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY,
                OrderStatus.CANCELLED}) {

            assertThat(service.report(5L, 100L, REF, status, null, 7L)).isTrue();
        }
        verify(client, never()).reportStatus(any(), anyString(), any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("a kitchen state set on OUR tablet does reach their till")
    void ourKitchenStatesAreReported() {
        // THE bug this fixes. A restaurant can accept on our vendor app instead
        // of their till. Without this their screen shows an order still waiting
        // while ours shows it cooking, and the two never converge.
        for (OrderStatus status : new OrderStatus[]{
                OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY}) {

            assertThat(service.report(5L, 100L, REF, status, null, null))
                    .as("reporting %s", status).isTrue();
        }
        verify(client, org.mockito.Mockito.times(3))
                .reportStatus(any(), anyString(), any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("a state reported by a DIFFERENT partner is still sent")
    void anotherPartnersReportIsStillForwarded() {
        // Origin is matched by partner, not by "came from some partner". A
        // venue on two systems must still have both kept in step.
        assertThat(service.report(5L, 100L, REF, OrderStatus.ACCEPTED, null, 99L)).isTrue();

        verify(client).reportStatus(any(), anyString(), any(), eq("ACCEPTED"), any(), anyString());
    }

    @Test
    @DisplayName("CREATED is never reported — we pushed the order, they know")
    void createdNotReported() {
        assertThat(service.report(5L, 100L, REF, OrderStatus.CREATED, null)).isTrue();

        verify(client, never()).reportStatus(any(), anyString(), any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("REFUNDED is not an order state on their side and is not sent")
    void refundedNotReported() {
        // Their words: a fact about money, not about the order. Forcing it onto
        // an order state would mark a delivered order cancelled.
        assertThat(service.report(5L, 100L, REF, OrderStatus.REFUNDED, null)).isTrue();

        verify(client, never()).reportStatus(any(), anyString(), any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("nothing is reported for a venue that is not on a partner till")
    void noGrantNoReport() {
        when(pushService.pushableGrant(anyLong())).thenReturn(Optional.empty());

        assertThat(service.report(5L, 100L, REF, OrderStatus.DELIVERED, null)).isTrue();
        verify(client, never()).reportStatus(any(), anyString(), any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("nothing is reported for an order that never reached their till")
    void undeliveredOrderNotReported() {
        // THE guard worth having. They would refuse a status on an order they
        // do not have, and that refusal would bury the real failure — the push
        // — under a second one.
        when(pushRepository.findByOrderIdAndPartnerId(anyLong(), anyLong()))
                .thenReturn(Optional.of(PartnerOrderPush.builder()
                        .status(PartnerPushStatus.REJECTED).build()));

        assertThat(service.report(5L, 100L, REF, OrderStatus.DELIVERED, null)).isTrue();
        verify(client, never()).reportStatus(any(), anyString(), any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("a transient failure asks to be retried")
    void retryableAsksForRedelivery() {
        when(client.reportStatus(any(), anyString(), any(), anyString(), any(), anyString()))
                .thenReturn(new PartnerOrderPushClient.Result.Retryable("connection timed out"));

        assertThat(service.report(5L, 100L, REF, OrderStatus.DELIVERED, null)).isFalse();
    }

    @Test
    @DisplayName("a refusal is logged and dropped, not retried")
    void refusalIsSettled() {
        // The food is already cooked and delivered. What is lost is the venue's
        // view of the order closing — worth a log, not worth a queue.
        when(client.reportStatus(any(), anyString(), any(), anyString(), any(), anyString()))
                .thenReturn(new PartnerOrderPushClient.Result.Rejected("409 already DELIVERED"));

        assertThat(service.report(5L, 100L, REF, OrderStatus.DELIVERED, null)).isTrue();
        verify(pushRepository, never()).save(any());
    }

    @Test
    @DisplayName("a cancellation they refuse is recorded as food the venue is owed for")
    void refusedCancellationIsRecorded() {
        // Their cutoff. Our order is cancelled and the customer refunded either
        // way; their 422 says the kitchen had already started, so the food
        // exists and somebody carries its cost. Recorded rather than logged,
        // because the commercial answer does not exist yet and a log line
        // cannot be settled against later.
        when(client.reportStatus(any(), anyString(), any(), anyString(), any(), anyString()))
                .thenReturn(new PartnerOrderPushClient.Result.Rejected(
                        "422 CANCELLATION_WINDOW_CLOSED (PREPARING)"));

        assertThat(service.report(5L, 100L, REF, OrderStatus.CANCELLED, "Customer changed mind"))
                .isTrue();

        verify(pushRepository).save(org.mockito.ArgumentMatchers.argThat(record ->
                record.getVenueOwedAt() != null
                        && record.getVenueOwedReason().contains("CANCELLATION_WINDOW_CLOSED")));
    }

    @Test
    @DisplayName("a refused cancellation is not counted twice")
    void refusedCancellationIsNotDoubleCounted() {
        // A redelivered message must not make the venue look owed for two
        // meals when they cooked one.
        when(pushRepository.findByOrderIdAndPartnerId(anyLong(), anyLong()))
                .thenReturn(Optional.of(PartnerOrderPush.builder()
                        .orderId(5L).partnerId(7L).externalOrderNo(REF)
                        .status(PartnerPushStatus.DELIVERED)
                        .venueOwedAt(java.time.LocalDateTime.now())
                        .venueOwedReason("422 CANCELLATION_WINDOW_CLOSED")
                        .build()));
        when(client.reportStatus(any(), anyString(), any(), anyString(), any(), anyString()))
                .thenReturn(new PartnerOrderPushClient.Result.Rejected("422 CANCELLATION_WINDOW_CLOSED"));

        service.report(5L, 100L, REF, OrderStatus.CANCELLED, null);

        verify(pushRepository, never()).save(any());
    }

    @Test
    @DisplayName("a refused cancellation is never retried")
    void refusedCancellationIsNotRetried() {
        // Not an integration fault. Sending it again would only produce the
        // same refusal, and would bury it.
        when(client.reportStatus(any(), anyString(), any(), anyString(), any(), anyString()))
                .thenReturn(new PartnerOrderPushClient.Result.Rejected("422 CANCELLATION_WINDOW_CLOSED"));

        assertThat(service.report(5L, 100L, REF, OrderStatus.CANCELLED, null)).isTrue();
    }

    @Test
    @DisplayName("the venue is named by THEIR id")
    void venueIdIsTheirs() {
        service.report(5L, 100L, REF, OrderStatus.DELIVERED, null);

        verify(client).reportStatus(any(), eq(REF), eq("55"), eq("DELIVERED"), any(), anyString());
    }

    @Test
    @DisplayName("a cancellation carries its reason, for the venue to read")
    void cancellationCarriesReason() {
        service.report(5L, 100L, REF, OrderStatus.CANCELLED, "Customer changed their mind");

        verify(client).reportStatus(any(), anyString(), any(), eq("CANCELLED"),
                eq("Customer changed their mind"), anyString());
    }
}
