package com.fooddelivery.restaurant.service;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.common.util.SlugUtils;
import com.fooddelivery.restaurant.dto.RestaurantCategoryDto;
import com.fooddelivery.restaurant.dto.SaveRestaurantCategoryRequest;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.entity.RestaurantCategory;
import com.fooddelivery.restaurant.repository.RestaurantCategoryRepository;
import com.fooddelivery.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The cuisines restaurants are filed under.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantCategoryService {

    private final RestaurantCategoryRepository categoryRepository;
    private final RestaurantRepository restaurantRepository;

    /**
     * What the app shows as chips: categories with at least one open
     * restaurant, named in the caller's language.
     *
     * <p>Empty categories are left out because a chip that filters to nothing
     * is a dead end — the customer taps it, sees an empty screen, and has
     * learned only that the app is broken.
     */
    @Transactional(readOnly = true)
    public List<RestaurantCategoryDto> listForCustomers(String language) {
        return categoryRepository.findWithOpenRestaurants().stream()
                .map(category -> toDto(category, language))
                .toList();
    }

    /** Everything, including empty and deactivated ones. For the admin panel. */
    @Transactional(readOnly = true)
    public List<RestaurantCategoryDto> listAll(String language) {
        return categoryRepository.findAll().stream()
                .map(category -> toDto(category, language))
                .toList();
    }

    @Transactional(readOnly = true)
    public RestaurantCategory getEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantCategory", "id", id));
    }

    @Transactional
    public RestaurantCategoryDto create(SaveRestaurantCategoryRequest request) {
        String slug = request.getSlug() != null && !request.getSlug().isBlank()
                ? request.getSlug().trim().toLowerCase()
                : SlugUtils.toSlug(request.getNameEn() != null ? request.getNameEn() : request.getNameUz());

        if (categoryRepository.existsBySlug(slug)) {
            throw new BusinessException("A category with slug '" + slug + "' already exists");
        }

        RestaurantCategory category = RestaurantCategory.builder()
                .slug(slug)
                .nameUz(request.getNameUz())
                .nameRu(request.getNameRu())
                .nameEn(request.getNameEn())
                .imageUrl(request.getImageUrl())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .active(request.getActive() == null || request.getActive())
                .build();

        category = categoryRepository.save(category);
        log.info("Restaurant category created: {} ({})", category.getNameUz(), category.getSlug());
        return toDto(category, null);
    }

    /**
     * Partial update: a field left out is left alone.
     *
     * <p>The slug is deliberately not editable. It is what analytics and any
     * future deep link key on, and renaming a category should not detach its
     * history.
     */
    @Transactional
    public RestaurantCategoryDto update(Long id, SaveRestaurantCategoryRequest request) {
        RestaurantCategory category = getEntity(id);

        if (request.getNameUz() != null) category.setNameUz(request.getNameUz());
        if (request.getNameRu() != null) category.setNameRu(request.getNameRu());
        if (request.getNameEn() != null) category.setNameEn(request.getNameEn());
        if (request.getImageUrl() != null) category.setImageUrl(request.getImageUrl());
        if (request.getSortOrder() != null) category.setSortOrder(request.getSortOrder());
        if (request.getActive() != null) category.setActive(request.getActive());

        return toDto(categoryRepository.save(category), null);
    }

    /**
     * Assign a restaurant to a cuisine, or to none.
     *
     * <p>Evicts the restaurant's cached DTO for the same reason every other
     * write to it does: the category is part of what a customer sees, and a
     * five-minute stale chip is a restaurant filed under the wrong cuisine.
     */
    @Transactional
    @CacheEvict(value = "restaurants", key = "#restaurantId")
    public void assign(Long restaurantId, Long categoryId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", restaurantId));

        if (categoryId == null) {
            restaurant.setCategory(null);
            restaurantRepository.save(restaurant);
            log.info("Restaurant {} moved out of every category", restaurantId);
            return;
        }

        RestaurantCategory category = getEntity(categoryId);
        restaurant.setCategory(category);
        restaurantRepository.save(restaurant);
        log.info("Restaurant {} filed under {}", restaurantId, category.getSlug());
    }

    public static RestaurantCategoryDto toDto(RestaurantCategory category, String language) {
        if (category == null) {
            return null;
        }
        return RestaurantCategoryDto.builder()
                .id(category.getId())
                .slug(category.getSlug())
                .name(category.nameFor(language))
                .imageUrl(category.getImageUrl())
                .sortOrder(category.getSortOrder())
                .build();
    }
}
