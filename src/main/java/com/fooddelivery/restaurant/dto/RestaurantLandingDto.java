package com.fooddelivery.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Everything a scanned QR code's landing page needs, in one request.
 *
 * <p>The page is served from another origin and another host, so it cannot make
 * a call, read an id out of the answer and make a second one without the
 * customer watching a spinner twice. The restaurant and its menu arrive
 * together.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A restaurant and its menu, for a public landing page")
public class RestaurantLandingDto {

    @Schema(description = "The restaurant, as the customer app shows it")
    private RestaurantDto restaurant;

    @Schema(description = "Menu categories, each with its items. Empty when the venue has no menu yet")
    private List<MenuCategoryDto> menu;
}
