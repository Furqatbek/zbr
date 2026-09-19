package com.fooddelivery.integration.partner.service;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.integration.partner.entity.PartnerVenueGrant;
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
     * @param restaurantId the venue the basket is for
     * @param items the menu items being ordered, already resolved
     * @throws BusinessException naming the offending dishes, in a sentence a
     *         customer can act on
     */
    public void checkOrderable(Long restaurantId, List<MenuItem> items) {
        Optional<PartnerVenueGrant> grant = pushService.pushableGrant(restaurantId);
        if (grant.isEmpty()) {
            // Not a partner venue. Nothing to check — this is most restaurants.
            return;
        }

        String source = grant.get().getPartner().getCode();

        List<String> unorderable = items.stream()
                .filter(item -> item.getExternalId() == null
                        || !source.equals(item.getExternalSource()))
                .map(MenuItem::getName)
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
                ? "«" + unorderable.get(0) + "» сейчас недоступен. Удалите его из корзины."
                : "Эти блюда сейчас недоступны: " + String.join(", ", unorderable)
                        + ". Удалите их из корзины.");
    }
}
