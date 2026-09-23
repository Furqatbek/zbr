package com.fooddelivery.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Add or change one size of a dish.
 *
 * <p>Used for both, and partial on update: a field left out is left alone. The
 * name is required when creating and optional when editing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create or update an item size")
public class SaveItemVariantRequest {

    @Schema(description = "What the customer picks, e.g. Large", example = "Large")
    @NotBlank(message = "Variant name is required", groups = OnCreate.class)
    @Size(max = 100)
    private String name;

    /**
     * A DIFFERENCE from the item's price, not the price of the size.
     *
     * <p>Negative is allowed — a small portion that costs less is the same
     * mechanism as a large one that costs more.
     */
    @Schema(description = "Difference from the item's price, not an absolute price", example = "8000")
    private BigDecimal priceDelta;

    @Schema(description = "False when this size is sold out but the dish is not", example = "true")
    private Boolean inStock;

    @Schema(description = "Lower sorts first", example = "2")
    private Integer sortOrder;

    @Schema(description = "False hides the size without deleting it", example = "true")
    private Boolean active;

    /** Validation group: fields required when creating, optional when editing. */
    public interface OnCreate {
    }
}
