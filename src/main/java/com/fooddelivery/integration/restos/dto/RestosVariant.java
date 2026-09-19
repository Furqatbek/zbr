package com.fooddelivery.integration.restos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One size or option of a product, as Restos sends it.
 *
 * <p>Their {@code price} REPLACES the base price rather than adding to it,
 * which is the opposite of how our {@link com.fooddelivery.restaurant.entity.ItemVariant}
 * stores it — ours holds a delta. The conversion happens once, at import.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestosVariant {

    private Long id;
    private String name;

    /** Absolute, and replaces the product's base price when chosen. */
    private BigDecimal price;

    private BigDecimal priceWithMargin;
    private Integer sortOrder;
    private Boolean available;
    private Boolean active;

    /** Same fallbacks as RestosProduct: partners vary in which flag they set. */
    public boolean isAvailable() {
        if (available != null) return available;
        if (active != null) return active;
        return true;
    }

    /**
     * What a customer choosing this variant should be charged — their channel
     * price when they send one, their base price otherwise. Never marked up
     * here; the pricing agreement is that their number is charged as sent.
     */
    public BigDecimal effectivePrice() {
        return priceWithMargin != null ? priceWithMargin : price;
    }
}
