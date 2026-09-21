package com.fooddelivery.integration.restos.controller;

import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.integration.restos.dto.*;
import com.fooddelivery.integration.restos.service.RestosMenuImportService;
import com.fooddelivery.restaurant.dto.RestaurantDto;
import com.fooddelivery.restaurant.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/api/v1/restos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Restos Integration", description = "Import menu from external Restos restaurant system")
@SecurityRequirement(name = "bearerAuth")
public class RestosIntegrationController {

    private final RestosMenuImportService importService;
    private final RestaurantService restaurantService;

    /**
     * Import menu from Restos into the restaurant owner's restaurant.
     * The owner just provides their Restos base URL and restaurant ID.
     * The backend resolves which local restaurant to populate from the JWT token.
     */
    @PostMapping("/import-menu")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Import menu from Restos",
            description = "Restaurant owner provides their Restos system URL and restaurant ID. " +
                    "Backend fetches menu and saves categories + products to the owner's restaurant.")
    public ResponseEntity<ApiResponse<MenuImportResult>> importMenu(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody ImportMenuBody body) {

        Long localRestaurantId = resolveOwnerRestaurant(currentUser, body.getLocalRestaurantId());

        log.info("Restaurant owner {} importing menu from Restos {} for local restaurant {}",
                currentUser.getId(), body.getBaseUrl(), localRestaurantId);

        MenuImportRequest request = MenuImportRequest.builder()
                .baseUrl(body.getBaseUrl())
                .externalRestaurantId(body.getExternalRestaurantId())
                .apiKey(body.getApiKey())
                .overwriteExisting(body.getOverwriteExisting())
                .build();

        MenuImportResult result = importService.importFullMenu(localRestaurantId, request);
        return ResponseEntity.ok(ApiResponse.success("Menu imported successfully", result));
    }

    /**
     * Import a menu the caller already holds, with no outbound call.
     *
     * <p>For a venue we cannot reach. Everything after the fetch is the same
     * code as {@code /import-menu} — same id matching, same price and
     * publishing rules, same brakes on deactivation.
     */
    @PostMapping("/import-menu-payload")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Import a supplied menu payload",
            description = "Import the venue's own menu JSON without fetching it. For venues whose "
                    + "system is unreachable from this server. Send their response body as "
                    + "'payload', or the array inside it as 'categories'.")
    public ResponseEntity<ApiResponse<MenuImportResult>> importMenuPayload(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody ImportMenuPayloadBody body) {

        Long localRestaurantId = resolveOwnerRestaurant(currentUser, body.getLocalRestaurantId());

        log.info("Restaurant owner {} importing a supplied menu payload for local restaurant {}",
                currentUser.getId(), localRestaurantId);

        SuppliedMenuImportRequest request = SuppliedMenuImportRequest.builder()
                .externalRestaurantId(body.getExternalRestaurantId())
                .payload(body.getPayload())
                .categories(body.getCategories())
                .overwriteExisting(body.getOverwriteExisting())
                .build();

        MenuImportResult result = importService.importSuppliedMenu(localRestaurantId, request);
        return ResponseEntity.ok(ApiResponse.success("Menu imported from the supplied payload", result));
    }

    /**
     * Preview menu from Restos before importing (read-only, no DB changes).
     */
    @PostMapping("/preview-menu")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Preview Restos menu", description = "Fetch and preview menu from Restos without importing")
    public ResponseEntity<ApiResponse<List<RestosCategory>>> previewMenu(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody PreviewMenuBody body) {

        log.info("Restaurant owner {} previewing Restos menu from {}", currentUser.getId(), body.getBaseUrl());

        List<RestosCategory> menu = importService.fetchCachedMenu(
                body.getBaseUrl(), body.getExternalRestaurantId(), body.getApiKey());
        return ResponseEntity.ok(ApiResponse.success(menu));
    }

    /**
     * Resolves which local restaurant to import into.
     * If localRestaurantId is provided, validates ownership.
     * If not provided, uses the owner's first (or only) restaurant.
     */
    private Long resolveOwnerRestaurant(UserPrincipal currentUser, Long localRestaurantId) {
        boolean isAdminOrPlatform = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_PLATFORM"));

        if (localRestaurantId != null) {
            restaurantService.validateRestaurantAccess(localRestaurantId, currentUser.getId(), isAdminOrPlatform);
            return localRestaurantId;
        }

        if (isAdminOrPlatform) {
            throw new BusinessException("Admin/Platform must specify localRestaurantId");
        }

        List<RestaurantDto> ownerRestaurants = restaurantService.getRestaurantsByOwner(currentUser.getId());
        if (ownerRestaurants.isEmpty()) {
            throw new BusinessException("You don't have any restaurants. Create a restaurant first.");
        }
        if (ownerRestaurants.size() > 1) {
            throw new BusinessException("You have multiple restaurants. Please specify localRestaurantId.");
        }

        return ownerRestaurants.get(0).getId();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportMenuBody {
        @NotBlank(message = "Restos base URL is required")
        private String baseUrl;

        @NotNull(message = "External restaurant ID is required")
        private Long externalRestaurantId;

        private String apiKey;

        private Long localRestaurantId;

        private Boolean overwriteExisting = false;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportMenuPayloadBody {
        @NotNull(message = "External restaurant ID is required")
        private Long externalRestaurantId;

        /** The venue's response body, unedited. */
        private RestosApiResponse<List<RestosCategory>> payload;

        /** Or just the array inside it. Exactly one of the two. */
        private List<RestosCategory> categories;

        private Long localRestaurantId;

        private Boolean overwriteExisting = false;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreviewMenuBody {
        @NotBlank(message = "Restos base URL is required")
        private String baseUrl;

        @NotNull(message = "External restaurant ID is required")
        private Long externalRestaurantId;

        private String apiKey;
    }
}
