package com.fooddelivery.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for menu category information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Menu category information")
public class MenuCategoryDto {

    @Schema(description = "Category ID")
    private Long id;

    @Schema(description = "Restaurant ID")
    private Long restaurantId;

    @Schema(description = "Category name")
    private String name;

    @Schema(description = "Category description")
    private String description;

    @Schema(description = "Category image URL")
    private String imageUrl;

    @Schema(description = "Sort order")
    private Integer sortOrder;

    @Schema(description = "Is active")
    private Boolean active;

    @Schema(description = "Items in this category")
    private List<MenuItemDto> items;
}
