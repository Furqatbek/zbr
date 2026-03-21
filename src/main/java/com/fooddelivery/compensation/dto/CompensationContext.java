package com.fooddelivery.compensation.dto;

import com.fooddelivery.compensation.entity.CompensationTriggerType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Context data for evaluating compensation rules.
 */
@Data
@Builder
public class CompensationContext {
    private Long orderId;
    private Long userId;
    private CompensationTriggerType triggerType;

    // Order details
    private BigDecimal orderTotal;
    private LocalDateTime estimatedDeliveryTime;
    private LocalDateTime actualDeliveryTime;

    // Delay information
    private Integer delayMinutes;

    // Missing items
    private List<MissingItemDto> missingItems;
    private BigDecimal missingItemsValue;

    // Additional context
    private String description;
}
