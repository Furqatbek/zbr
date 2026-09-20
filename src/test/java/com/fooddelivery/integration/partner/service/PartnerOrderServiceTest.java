package com.fooddelivery.integration.partner.service;

import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.integration.partner.dto.PartnerOrderStatus;
import com.fooddelivery.integration.partner.entity.Partner;
import com.fooddelivery.integration.partner.entity.PartnerCapability;
import com.fooddelivery.integration.partner.security.PartnerPrincipal;
import com.fooddelivery.order.dto.CancelOrderRequest;
import com.fooddelivery.order.dto.OrderDto;
import com.fooddelivery.order.dto.UpdateOrderStatusRequest;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.order.service.OrderService;
import com.fooddelivery.restaurant.entity.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Order progress reported by a venue's till.
 *
 * <p>The retry behaviour here is load-bearing rather than polite: the partner
 * calls us over the internet and retries on timeout, so a second delivery of
 * the same message must not be the thing that breaks an order someone is
 * cooking.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Partner order status")
class PartnerOrderServiceTest {

    private static final String REF = "FD-20260919-A7K2M9";

    @Mock private OrderRepository orderRepository;
    @Mock private OrderService orderService;
    @Mock private PartnerAccessService accessService;

    private PartnerOrderService service;
    private PartnerPrincipal restos;

