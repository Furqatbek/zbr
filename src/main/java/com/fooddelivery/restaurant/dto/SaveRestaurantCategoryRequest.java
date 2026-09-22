package com.fooddelivery.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create or update a cuisine category.
 *
 * <p>Names are per language rather than one string, because the app renders
 * them verbatim and ships in three. Only Uzbek is required: a category with one
 * name is usable everywhere through the fallback, where a category with no name
 * in the caller's language would render a blank chip.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create or update a restaurant cuisine category")
public class SaveRestaurantCategoryRequest {

    @Schema(description = "Stable identifier. Derived from the English name when omitted, "
            + "and never changed by an update", example = "burgers")
    @Size(max = 80)
    private String slug;

    @Schema(description = "Name in Uzbek — the fallback for the other two", example = "Burgerlar")
    @NotBlank(message = "nameUz is required")
    @Size(max = 120)
    private String nameUz;

    @Schema(description = "Name in Russian", example = "Бургеры")
    @Size(max = 120)
    private String nameRu;

    @Schema(description = "Name in English", example = "Burgers")
    @Size(max = 120)
    private String nameEn;

    @Schema(description = "Square 256x256 PNG", example = "https://zbrr.uz/media/cat/burgers.png")
    @Size(max = 500)
    private String imageUrl;

    @Schema(description = "Lower sorts first in the chip rail", example = "30")
    private Integer sortOrder;

    @Schema(description = "Inactive categories are offered to nobody", example = "true")
    private Boolean active;
}
