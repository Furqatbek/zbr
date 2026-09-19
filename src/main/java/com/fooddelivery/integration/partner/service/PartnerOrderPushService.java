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
import com.fooddelivery.restaurant.entity.ItemVariant;
import com.fooddelivery.restaurant.entity.MenuItem;
import com.fooddelivery.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
                .restaurantId(parseVenueId(venueId))
                // Assigned once at creation and never changed, which is exactly
                // what an idempotency key has to be.
                .externalOrderId(order.getExternalOrderNo())
                .orderType(order.getOrderType() != null ? order.getOrderType().name() : null)
                .paymentMode(order.getPaymentMode() != null ? order.getPaymentMode().name() : null)
                .customer(OutboundOrder.Customer.builder()
                        .name(order.getCustomerName())
                        .phone(order.getCustomerPhone())
                        .build())
                .delivery(order.getDeliveryAddress() == null ? null
                        : OutboundOrder.Delivery.builder()
                                .address(order.getDeliveryAddress())
                                .latitude(order.getDeliveryLatitude())
                                .longitude(order.getDeliveryLongitude())
                                .instructions(order.getDeliveryInstructions())
                                .build())
                .items(items)
                .expectedTotal(reconcilableTotal(order))
                .subtotal(order.getSubtotal())
                .deliveryFee(order.getDeliveryFee())
                .build();
    }

    /**
     * The figure that has to reconcile between us and the partner for this
     * ticket: the food at their published prices, plus the delivery fee.
     *
     * <p>Not our order total, which was the first reading of their mapping and
     * was wrong. Ours also carries a platform service fee and any tip — only we
     * know about — so every delivery order would have been refused for a price
     * mismatch while the prices agreed to the so'm, and their documented remedy
     * (re-pull the menu) would have fixed nothing because the menu was never
     * stale. Both sides would have hunted price drift that did not exist.
     *
     * <p>Not the food alone either: the venue is owed the delivery fee too.
     * This is deliberately the only number both sides can compute from the same
     * inputs — their menu — which is what makes a check on it mean anything.
     *
     * <p>A discount we fund is not deducted. A promotion of ours does not
     * reduce what the venue is owed for the food they cooked.
     */
    private java.math.BigDecimal reconcilableTotal(Order order) {
        java.math.BigDecimal subtotal = order.getSubtotal() != null
                ? order.getSubtotal() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal deliveryFee = order.getDeliveryFee() != null
                ? order.getDeliveryFee() : java.math.BigDecimal.ZERO;
        return subtotal.add(deliveryFee);
    }

    /**
     * Their venue id is stored as text because not every partner numbers their
     * venues, but Restos read it as a number. An unparseable one is left null
     * rather than guessed: their API refuses the order, which is the visible
     * failure we want, where a wrong venue id would print in someone else's
     * kitchen.
     */
    private Long parseVenueId(String venueId) {
        try {
            return venueId == null ? null : Long.parseLong(venueId.trim());
        } catch (NumberFormatException e) {
            log.error("Venue id '{}' is not numeric; the partner will refuse this order", venueId);
            return null;
        }
    }

    private OutboundOrder.Item toItem(OrderItem item) {
        MenuItem menuItem = item.getMenuItem();
        return OutboundOrder.Item.builder()
                // Their product id, stamped on the item when we imported their
                // menu. Null means an item they have never heard of, which
                // PartnerOrderGuard stops being ordered at all.
                .productId(menuItem != null ? menuItem.getExternalId() : null)
                // Their variant id, not ours. A dish sold by size is refused
                // outright without it.
                .variantId(externalVariantId(menuItem, item.getVariantId()))
                .quantity(item.getQuantity())
                .specialInstructions(item.getSpecialInstructions())
                .name(item.getItemName())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getTotalPrice())
                .build();
    }

    /**
     * Translate the variant our order line records into the partner's id.
     *
     * <p>The order stores OUR variant id, which means nothing to their kitchen.
     * The mapping lives on the variant itself, put there by the menu import.
     */
    private Long externalVariantId(MenuItem menuItem, Long ourVariantId) {
        if (menuItem == null || ourVariantId == null || menuItem.getVariants() == null) {
            return null;
        }
        return menuItem.getVariants().stream()
                .filter(variant -> ourVariantId.equals(variant.getId()))
                .map(ItemVariant::getExternalId)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