    @BeforeEach
    void setUp() {
        service = new PartnerOrderService(orderRepository, orderService, accessService);
        restos = new PartnerPrincipal(
                Partner.builder().id(7L).code("RESTOS").active(true).build(), 1L);
        when(orderService.getOrderById(anyLong())).thenReturn(mock(OrderDto.class));
        when(orderService.updateOrderStatus(anyLong(), any(), anyBoolean(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(mock(OrderDto.class));
        when(orderService.cancelOrder(anyLong(), any(), any(), anyBoolean(), any()))
                .thenReturn(mock(OrderDto.class));
    }

    private Order order(OrderStatus status) {
        Order order = mock(Order.class);
        when(order.getId()).thenReturn(5L);
        when(order.getExternalOrderNo()).thenReturn(REF);
        when(order.getStatus()).thenReturn(status);
        when(order.getRestaurant()).thenReturn(Restaurant.builder().id(100L).build());
        when(order.canTransitionTo(any())).thenAnswer(i -> status.canTransitionTo(i.getArgument(0)));
        when(orderRepository.findByExternalOrderNo(REF)).thenReturn(Optional.of(order));
        return order;
    }

    @Test
    @DisplayName("an acceptance goes through the normal status change, not around it")
    void acceptanceUsesOrderService() {
        order(OrderStatus.CREATED);

        service.report(restos, REF, PartnerOrderStatus.ACCEPTED, null);

        // Through OrderService so the customer, the courier and the vendor app
        // all hear about it. Setting the status directly would give
        // partner-driven orders a second, silent lifecycle.
        ArgumentCaptor<UpdateOrderStatusRequest> captor =
                ArgumentCaptor.forClass(UpdateOrderStatusRequest.class);
        verify(orderService).updateOrderStatus(eq(5L), captor.capture(),
                eq(false), eq(true), eq(false), eq(7L));
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test
    @DisplayName("reporting a status the order already has is a no-op, not an error")
    void replayIsIdempotent() {
        // The retry case. Their call to us timed out after we committed; they
        // send it again. Failing here would turn a network blip into a stuck
        // order.
        order(OrderStatus.ACCEPTED);

        service.report(restos, REF, PartnerOrderStatus.ACCEPTED, null);

        verify(orderService, never()).updateOrderStatus(
                anyLong(), any(), anyBoolean(), anyBoolean(), anyBoolean(), any());
        verify(orderService, never()).cancelOrder(anyLong(), any(), any(), anyBoolean(), any());
    }

    @Test
    @DisplayName("a status the order cannot reach is a conflict naming both")
    void impossibleTransitionConflicts() {
        order(OrderStatus.DELIVERED);

        assertThatThrownBy(() -> service.report(restos, REF, PartnerOrderStatus.ACCEPTED, null))
                .isInstanceOf(PartnerStateConflictException.class)
                // Both statuses, so a genuine disagreement is diagnosable
                // rather than a bare rejection.
                .hasMessageContaining("DELIVERED")
                .hasMessageContaining("ACCEPTED");
    }

    @Test
    @DisplayName("a decline cancels and refunds rather than just setting a status")
    void declineCancelsAndRefunds() {
        order(OrderStatus.ACCEPTED);

        service.report(restos, REF, PartnerOrderStatus.DECLINED, "Out of lamb");

        ArgumentCaptor<CancelOrderRequest> captor = ArgumentCaptor.forClass(CancelOrderRequest.class);
        // cancelOrder, not updateStatus: that is what returns the customer's money.
        verify(orderService).cancelOrder(eq(5L), captor.capture(), any(), eq(true), eq(7L));
        assertThat(captor.getValue().getReason()).isEqualTo("Out of lamb");
    }

    @Test
    @DisplayName("a decline mid-cook is allowed — the customer's window does not bind the venue")
    void declineWhileCookingIsAllowed() {
        order(OrderStatus.PREPARING);

        service.report(restos, REF, PartnerOrderStatus.DECLINED, "Kitchen fire");

        // onBehalfOfBusiness = true. A venue that has to stop must be able to,
        // however far along the order is.
        verify(orderService).cancelOrder(eq(5L), any(), any(), eq(true), eq(7L));
    }

    @Test
    @DisplayName("a decline with no reason still says something to the customer")
    void declineWithoutReasonGetsADefault() {
        order(OrderStatus.CREATED);

        service.report(restos, REF, PartnerOrderStatus.DECLINED, "   ");

        ArgumentCaptor<CancelOrderRequest> captor = ArgumentCaptor.forClass(CancelOrderRequest.class);
        verify(orderService).cancelOrder(anyLong(), captor.capture(), any(), anyBoolean(), any());
        assertThat(captor.getValue().getReason()).isNotBlank();
    }

    @Test
    @DisplayName("a change they drove is stamped with their id, so it is not echoed back")
    void partnerOriginIsStamped() {
        // The stamp is what stops us reporting their own kitchen state back to
        // the kitchen that set it — while still forwarding one a restaurant set
        // on our vendor app, which carries no stamp.
        order(OrderStatus.CREATED);

        service.report(restos, REF, PartnerOrderStatus.ACCEPTED, null);

        verify(orderService).updateOrderStatus(anyLong(), any(), anyBoolean(), anyBoolean(),
                anyBoolean(), eq(7L));
    }

    @Test
    @DisplayName("the capability is checked against the order's own restaurant")
    void capabilityCheckedAgainstTheOrder() {
        order(OrderStatus.CREATED);

        service.report(restos, REF, PartnerOrderStatus.ACCEPTED, null);

        // Against the restaurant on the order, never against anything the
        // caller supplied — that is what stops one partner reporting on
        // another's orders.
        verify(accessService).requireCapabilityOnRestaurant(
                restos, 100L, PartnerCapability.ORDER_STATUS_WRITE);
    }

    @Test
    @DisplayName("an unknown order reference is not found")
    void unknownOrderIsNotFound() {
        when(orderRepository.findByExternalOrderNo(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.report(restos, "FD-19990101-ZZZZZZ",
                PartnerOrderStatus.ACCEPTED, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("every reportable status maps to one we actually have")
    void allReportedStatusesMap() {
        for (PartnerOrderStatus status : PartnerOrderStatus.values()) {
            assertThat(status.toOrderStatus()).isNotNull();
        }
        // DECLINED has no status of its own — an order nobody will cook is
        // cancelled, and the refusal lives in the cancellation reason.
        assertThat(PartnerOrderStatus.DECLINED.toOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(PartnerOrderStatus.DECLINED.isDecline()).isTrue();
        assertThat(PartnerOrderStatus.ACCEPTED.isDecline()).isFalse();
    }
}
