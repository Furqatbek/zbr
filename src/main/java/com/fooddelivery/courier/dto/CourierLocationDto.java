package com.fooddelivery.courier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierLocationDto {
    private Long courierId;
    private BigDecimal lat;
    private BigDecimal lng;
    private LocalDateTime timestamp;
}
