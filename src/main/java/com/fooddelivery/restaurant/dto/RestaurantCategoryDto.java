package com.fooddelivery.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A cuisine, as the app renders it: one name, already in the right language.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Restaurant cuisine category")
public class RestaurantCategoryDto {

    @Schema(description = "Category ID", example = "3")
    private Long id;

    @Schema(description = "Stable identifier that survives renames", example = "burgers")
    private String slug;

    @Schema(description = "Name in the language of the request's Accept-Language header",
            example = "Burgerlar")
    private String name;

    @Schema(description = "Square 256x256 image, or null")
    private String imageUrl;

    @Schema(description = "Ordering for the chip rail; lower comes first")
    private Integer sortOrder;

    /*
     * The three stored names, carried through so the language can be resolved
     * AFTER a cached read rather than before it. getRestaurantById is
     * @Cacheable by id, so a name resolved on the way in would be cached under
     * that id and served to the next customer in whatever language the first
     * one asked for.
     *
     * Hidden from the response: the app asked for one name, already resolved.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String nameUz;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private String nameRu;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private String nameEn;

    /** A copy with {@code name} resolved for this request's language. */
    public RestaurantCategoryDto localized(String language) {
        return toBuilder()
                .name(com.fooddelivery.common.i18n.LocalizedName.pick(language, nameUz, nameRu, nameEn))
                .build();
    }
}
