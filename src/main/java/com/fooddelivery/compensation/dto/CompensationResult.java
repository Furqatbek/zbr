package com.fooddelivery.compensation.dto;

import com.fooddelivery.compensation.entity.CompensationType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Result of compensation calculation.
 */
@Data
@Builder
public class CompensationResult {
    private boolean eligible;
    private Long ruleId;
    private String ruleName;
    private CompensationType compensationType;
    private BigDecimal creditAmount;
    private BigDecimal refundAmount;
    private boolean freeDelivery;
    private String description;
}
