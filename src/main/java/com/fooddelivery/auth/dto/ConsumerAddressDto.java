package com.fooddelivery.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for consumer address.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerAddressDto {

    private Long id;
    private String label;
    private String fullAddress;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String apartmentNumber;
    private String entrance;
    private String instructions;
    private Boolean isDefault;
    private LocalDateTime createdAt;
}
