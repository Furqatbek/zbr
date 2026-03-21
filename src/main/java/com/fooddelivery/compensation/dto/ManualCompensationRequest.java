package com.fooddelivery.compensation.dto;

import com.fooddelivery.compensation.entity.CompensationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request for issuing manual compensation.
 */
@Data
public class ManualCompensationRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    private Long orderId;

    @NotNull(message = "Compensation type is required")
    private CompensationType compensationType;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String description;
}
