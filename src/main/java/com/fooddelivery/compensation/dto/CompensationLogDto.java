package com.fooddelivery.compensation.dto;

import com.fooddelivery.compensation.entity.CompensationStatus;
import com.fooddelivery.compensation.entity.CompensationTriggerType;
import com.fooddelivery.compensation.entity.CompensationType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for compensation log.
 */
@Data
@Builder
public class CompensationLogDto {
    private Long id;
    private Long userId;
    private String userEmail;
    private Long orderId;
    private String orderNumber;
    private CompensationTriggerType triggerType;
    private CompensationType compensationType;
    private BigDecimal amount;
    private String description;
    private Long ruleId;
    private String ruleName;
    private CompensationStatus status;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
