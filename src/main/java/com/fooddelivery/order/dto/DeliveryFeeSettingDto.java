package com.fooddelivery.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for individual delivery fee setting.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryFeeSettingDto {
    private Long id;
    private String settingKey;
    private BigDecimal settingValue;
    private String description;
    private LocalDateTime updatedAt;
}
