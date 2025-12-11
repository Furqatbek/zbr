package com.fooddelivery.analytics.cx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for individual NPS survey.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NpsSurveyResponseDto {

    private Long id;
    private Long userId;
    private Integer score;
    private String category; // PROMOTER, PASSIVE, DETRACTOR
    private String comment;
    private String surveyChannel;
    private String userSegment;
    private Boolean isFollowUpRequested;
    private LocalDateTime createdAt;
}
