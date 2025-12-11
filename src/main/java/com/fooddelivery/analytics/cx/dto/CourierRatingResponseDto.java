package com.fooddelivery.analytics.cx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for individual courier rating.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierRatingResponseDto {

    private Long id;
    private Long courierId;
    private Long userId;
    private Long orderId;
    private Long deliveryId;
    private Integer score;
    private Integer professionalismScore;
    private Integer communicationScore;
    private Integer timelinessScore;
    private String comment;
    private BigDecimal tipAmount;
    private LocalDateTime createdAt;
}
