package com.fooddelivery.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a review.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create review request")
public class CreateReviewRequest {

    @Min(value = 1, message = "Restaurant rating must be at least 1")
    @Max(value = 5, message = "Restaurant rating must be at most 5")
    @Schema(description = "Restaurant rating (1-5)")
    private Integer restaurantRating;

    @Min(value = 1, message = "Courier rating must be at least 1")
    @Max(value = 5, message = "Courier rating must be at most 5")
    @Schema(description = "Courier rating (1-5)")
    private Integer courierRating;

    @Min(value = 1, message = "Food rating must be at least 1")
    @Max(value = 5, message = "Food rating must be at most 5")
    @Schema(description = "Food rating (1-5)")
    private Integer foodRating;

    @Size(max = 1000, message = "Comment must be at most 1000 characters")
    @Schema(description = "Review comment")
    private String comment;

    @Schema(description = "Review tags (e.g., FAST_DELIVERY, TASTY_FOOD)")
    private List<String> tags;
}
