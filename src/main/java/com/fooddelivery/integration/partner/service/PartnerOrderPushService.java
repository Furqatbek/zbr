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
import com.fooddelivery.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Sending an order to the partner whose till the venue cooks from.
 *
 * <p>Never called in the order-creation request path. If a partner's system is
 * slow or down, the customer's order must still be taken — the food arriving
 * late is recoverable, the order not existing is not.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerOrderPushService {

    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private final OrderRepository orderRepository;
    private final PartnerVenueGrantRepository grantRepository;
    private final PartnerOrderPushRepository pushRepository;
    private final PartnerOrderPushClient client;

    /**
     * Push one order, if its venue is switched on for it.
     *
     * @return false when the push failed in a way worth retrying, so the caller
     *         can leave the message for redelivery. True means there is nothing
     *         more to do — delivered, rejected, or never applicable.
     */
    @Transactional
    public boolean push(Long orderId) {
        Optional<Order> found = orderRepository.findById(orderId);
        if (found.isEmpty()) {
            log.warn("Cannot push order {} to a partner: no such order", orderId);
            return true;
        }
        Order order = found.get();

        Optional<PartnerVenueGrant> grant = pushableGrant(order.getRestaurant().getId());
        if (grant.isEmpty()) {
            // The ordinary case for most restaurants. Not an error, and not
            // worth a row — the overwhelming majority of orders never go near a
            // partner.
            return true;
        }

        Partner partner = grant.get().getPartner();
        PartnerOrderPush record = pushRepository
                .findByOrderIdAndPartnerId(orderId, partner.getId())
                .orElseGet(() -> PartnerOrderPush.builder()
                        .orderId(orderId)
                        .partnerId(partner.getId())
                        .externalOrderNo(order.getExternalOrderNo())
                        .status(PartnerPushStatus.PENDING)
                        .attempts(0)
                        .build());

        // The guard against a second ticket. A redelivered message, a retry
        // after a timeout and a manual replay all land here, and a double print
        // is a double-cooked order rather than a cosmetic bug. Their
        // idempotency key is the backstop; this is the near one.
        if (record.getStatus().isTerminal()) {
            log.debug("Order {} already {} for partner {} — not pushing again",
                    order.getExternalOrderNo(), record.getStatus(), partner.getCode());
            return true;
        }

        record.setAttempts(record.getAttempts() + 1);
        PartnerOrderPushClient.Result result =
                client.push(partner, build(order, grant.get().getExternalVenueId()));

        if (result instanceof PartnerOrderPushClient.Result.Accepted accepted) {
            record.setStatus(PartnerPushStatus.DELIVERED);
            record.setPartnerOrderId(accepted.partnerOrderId());
            record.setDeliveredAt(LocalDateTime.now());
            record.setLastError(null);
            pushRepository.save(record);
            log.info("Order {} is on {}'s till{} (their id {})",
                    order.getExternalOrderNo(), partner.getCode(),
                    accepted.duplicate() ? ", already was" : "", accepted.partnerOrderId());
            return true;
        }

        if (result instanceof PartnerOrderPushClient.Result.Rejected rejected) {
            record.setStatus(PartnerPushStatus.REJECTED);
            record.setLastError(rejected.reason());
            pushRepository.save(record);
            // Loud: an order the kitchen will never see, that the customer has
            // already placed and may have paid for. Retrying would only bury it.
            log.error("PARTNER ORDER REJECTED — order {} will not reach {}'s kitchen: {}",
                    order.getExternalOrderNo(), partner.getCode(), rejected.reason());
            return true;
        }

        PartnerOrderPushClient.Result.Retryable retryable = (PartnerOrderPushClient.Result.Retryable) result;
        record.setStatus(PartnerPushStatus.FAILED);
        record.setLastError(retryable.reason());
        pushRepository.save(record);
        log.warn("Order {} not yet on {}'s till (attempt {}): {}",
                order.getExternalOrderNo(), partner.getCode(), record.getAttempts(), retryable.reason());
        return false;
    }

    /**
     * The grant that sends this restaurant's orders somewhere, if there is one
     * and everything it needs is configured.
     */
    @Transactional(readOnly = true)
    public Optional<PartnerVenueGrant> pushableGrant(Long restaurantId) {
        return grantRepository.findByRestaurantIdAndPushOrdersTrue(restaurantId)
                .filter(grant -> {
                    Partner partner = grant.getPartner();
                    boolean ready = Boolean.TRUE.equals(partner.getActive())
                            && partner.getOutboundBaseUrl() != null
                            && !partner.getOutboundBaseUrl().isBlank();
                    if (!ready) {
                        // A venue switched on against a partner we cannot call
                        // is a misconfiguration, and the symptom is orders
                        // quietly not printing.
                        log.error("Restaurant {} is set to push orders to {}, which is inactive or "
                                        + "has no outbound URL. Orders are NOT reaching that kitchen.",
                                restaurantId, partner.getCode());
                    }
                    return ready;
                });
    }

    OutboundOrder build(Order order, String venueId) {
        List<OutboundOrder.Item> items = order.getItems() == null ? List.of()
                : order.getItems().stream().map(this::toItem).toList();

        return OutboundOrder.builder()
                // Assigned once at creation and never changed, which is exactly
                // what an idempotency key has to be.
                .externalOrderId(order.getExternalOrderNo())
                .venueId(venueId)
                .orderType(order.getOrderType() != null ? order.getOrderType().name() : null)
                .items(items)
                // Their validation compares this against what their own prices
                // add up to, and refuses the order if it disagrees. The food
                // only: our total carries delivery, tip and an 8% tax line,
                // none of which exist in the menu they published.
                .expectedTotal(order.getSubtotal())
                .subtotal(order.getSubtotal())
                .deliveryFee(order.getDeliveryFee())
                .total(order.getTotal())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryInstructions(order.getDeliveryInstructions())
                .notes(order.getNotes())
                .placedAt(order.getCreatedAt() != null
                        ? order.getCreatedAt().atOffset(ZoneOffset.UTC).format(ISO_UTC) : null)
                .build();
    }

    private OutboundOrder.Item toItem(OrderItem item) {
        return OutboundOrder.Item.builder()
                // Their product id, stamped on the item when we imported their
                // menu. Null here means an item they have never heard of, which
                // their API refuses for the whole basket — PartnerOrderGuard
                // stops such an order being placed at all.
                .productId(item.getMenuItem() != null && item.getMenuItem().getExternalId() != null
                        ? String.valueOf(item.getMenuItem().getExternalId()) : null)
                .name(item.getItemName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getTotalPrice())
                .notes(item.getSpecialInstructions())
                .build();
    }
}
