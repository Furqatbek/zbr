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
     * <p>Kitchen states are in this set as well as delivery ones, because a
     * restaurant can work from OUR tablet instead of their till: accepting
     * there has to reach the till, or their screen shows an order still
     * waiting while ours shows it cooking. What stops the echo is not the state
     * but its origin — see {@link #report}.
     *
     * <p>CREATED is excluded: we pushed the order, so they already know it
     * exists. REFUNDED is excluded because it is a fact about money rather than
     * about the order, and forcing it onto an order state would mark a
     * delivered order cancelled — their reasoning, and the same reason we do
     * not accept it from them.
     */
    private static final Set<OrderStatus> REPORTABLE = EnumSet.of(
            OrderStatus.ACCEPTED,
            OrderStatus.PREPARING,
            OrderStatus.READY,
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
     * Write down that a venue cooked food for an order we cancelled.
     *
     * <p>Its own transaction because the caller is read-only: this is the one
     * side effect of reporting a status, and it exists so the number at the end
     * of the week is explainable.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    void recordVenueOwed(PartnerOrderPush push, String partnerCode,
                         String externalOrderNo, String reason) {
        if (push.getVenueOwedAt() != null) {
            return;
        }
        push.setVenueOwedAt(java.time.LocalDateTime.now());
        push.setVenueOwedReason(reason);
        pushRepository.save(push);

        log.warn("VENUE OWED — {} refused the cancellation of order {}: their kitchen had already "
                        + "started, so the food was made and someone carries the cost. {}",
                partnerCode, externalOrderNo, reason);
    }

    /**
     * @return false when the report failed in a way worth retrying.
     */
    @Transactional(readOnly = true)
    public boolean report(Long orderId, Long restaurantId, String externalOrderNo,
                          OrderStatus status, String reason) {
        return report(orderId, restaurantId, externalOrderNo, status, reason, null);
    }

    /**
     * @param reportedByPartnerId the partner whose system caused this change,
     *        or null when it originated here.
     */
    @Transactional(readOnly = true)
    public boolean report(Long orderId, Long restaurantId, String externalOrderNo,
                          OrderStatus status, String reason, Long reportedByPartnerId) {

        if (!REPORTABLE.contains(status)) {
            return true;
        }

        Optional<PartnerVenueGrant> grant = pushService.pushableGrant(restaurantId);
        if (grant.isEmpty()) {
            return true;
        }
        Partner partner = grant.get().getPartner();

        // Never send a change back to the partner who reported it. This is the
        // whole rule: origin decides, not the state. A kitchen state they set
        // is theirs and they know it; the same state set on our vendor app is
        // news, and without it their till shows an order still waiting while
        // ours shows it cooking. Their side applies the identical rule in the
        // other direction.
        if (partner.getId().equals(reportedByPartnerId)) {
            log.debug("Not reporting {} on order {} to {}: they reported it",
                    status, externalOrderNo, partner.getCode());
            return true;
        }

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
            if (status == OrderStatus.CANCELLED) {
                // Their cutoff refused the cancellation because the kitchen had
                // already started. Our order is cancelled and the customer
                // refunded either way — we cannot un-cancel it and would not
                // want to. What their 422 says is that the venue is owed for a
                // ticket it has already cooked, and that is a fact about money
                // rather than an integration fault.
                //
                // Recorded rather than merely logged, because the commercial
                // answer does not exist yet and a log line cannot be settled
                // against later.
                recordVenueOwed(push.get(), partner.getCode(), externalOrderNo, rejected.reason());
                return true;
            }

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
