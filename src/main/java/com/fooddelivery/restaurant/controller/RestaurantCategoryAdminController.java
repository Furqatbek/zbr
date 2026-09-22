package com.fooddelivery.restaurant.controller;

import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.common.i18n.RequestLanguage;
import com.fooddelivery.restaurant.dto.RestaurantCategoryDto;
import com.fooddelivery.restaurant.dto.RestaurantDto;
import com.fooddelivery.restaurant.dto.SaveRestaurantCategoryRequest;
import com.fooddelivery.restaurant.service.RestaurantCategoryService;
import com.fooddelivery.restaurant.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Managing cuisines, and the two switches on a restaurant that only the
 * platform may throw.
 *
 * <p>Under {@code /admin/} rather than on the restaurant's own resource, for
 * the same reason partner administration is: an owner must not be able to file
 * their own restaurant under whatever cuisine is trending, or put themselves in
 * the recommended carousel.
 */
@RestController
@RequestMapping("/api/v1/admin/restaurant-categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Restaurant categories (admin)", description = "Cuisine categories and assignment")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM')")
public class RestaurantCategoryAdminController {

    private final RestaurantCategoryService categoryService;
    private final RestaurantService restaurantService;

    /** Everything, including empty and deactivated ones. */
    @GetMapping
    @Operation(summary = "List every category")
    public ResponseEntity<ApiResponse<List<RestaurantCategoryDto>>> list(
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {

        return ResponseEntity.ok(ApiResponse.success(
                categoryService.listAll(RequestLanguage.from(acceptLanguage))));
    }

    @PostMapping
    @Operation(summary = "Create a category",
            description = "nameUz is required and is the fallback for the other languages.")
    public ResponseEntity<ApiResponse<RestaurantCategoryDto>> create(
            @Valid @RequestBody SaveRestaurantCategoryRequest request) {

        RestaurantCategoryDto category = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created", category));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a category",
            description = "Partial: a field left out is left alone. The slug never changes — "
                    + "it is what analytics and deep links key on.")
    public ResponseEntity<ApiResponse<RestaurantCategoryDto>> update(
            @PathVariable Long id,
            @RequestBody SaveRestaurantCategoryRequest request) {

        return ResponseEntity.ok(ApiResponse.success("Category updated",
                categoryService.update(id, request)));
    }

    /**
     * File a restaurant under a cuisine, or under none.
     *
     * <p>{@code categoryId} omitted removes the assignment, which is how a
     * restaurant filed wrongly gets unfiled without inventing a category for it.
     */
    @PutMapping("/assignments/{restaurantId}")
    @Operation(summary = "Assign a restaurant to a category")
    public ResponseEntity<ApiResponse<RestaurantDto>> assign(
            @PathVariable Long restaurantId,
            @Parameter(description = "Category id, or omit to clear the assignment")
            @RequestParam(required = false) Long categoryId) {

        categoryService.assign(restaurantId, categoryId);
        return ResponseEntity.ok(ApiResponse.success("Category assigned",
                restaurantService.getRestaurantById(restaurantId)));
    }
}
