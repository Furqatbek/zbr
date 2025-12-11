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
