package com.fooddelivery.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for calculating delivery fee before placing an order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Calculate delivery fee request")
public class CalculateDeliveryFeeRequest {

    @Schema(description = "Restaurant ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Restaurant ID is required")
    private Long restaurantId;

    @Schema(description = "Delivery latitude", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Delivery latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal deliveryLatitude;

    @Schema(description = "Delivery longitude", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Delivery longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal deliveryLongitude;
}
