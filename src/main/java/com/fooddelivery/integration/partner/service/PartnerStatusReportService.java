package com.fooddelivery.integration.partner.service;

import com.fooddelivery.integration.partner.client.PartnerOrderPushClient;
import com.fooddelivery.integration.partner.entity.Partner;
import com.fooddelivery.integration.partner.entity.PartnerOrderPush;
import com.fooddelivery.integration.partner.entity.PartnerPushStatus;
import com.fooddelivery.integration.partner.entity.PartnerVenueGrant;
import com.fooddelivery.integration.partner.repository.PartnerOrderPushRepository;
import com.fooddelivery.order.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Telling a partner's till what happened to an order after it left the kitchen.
 *
 * <p>Restos already know everything up to {@code READY} — they are the ones
 * doing it. What they cannot see is the delivery: whether a courier came, took
 * it, and arrived. Without that a counter is left wondering where an order went,
 * and their order list never closes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerStatusReportService {

    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * The states worth a call, which is not the same as the states we have.
     *
     * <p>Everything up to READY is theirs to report to us, not ours to them —
     * sending it back would be telling a kitchen what it just did. CREATED is
     * excluded for the same reason: we pushed the order, so they know.
     *
     * <p>REFUNDED is excluded because it is a fact about money rather than about
     * the order, and their own note says forcing it onto an order state would
     * mark a delivered order cancelled.
     */
    private static final Set<OrderStatus> REPORTABLE = EnumSet.of(
            OrderStatus.COURIER_ASSIGNED,
            OrderStatus.PICKED_UP,
            OrderStatus.IN_TRANSIT,
            OrderStatus.DELIVERED,
            OrderStatus.COMPLETED,
            OrderStatus.CANCELLED);

    private final PartnerOrderPushService pushService;
    private final PartnerOrderPushRepository pushRepository;
    private final PartnerOrderPushClient client;

    /**
     * @return false when the report failed in a way worth retrying.
     */
    @Transactional(readOnly = true)
    public boolean report(Long orderId, Long restaurantId, String externalOrderNo,
                          OrderStatus status, String reason) {

        if (!REPORTABLE.contains(status)) {
            return true;
        }

        Optional<PartnerVenueGrant> grant = pushService.pushableGrant(restaurantId);
        if (grant.isEmpty()) {
            return true;
        }
        Partner partner = grant.get().getPartner();

        // Only report on an order they actually have. A status update for an
        // order that never reached their till is one they would refuse, and it
        // would bury the real failure — the push — under a second one.
        Optional<PartnerOrderPush> push =
                pushRepository.findByOrderIdAndPartnerId(orderId, partner.getId());
        if (push.isEmpty() || push.get().getStatus() != PartnerPushStatus.DELIVERED) {
            log.debug("Not reporting {} on order {}: it never reached {}'s till",
                    status, externalOrderNo, partner.getCode());
            return true;
        }

        PartnerOrderPushClient.Result result = client.reportStatus(
                partner, externalOrderNo, grant.get().getExternalVenueId(),
                status.name(), reason,
                java.time.LocalDateTime.now().atOffset(ZoneOffset.UTC).format(ISO_UTC));

        if (result instanceof PartnerOrderPushClient.Result.Accepted) {
            log.debug("Reported {} on order {} to {}", status, externalOrderNo, partner.getCode());
            return true;
        }
        if (result instanceof PartnerOrderPushClient.Result.Rejected rejected) {
            // Not retried, and not fatal either. The order is already cooked and
            // delivered; what is lost is the venue's view of it closing, which
            // is worth a log and not worth a queue.
            log.warn("{} refused status {} on order {}: {}",
                    partner.getCode(), status, externalOrderNo, rejected.reason());
            return true;
        }

        log.warn("Could not report {} on order {} to {}: {}", status, externalOrderNo,
                partner.getCode(), ((PartnerOrderPushClient.Result.Retryable) result).reason());
        return false;
    }
}
