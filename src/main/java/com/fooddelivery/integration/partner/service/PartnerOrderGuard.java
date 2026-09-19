package com.fooddelivery.integration.partner.service;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.integration.partner.entity.PartnerVenueGrant;
import com.fooddelivery.order.entity.OrderItem;
import com.fooddelivery.restaurant.entity.MenuItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Refuses a basket the partner's kitchen could not read, before the customer
 * pays for it.
 *
 * <p>A partner's order API rejects the whole basket if any line names a product
 * they do not have — which is the right call on their side: an order the kitchen
 * cannot read is worse than a refused one. But it means the failure lands
 * <em>after</em> checkout, on a customer who has already paid, for an order that
 * will never be cooked.
 *
 * <p>It is not a hypothetical. Our vendor app lets a restaurant add menu items
 * directly, without going through the till, so a venue running on a partner POS
 * ends up with a mixed catalogue the first time someone adds a lunch special.
 * Checking at checkout turns "your money is gone and your food is not coming"
 * into "that dish is not available right now", which is a conversation worth
 * having before payment rather than after.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerOrderGuard {

    private final PartnerOrderPushService pushService;

    /**
     * Whether the partner's kitchen could act on this line.
     *
     * <p>Two ways it could not. The dish may be one they have never heard of —
     * something added in our own panel, which carries no id of theirs. Or it
     * may be a dish they sell by size with no size chosen: they refuse those
     * outright rather than charging the base price, because the alternative is
     * a Large billed as a Regular, cooked Large, with nothing on the ticket to
     * show it.
     */
    private boolean canBeCooked(OrderItem line, String source) {
        MenuItem item = line.getMenuItem();
        if (item == null || item.getExternalId() == null || !source.equals(item.getExternalSource())) {
            return false;
        }
        boolean sellsBySize = item.getVariants() != null && item.getVariants().stream()
                .anyMatch(variant -> Boolean.TRUE.equals(variant.getActive())
                        && variant.getExternalId() != null);
        return !sellsBySize || line.getVariantId() != null;
    }

    /**
     * @param restaurantId the venue the basket is for
     * @param lines the order lines being placed, already resolved
     * @throws BusinessException naming the offending dishes, in a sentence a
     *         customer can act on
     */
    public void checkOrderable(Long restaurantId, List<OrderItem> lines) {
        Optional<PartnerVenueGrant> grant = pushService.pushableGrant(restaurantId);
        if (grant.isEmpty()) {
            // Not a partner venue. Nothing to check — this is most restaurants.
            return;
        }

        String source = grant.get().getPartner().getCode();

        List<String> unorderable = lines.stream()
                .filter(line -> !canBeCooked(line, source))
                .map(OrderItem::getItemName)
                .distinct()
                .toList();

        if (unorderable.isEmpty()) {
            return;
        }

        // An operational problem with the venue's catalogue rather than a
        // customer mistake, so it is logged for us as well as answered to them.
        log.warn("Restaurant {} has items not known to {}: {}. Orders containing them are refused.",
                restaurantId, source, unorderable);

        throw new BusinessException(unorderable.size() == 1
                ? "«" + unorderable.get(0) + "» сейчас недоступен. Проверьте выбор порции "
                        + "или удалите его из корзины."
                : "Эти блюда сейчас недоступны: " + String.join(", ", unorderable)
                        + ". Проверьте выбор порции или удалите их из корзины.");
    }
}
