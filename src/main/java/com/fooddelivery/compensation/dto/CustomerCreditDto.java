package com.fooddelivery.compensation.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for customer credit.
 */
@Data
@Builder
public class CustomerCreditDto {
    private Long id;
    private Long userId;
    private String userEmail;
    private BigDecimal balance;
    private BigDecimal lifetimeEarned;
    private BigDecimal lifetimeUsed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
