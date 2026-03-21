package com.fooddelivery.compensation.dto;

import com.fooddelivery.compensation.entity.CompensationTriggerType;
import com.fooddelivery.compensation.entity.CompensationType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for compensation rule.
 */
@Data
@Builder
public class CompensationRuleDto {
    private Long id;
    private String name;
    private String description;
    private CompensationTriggerType triggerType;
    private Map<String, Object> conditions;
    private CompensationType compensationType;
    private BigDecimal compensationValue;
    private BigDecimal compensationPercent;
    private BigDecimal maxCompensation;
    private Integer priority;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
