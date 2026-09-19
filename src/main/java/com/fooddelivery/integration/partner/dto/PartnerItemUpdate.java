package com.fooddelivery.integration.partner.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A change to one menu item, as the partner describes it.
 *
 * <p>Every field except the item id is optional, and null means "leave this
 * alone" rather than "clear it". That is what makes this a partial update: the
 * existing full-replacement endpoint would wipe a description because the
 * caller only wanted to move a price.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Partial update to one menu item, addressed by the partner's own product id")
public class PartnerItemUpdate {

    @NotBlank(message = "The partner's product id is required")
    @Schema(description = "The product id in the partner's system", example = "4417")
    private String externalItemId;

    /**
     * Charged to the customer exactly as sent. We add nothing to it — see the
     * pricing agreement — so this is the number that appears in the app.
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero")
    @Digits(integer = 8, fraction = 2, message = "Price has too many digits")
    @Schema(description = "New price, charged verbatim. Omit to leave unchanged", example = "32000")
    private BigDecimal price;

    /**
     * Sold out, not delisted. Removing an item from the menu is done by
     * dropping it from the menu snapshot, which the sync then deactivates.
     */
    @Schema(description = "Whether the item can be ordered right now. Omit to leave unchanged")
    private Boolean available;

    public boolean isEmpty() {
        return price == null && available == null;
    }
}
