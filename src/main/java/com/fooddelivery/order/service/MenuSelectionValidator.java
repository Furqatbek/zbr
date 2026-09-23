package com.fooddelivery.order.service;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.order.dto.OrderItemRequest;
import com.fooddelivery.restaurant.entity.ItemOption;
import com.fooddelivery.restaurant.entity.ItemVariant;
import com.fooddelivery.restaurant.entity.MenuItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Whether a customer's choices on a dish are ones the kitchen can cook.
 *
 * <p>The order path used to check only that each id belonged to the dish. So an
 * order could arrive with no sauce chosen from a group marked required, with
 * five extras from a group capped at three, or with a size that sold out at
 * lunchtime — all accepted, all priced, and all discovered by whoever had to
 * make it.
 *
 * <p>The app validates the same rules for a good experience; this validates
 * them because a rule that lives only in a client is one release away from not
 * existing. Messages are written to be shown to a customer: they name the dish
 * and the group, because "invalid selection" tells someone holding a phone
 * nothing they can act on.
 */
@Component
@Slf4j
public class MenuSelectionValidator {

    /** Options with no group of their own are each their own question. */
    private static final String UNGROUPED = "";

    /**
     * @throws BusinessException naming the dish and what is wrong with it
     */
    public void validate(MenuItem item, OrderItemRequest request) {
        validateVariant(item, request);
        validateOptions(item, request);
    }

    private void validateVariant(MenuItem item, OrderItemRequest request) {
        List<ItemVariant> sellable = item.getVariants().stream()
                .filter(v -> Boolean.TRUE.equals(v.getActive()))
                .toList();

        List<ItemVariant> available = sellable.stream()
                .filter(v -> Boolean.TRUE.equals(v.getInStock()))
                .toList();

        // Every size gone is the dish being gone. Asking for a size here and
        // then refusing each one in turn is a dead end of our own making: the
        // customer is told to choose from a list where nothing can be chosen.
        if (!sellable.isEmpty() && available.isEmpty()) {
            throw new BusinessException("'" + item.getName()
                    + "' is unavailable right now — every size is sold out.");
        }

        if (request.getVariantId() == null) {
            // A dish sold in sizes has no meaningful price without one: the
            // customer would be charged the base and the kitchen would have to
            // guess which one to make. Only the sizes they can actually pick
            // are named.
            if (!available.isEmpty()) {
                throw new BusinessException("Choose a size for '" + item.getName() + "': "
                        + available.stream().map(ItemVariant::getName).collect(Collectors.joining(", ")));
            }
            return;
        }

        ItemVariant chosen = sellable.stream()
                .filter(v -> v.getId().equals(request.getVariantId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "That size is no longer available for '" + item.getName() + "'"));

        if (!Boolean.TRUE.equals(chosen.getInStock())) {
            throw new BusinessException("'" + chosen.getName() + "' is sold out for '"
                    + item.getName() + "'. Please choose another size.");
        }
    }

    private void validateOptions(MenuItem item, OrderItemRequest request) {
        List<Long> requested = request.getOptionIds() == null ? List.of() : request.getOptionIds();

        // The same add-on twice is not a quantity — nothing in the model carries
        // one — so it would silently be charged twice and cooked once.
        Set<Long> distinct = new java.util.LinkedHashSet<>(requested);
        if (distinct.size() != requested.size()) {
            throw new BusinessException("The same add-on was chosen twice for '" + item.getName() + "'");
        }

        List<ItemOption> sellable = item.getOptions().stream()
                .filter(o -> Boolean.TRUE.equals(o.getActive()))
                .toList();

        List<ItemOption> chosen = new ArrayList<>();
        for (Long optionId : distinct) {
            ItemOption option = sellable.stream()
                    .filter(o -> o.getId().equals(optionId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(
                            "That add-on is no longer available for '" + item.getName() + "'"));

            if (!Boolean.TRUE.equals(option.getInStock())) {
                throw new BusinessException("'" + option.getName() + "' is sold out for '"
                        + item.getName() + "'");
            }
            chosen.add(option);
        }

        validateGroups(item, sellable, chosen);
    }

    /**
     * The group rules: required means one must be chosen, maxSelections caps
     * how many may be.
     *
     * <p>Both are stored on every option although they describe the group, so
     * the strictest value found in a group wins — the alternative is letting an
     * inconsistent row decide, and a cap is not a cap if a sloppy edit can lift
     * it.
     */
    private void validateGroups(MenuItem item, List<ItemOption> sellable, List<ItemOption> chosen) {
        Map<String, List<ItemOption>> offered = sellable.stream()
                .collect(Collectors.groupingBy(MenuSelectionValidator::groupOf,
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, Long> chosenPerGroup = chosen.stream()
                .collect(Collectors.groupingBy(MenuSelectionValidator::groupOf,
                        LinkedHashMap::new, Collectors.counting()));

        offered.forEach((group, options) -> {
            long picked = chosenPerGroup.getOrDefault(group, 0L);

            boolean required = options.stream().anyMatch(o -> Boolean.TRUE.equals(o.getRequired()));
            if (required && picked == 0) {
                throw new BusinessException("Choose " + describe(group) + " for '" + item.getName() + "'");
            }

            int cap = options.stream()
                    .map(ItemOption::getMaxSelections)
                    .filter(Objects::nonNull)
                    .min(Integer::compareTo)
                    .orElse(Integer.MAX_VALUE);
            if (picked > cap) {
                throw new BusinessException("Choose at most " + cap + " from "
                        + describe(group) + " for '" + item.getName() + "'");
            }
        });
    }

    private static String groupOf(ItemOption option) {
        return option.getGroupName() == null ? UNGROUPED : option.getGroupName();
    }

    private static String describe(String group) {
        return group.isEmpty() ? "an option" : "'" + group + "'";
    }
}
