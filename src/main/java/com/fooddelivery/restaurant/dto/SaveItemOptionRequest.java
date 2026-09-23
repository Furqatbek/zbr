package com.fooddelivery.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Add or change one add-on of a dish.
 *
 * <p>Partial on update: a field left out is left alone.
 *
 * <p>{@code groupName} is what turns a flat list into a picker — options
 * sharing a group are one question asked of the customer ("Sauce"), and
 * {@code required} and {@code maxSelections} describe how that question may be
 * answered. They are stored per option but describe the group, so keep them
 * consistent across it; the order validator reads the strictest value it finds.
 */
@Data
@lombok.EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create or update an item add-on")
public class SaveItemOptionRequest extends com.fooddelivery.common.dto.StrictRequest {

    @Schema(description = "The question this option answers, e.g. Sauce", example = "Extras")
    @Size(max = 100)
    private String groupName;

    @Schema(description = "What the customer picks", example = "Cheese")
    @NotBlank(message = "Option name is required", groups = SaveItemVariantRequest.OnCreate.class)
    @Size(max = 100)
    private String name;

    @Schema(description = "Added to the line price when chosen", example = "5000")
    private BigDecimal priceDelta;

    @Schema(description = "Preselected in the app", example = "false")
    private Boolean isDefault;

    @Schema(description = "How many options may be chosen from this GROUP", example = "3")
    @Min(value = 1, message = "maxSelections must be at least 1")
    private Integer maxSelections;

    @Schema(description = "True when the customer must choose from this group", example = "false")
    private Boolean required;

    @Schema(description = "False when this add-on is sold out", example = "true")
    private Boolean inStock;

    @Schema(description = "Lower sorts first", example = "1")
    private Integer sortOrder;

    @Schema(description = "False hides the add-on without deleting it", example = "true")
    private Boolean active;
}
