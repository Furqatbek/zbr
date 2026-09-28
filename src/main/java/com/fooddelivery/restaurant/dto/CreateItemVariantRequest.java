package com.fooddelivery.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating an item variant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create item variant request")
public class CreateItemVariantRequest {

    /**
     * The existing row this entry refers to, when the client knows it.
     *
     * <p>Optional. Matching falls back to the name, which works until somebody
     * renames a size: "Large" to "Katta" matches nothing, so the old row is
     * deleted and a new one created, and a customer holding that id in their
     * basket is pointing at a row that no longer exists. Order history is
     * unaffected — the name and price are snapshotted on the line — but a
     * basket in progress is not history.
     *
     * <p>An id belonging to a different dish is ignored rather than adopted.
     */
    @Schema(description = "Existing id, so a rename keeps its row", example = "11")
    private Long id;

    @Schema(description = "Variant name", example = "Large", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Variant name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Schema(description = "Price difference from base", example = "3.00")
    @DecimalMin(value = "-1000.00", message = "Price delta must be valid")
    @Builder.Default
    private BigDecimal priceDelta = BigDecimal.ZERO;

    @Schema(description = "Sort order")
    private Integer sortOrder;
}
