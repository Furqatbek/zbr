package com.fooddelivery.integration.partner.service;

import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.integration.partner.dto.PartnerOrderStatus;
import com.fooddelivery.integration.partner.entity.PartnerCapability;
import com.fooddelivery.integration.partner.security.PartnerPrincipal;
import com.fooddelivery.order.dto.CancelOrderRequest;
import com.fooddelivery.order.dto.OrderDto;
import com.fooddelivery.order.dto.UpdateOrderStatusRequest;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order progress reported by the venue's own till.
 *
 * <p>The partner reports in their vocabulary against our order reference, and
 * this translates it into a platform status change that goes through the same
 * service every other actor uses — so a partner acceptance broadcasts to the
 * customer, the courier and the vendor app exactly as a tap in the vendor app
 * would. Bypassing {@link OrderService} to set the status directly would give
 * partner-driven orders a silent, second lifecycle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerOrderService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final PartnerAccessService accessService;

    @Transactional
    public OrderDto report(PartnerPrincipal principal, String externalOrderNo,
                           PartnerOrderStatus reported, String reason) {

        Order order = orderRepository.findByExternalOrderNo(externalOrderNo)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Checked against the ORDER's restaurant, not against anything the
        // caller supplied. Order references are ours and guessable in shape, so
        // this is what stops one partner reporting status on another's orders.
        accessService.requireCapabilityOnRestaurant(
                principal, order.getRestaurant().getId(), PartnerCapability.ORDER_STATUS_WRITE);

        OrderStatus target = reported.toOrderStatus();

        // Safe to retry, by design and by agreement: reporting a state the
        // order is already in succeeds and changes nothing, rather than failing
        // on a "cannot go from ACCEPTED to ACCEPTED" rule. Their side does the
        // same for us. A partner's retry after a timeout must never be the
        // thing that breaks an order.
        if (order.getStatus() == target) {
            log.debug("Partner {} reported {} on order {}, already there — no-op",
                    principal.getPartnerCode(), reported, externalOrderNo);
            return orderService.getOrderById(order.getId());
        }

        if (!order.canTransitionTo(target)) {
            // A genuine disagreement about where the order is, which must be
            // visible rather than silently applied — so both statuses are named.
            throw new PartnerStateConflictException(
                    "Order " + externalOrderNo + " is " + order.getStatus()
                            + " here and cannot move to " + target
                            + " (you reported " + reported + ")");
        }

        if (reported.isDecline()) {
            // A decline is a cancellation with the venue's authority behind it,
            // so it goes through cancelOrder rather than a bare status change:
            // that is what refunds the customer. onBehalfOfBusiness, because a
            // venue may refuse an order it has already started cooking — the
            // customer cancellation window does not apply to them.
            log.info("Partner {} declined order {}: {}",
                    principal.getPartnerCode(), externalOrderNo, reason);
            return orderService.cancelOrder(order.getId(),
                    CancelOrderRequest.builder()
                            .reason(reason != null && !reason.isBlank()
                                    ? reason
                                    : "Declined by the restaurant")
                            .build(),
                    null, true, principal.getPartnerId());
        }

        log.info("Partner {} reported {} on order {}", principal.getPartnerCode(), reported, externalOrderNo);
        // Stamped with the partner who reported it, so we do not send their own
        // kitchen state straight back to the kitchen that just set it.
        return orderService.updateOrderStatus(order.getId(),
                UpdateOrderStatusRequest.builder().status(target).build(),
                false, true, false, principal.getPartnerId());
    }
}
