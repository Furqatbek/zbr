package com.fooddelivery.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a menu category.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create menu category request")
public class CreateMenuCategoryRequest {

    @Schema(description = "Category name", example = "Appetizers", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Schema(description = "Category description", example = "Start your meal right")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Schema(description = "Category image URL")
    private String imageUrl;

    @Schema(description = "Sort order (optional)")
    private Integer sortOrder;
}
