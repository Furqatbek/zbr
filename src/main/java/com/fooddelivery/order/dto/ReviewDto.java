package com.fooddelivery.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for review information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Review information")
public class ReviewDto {

    @Schema(description = "Review ID")
    private Long id;

    @Schema(description = "Order ID")
    private Long orderId;

    @Schema(description = "Consumer ID")
    private Long consumerId;

    @Schema(description = "Consumer name")
    private String consumerName;

    @Schema(description = "Restaurant ID")
    private Long restaurantId;

    @Schema(description = "Courier ID")
    private Long courierId;

    @Schema(description = "Restaurant rating (1-5)")
    private Integer restaurantRating;

    @Schema(description = "Courier rating (1-5)")
    private Integer courierRating;

    @Schema(description = "Food rating (1-5)")
    private Integer foodRating;

    @Schema(description = "Review comment")
    private String comment;

    @Schema(description = "Review tags")
    private List<String> tags;

    @Schema(description = "Created at")
    private LocalDateTime createdAt;
}
