package com.fooddelivery.restaurant.controller;

import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.common.i18n.RequestLanguage;
import com.fooddelivery.restaurant.dto.RestaurantDto;
import com.fooddelivery.restaurant.dto.RestaurantLandingDto;
import com.fooddelivery.restaurant.service.MenuService;
import com.fooddelivery.restaurant.service.RestaurantEtaEnricher;
import com.fooddelivery.restaurant.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * What a scanned QR code resolves to.
 *
 * <p>The poster on a table says {@code app.zbrr.uz/r/qahvoon}. That page is
 * served from Vercel, hits this once, and renders the restaurant with its menu
 * — before the customer has an account, an app, or any reason to trust us yet.
 * Public for the same reason the menu endpoints are: a menu behind a login is a
 * menu nobody reads.
 *
 * <p>The path segment is whatever is printed on the poster. A slug is what you
 * want there — {@code /r/qahvoon} survives the restaurant being renamed in our
 * database and reads as something on a sticker — but an id is accepted so a
 * code can be printed before anyone has agreed on a slug.
 */
@RestController
@RequestMapping("/api/v1/public/r")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Public landing", description = "Restaurant + menu for QR landing pages")
public class PublicLandingController {

    private final RestaurantService restaurantService;
    private final MenuService menuService;
    private final RestaurantEtaEnricher etaEnricher;

    @GetMapping("/{slugOrId}")
    @Operation(summary = "Restaurant and menu for a QR landing page",
            description = "Public. Accepts a slug (preferred) or a numeric id. "
                    + "Optional lat/lng add distance and an arrival estimate.")
    public ResponseEntity<ApiResponse<RestaurantLandingDto>> landing(
            @Parameter(description = "Restaurant slug, e.g. qahvoon — or its numeric id")
            @PathVariable String slugOrId,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lng,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {

        RestaurantDto restaurant = resolve(slugOrId);

        // Same enrichment the app's own cards get, so the web page and the app
        // do not quote different arrival times for the same venue.
        restaurant = etaEnricher.forRequest(restaurant, lat, lng,
                RequestLanguage.from(acceptLanguage));

        RestaurantLandingDto body = RestaurantLandingDto.builder()
                .restaurant(restaurant)
                .menu(menuService.getFullMenu(restaurant.getId()))
                .build();

        // A poster is scanned in bursts — a table of six, a queue at a till —
        // and the menu behind it changes a few times a day at most.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePublic())
                .body(ApiResponse.success(body));
    }

    /**
     * A slug that happens to be all digits is still a slug.
     *
     * <p>Tried as an id only when it parses as one, and falling back to the
     * slug lookup if no such restaurant exists — otherwise a venue slugged
     * "5" would be unreachable, and the failure would be a confusing 404 for a
     * poster that is already printed.
     */
    private RestaurantDto resolve(String slugOrId) {
        if (slugOrId.chars().allMatch(Character::isDigit)) {
            try {
                return restaurantService.getRestaurantById(Long.parseLong(slugOrId));
            } catch (NumberFormatException tooLong) {
                // Longer than a long: not an id, so it can only be a slug.
            } catch (RuntimeException notFound) {
                log.debug("No restaurant with id {}, trying it as a slug", slugOrId);
            }
        }
        return restaurantService.getRestaurantBySlug(slugOrId);
    }
}
