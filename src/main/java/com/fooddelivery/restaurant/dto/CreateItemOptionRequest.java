package com.fooddelivery.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating an item option.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create item option request")
public class CreateItemOptionRequest {

    @Schema(description = "Option group name", example = "Toppings")
    @Size(max = 100, message = "Group name must not exceed 100 characters")
    private String groupName;

    @Schema(description = "Option name", example = "Extra Cheese", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Option name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Schema(description = "Price difference", example = "1.50")
    @DecimalMin(value = "0.00", message = "Price delta must be non-negative")
    @Builder.Default
    private BigDecimal priceDelta = BigDecimal.ZERO;

    @Schema(description = "Is default selection", example = "false")
    @Builder.Default
    private Boolean isDefault = false;

    @Schema(description = "Maximum selections allowed", example = "3")
    @Min(value = 1, message = "Max selections must be at least 1")
    @Builder.Default
    private Integer maxSelections = 1;

    @Schema(description = "Is required", example = "false")
    @Builder.Default
    private Boolean required = false;

    @Schema(description = "Sort order")
    private Integer sortOrder;
}
