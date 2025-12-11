package com.fooddelivery.analytics.cx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for individual restaurant rating.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantRatingResponseDto {

    private Long id;
    private Long restaurantId;
    private Long userId;
    private Long orderId;
    private Integer score;
    private Integer foodQualityScore;
    private Integer portionSizeScore;
    private Integer valueForMoneyScore;
    private String comment;
    private String restaurantResponse;
    private LocalDateTime createdAt;
}
