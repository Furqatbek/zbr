package com.fooddelivery.compensation.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for missing item information.
 */
@Data
@Builder
public class MissingItemDto {
    private Long menuItemId;
    private String itemName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalValue;
}
