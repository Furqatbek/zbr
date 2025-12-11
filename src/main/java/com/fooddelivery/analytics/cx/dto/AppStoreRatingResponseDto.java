package com.fooddelivery.analytics.cx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for individual app store rating.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppStoreRatingResponseDto {

    private Long id;
    private String platformName;
    private Integer score;
    private String title;
    private String comment;
    private String reviewId;
    private String country;
    private String appVersion;
    private Double sentimentScore;
    private LocalDateTime createdAt;
}
