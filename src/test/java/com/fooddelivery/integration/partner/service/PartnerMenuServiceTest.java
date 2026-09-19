package com.fooddelivery.integration.partner.service;

import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.integration.partner.dto.PartnerBulkResult;
import com.fooddelivery.integration.partner.dto.PartnerItemUpdate;
import com.fooddelivery.restaurant.entity.MenuItem;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.repository.MenuItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Price and availability pushed by a partner.
 *
 * <p>The price case has a trap in it: what the customer pays is
 * {@code priceWithMargin}, not {@code price}. Writing only the obvious field
 * would report success to the partner while the app kept charging the old
 * number, which is the kind of bug nobody notices until a reconciliation.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Partner menu updates")
class PartnerMenuServiceTest {

    private static final String SOURCE = "RESTOS";

    @Mock private MenuItemRepository menuItemRepository;

    private PartnerMenuService service;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        service = new PartnerMenuService(menuItemRepository);
        restaurant = Restaurant.builder().id(100L).name("Osh Markazi").build();
        when(menuItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(menuItemRepository.findByRestaurantAndExternalId(anyLong(), anyString(), any()))
                .thenReturn(Optional.empty());
    }

    private MenuItem item(Long externalId) {
        MenuItem item = MenuItem.builder()
                .id(1L).name("Plov")
                .price(new BigDecimal("30000")).priceWithMargin(new BigDecimal("33000"))
                .inStock(true).active(true)
                .externalId(externalId).externalSource(SOURCE)
                .build();
        when(menuItemRepository.findByRestaurantAndExternalId(100L, SOURCE, externalId))
                .thenReturn(Optional.of(item));
        return item;
    }

    private PartnerItemUpdate update(String id, BigDecimal price, Boolean available) {
        return PartnerItemUpdate.builder().externalItemId(id).price(price).available(available).build();
    }

    @Test
    @DisplayName("a new price is written to the field the customer is actually charged")
    void priceGoesToBothFields() {
        MenuItem item = item(4417L);

        service.applyUpdate(restaurant, SOURCE, update("4417", new BigDecimal("32000"), null));

        assertThat(item.getPrice()).isEqualByComparingTo("32000");
        // getEffectivePrice() prefers priceWithMargin, and that is what becomes
        // the order line's unit price. Leaving it stale would mean the partner's
        // change was accepted and then ignored.
        assertThat(item.getPriceWithMargin()).isEqualByComparingTo("32000");
        assertThat(item.getEffectivePrice()).isEqualByComparingTo("32000");
    }

    @Test
    @DisplayName("a partner's price is charged verbatim, with nothing added")
    void noMarginIsAdded() {
        MenuItem item = item(4417L);

        service.applyUpdate(restaurant, SOURCE, update("4417", new BigDecimal("32000"), null));

        // The pricing agreement in one assertion.
        assertThat(item.getEffectivePrice()).isEqualByComparingTo(item.getPrice());
    }

    @Test
    @DisplayName("availability changes stock, never the item's existence")
    void availabilityIsNotDelisting() {
        MenuItem item = item(4417L);

        service.applyUpdate(restaurant, SOURCE, update("4417", null, false));

        assertThat(item.getInStock()).isFalse();
        // Sold out, not withdrawn. Retiring a dish is the sync's job; letting a
        // lunchtime sell-out do it would take food off the menu for good.
        assertThat(item.getActive()).isTrue();
    }

    @Test
    @DisplayName("omitted fields are left alone")
    void omittedFieldsUntouched() {
        MenuItem item = item(4417L);

        service.applyUpdate(restaurant, SOURCE, update("4417", null, false));

        // The whole point of a partial update: the full-replacement endpoint
        // would have blanked the price here.
        assertThat(item.getPrice()).isEqualByComparingTo("30000");
        assertThat(item.getName()).isEqualTo("Plov");
    }

    @Test
    @DisplayName("an unknown item is not found")
    void unknownItemNotFound() {
        assertThatThrownBy(() -> service.applyUpdate(restaurant, SOURCE,
                update("9999", new BigDecimal("1000"), null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("a bulk update applies what it can and reports the rest")
    void bulkIsPartial() {
        item(4417L);
        item(4418L);

        PartnerBulkResult result = service.applyUpdates(restaurant, SOURCE, List.of(
                update("4417", new BigDecimal("32000"), null),
                update("4418", null, false),
                update("9999", new BigDecimal("1000"), null)));

        // A markup change across four hundred dishes must not be lost because
        // three of them were deleted here last week.
        assertThat(result.getUpdated()).isEqualTo(2);
        assertThat(result.getUnknownItemIds()).containsExactly("9999");
    }

    @Test
    @DisplayName("an update that changes nothing is rejected rather than counted")
    void emptyUpdateRejected() {
        item(4417L);

        PartnerBulkResult result = service.applyUpdates(restaurant, SOURCE,
                List.of(update("4417", null, null)));

        assertThat(result.getUpdated()).isZero();
        assertThat(result.getRejected()).hasSize(1);
    }

    @Test
    @DisplayName("an unparseable id is reported as unknown, not as a 400 for the whole batch")
    void nonNumericIdIsUnknown() {
        item(4417L);

        PartnerBulkResult result = service.applyUpdates(restaurant, SOURCE, List.of(
                update("4417", new BigDecimal("32000"), null),
                update("not-a-number", new BigDecimal("1000"), null)));

        // Rejecting the whole call over one odd id would lose the other 399
        // updates, and from the partner's side "unknown" and "unparseable" mean
        // the same thing anyway.
        assertThat(result.getUpdated()).isEqualTo(1);
        assertThat(result.getUnknownItemIds()).containsExactly("not-a-number");
    }

    @Test
    @DisplayName("a partner can only reach items that came from them")
    void scopedToThePartnersOwnItems() {
        // The lookup is keyed on externalSource, which is the partner's code.
        // An item the restaurant typed in by hand carries no source and is not
        // theirs to reprice; another partner's items are not either.
        item(4417L);

        assertThatThrownBy(() -> service.applyUpdate(restaurant, "OTHER_POS",
                update("4417", new BigDecimal("1"), null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
