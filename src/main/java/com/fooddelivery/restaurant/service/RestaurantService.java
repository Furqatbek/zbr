package com.fooddelivery.restaurant.service;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserStatus;
import com.fooddelivery.auth.service.UserService;
import com.fooddelivery.common.annotation.Auditable;
import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.common.service.ImageStorageService;
import com.fooddelivery.common.util.SlugUtils;
import com.fooddelivery.restaurant.dto.*;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.entity.RestaurantStatus;
import com.fooddelivery.restaurant.mapper.RestaurantMapper;
import com.fooddelivery.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for restaurant operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMapper restaurantMapper;
    private final UserService userService;
    private final ImageStorageService imageStorageService;

    /**
     * Create a new restaurant.
     */
    @Transactional
    @Auditable(action = "CREATE_RESTAURANT", entityType = "Restaurant")
    public RestaurantDto createRestaurant(Long ownerId, CreateRestaurantRequest request) {
        log.info("Creating restaurant: {} for owner: {}", request.getName(), ownerId);

        User owner = userService.getUserEntityById(ownerId);

        Restaurant restaurant = restaurantMapper.toEntity(request);
        restaurant.setOwner(owner);
        restaurant.setSlug(generateUniqueSlug(request.getName()));
        restaurant.setStatus(RestaurantStatus.PENDING);
        restaurant.setIsOpen(false);

        restaurant = restaurantRepository.save(restaurant);
        log.info("Restaurant created with id: {}", restaurant.getId());

        return restaurantMapper.toDto(restaurant);
    }

    /**
     * Get restaurant by ID.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "restaurants", key = "#id")
    public RestaurantDto getRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", id));
        return restaurantMapper.toDto(restaurant);
    }

    /**
     * Get restaurant by slug.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "restaurants", key = "'slug:' + #slug")
    public RestaurantDto getRestaurantBySlug(String slug) {
        Restaurant restaurant = restaurantRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "slug", slug));
        return restaurantMapper.toDto(restaurant);
    }

    /**
     * Get restaurant entity by ID (internal use).
     */
    @Transactional(readOnly = true)
    public Restaurant getRestaurantEntityById(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", id));
    }

    /**
     * Update restaurant.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "restaurants", key = "#id"),
            // The SAME restaurant is cached under two keys. Evicting only #id
            // left GET /restaurants/slug/{slug} serving the pre-update row for
            // the rest of the 5-minute TTL — most visibly, a restaurant the
            // owner had just closed still showed as open. The slug is assigned
            // once at creation and never regenerated, so #result.slug is the
            // key the entry actually lives under.
            @CacheEvict(value = "restaurants", key = "'slug:' + #result.slug",
                        condition = "#result != null")
    })
    @Auditable(action = "UPDATE_RESTAURANT", entityType = "Restaurant")
    public RestaurantDto updateRestaurant(Long id, CreateRestaurantRequest request) {
        Restaurant restaurant = getRestaurantEntityById(id);

        // Update fields
        if (request.getName() != null) {
            restaurant.setName(request.getName());
        }
        if (request.getDescription() != null) {
            restaurant.setDescription(request.getDescription());
        }
        if (request.getPhone() != null) {
            restaurant.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            restaurant.setEmail(request.getEmail());
        }
        if (request.getAddressLine1() != null) {
            restaurant.setAddressLine1(request.getAddressLine1());
        }
        if (request.getAddressLine2() != null) {
            restaurant.setAddressLine2(request.getAddressLine2());
        }
        if (request.getCity() != null) {
            restaurant.setCity(request.getCity());
        }
        if (request.getState() != null) {
            restaurant.setState(request.getState());
        }
        if (request.getPostalCode() != null) {
            restaurant.setPostalCode(request.getPostalCode());
        }
        if (request.getCountry() != null) {
            restaurant.setCountry(request.getCountry());
        }
        if (request.getLatitude() != null) {
            restaurant.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            restaurant.setLongitude(request.getLongitude());
        }
        if (request.getAcceptsDelivery() != null) {
            restaurant.setAcceptsDelivery(request.getAcceptsDelivery());
        }
        if (request.getAcceptsTakeaway() != null) {
            restaurant.setAcceptsTakeaway(request.getAcceptsTakeaway());
        }
        if (request.getAcceptsDineIn() != null) {
            restaurant.setAcceptsDineIn(request.getAcceptsDineIn());
        }
        if (request.getMinimumOrder() != null) {
            restaurant.setMinimumOrder(request.getMinimumOrder());
        }
        if (request.getDeliveryFee() != null) {
            restaurant.setDeliveryFee(request.getDeliveryFee());
        }
        if (request.getDeliveryRadiusKm() != null) {
            restaurant.setDeliveryRadiusKm(request.getDeliveryRadiusKm());
        }
        if (request.getAveragePrepTimeMinutes() != null) {
            restaurant.setAveragePrepTimeMinutes(request.getAveragePrepTimeMinutes());
        }
        if (request.getOpensAt() != null) {
            restaurant.setOpensAt(request.getOpensAt());
        }
        if (request.getClosesAt() != null) {
            restaurant.setClosesAt(request.getClosesAt());
        }

        restaurant = restaurantRepository.save(restaurant);
        log.info("Restaurant updated: {}", restaurant.getId());

        return restaurantMapper.toDto(restaurant);
    }

    /**
     * Partial update — only non-null fields are updated.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "restaurants", key = "#id"),
            // The SAME restaurant is cached under two keys. Evicting only #id
            // left GET /restaurants/slug/{slug} serving the pre-update row for
            // the rest of the 5-minute TTL — most visibly, a restaurant the
            // owner had just closed still showed as open. The slug is assigned
            // once at creation and never regenerated, so #result.slug is the
            // key the entry actually lives under.
            @CacheEvict(value = "restaurants", key = "'slug:' + #result.slug",
                        condition = "#result != null")
    })
    public RestaurantDto updateRestaurantPartial(Long id, UpdateRestaurantRequest request) {
        Restaurant restaurant = getRestaurantEntityById(id);

        if (request.getName() != null) restaurant.setName(request.getName());
        if (request.getDescription() != null) restaurant.setDescription(request.getDescription());
        if (request.getPhone() != null) restaurant.setPhone(request.getPhone());
        if (request.getEmail() != null) restaurant.setEmail(request.getEmail());
        if (request.getAddressLine1() != null) restaurant.setAddressLine1(request.getAddressLine1());
        if (request.getAddressLine2() != null) restaurant.setAddressLine2(request.getAddressLine2());
        if (request.getCity() != null) restaurant.setCity(request.getCity());
        if (request.getState() != null) restaurant.setState(request.getState());
        if (request.getPostalCode() != null) restaurant.setPostalCode(request.getPostalCode());
        if (request.getCountry() != null) restaurant.setCountry(request.getCountry());
        if (request.getLatitude() != null) restaurant.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) restaurant.setLongitude(request.getLongitude());
        if (request.getAcceptsDelivery() != null) restaurant.setAcceptsDelivery(request.getAcceptsDelivery());
        if (request.getAcceptsTakeaway() != null) restaurant.setAcceptsTakeaway(request.getAcceptsTakeaway());
        if (request.getAcceptsDineIn() != null) restaurant.setAcceptsDineIn(request.getAcceptsDineIn());
        if (request.getMinimumOrder() != null) restaurant.setMinimumOrder(request.getMinimumOrder());
        if (request.getDeliveryFee() != null) restaurant.setDeliveryFee(request.getDeliveryFee());
        if (request.getDeliveryRadiusKm() != null) restaurant.setDeliveryRadiusKm(request.getDeliveryRadiusKm());
        if (request.getAveragePrepTimeMinutes() != null) restaurant.setAveragePrepTimeMinutes(request.getAveragePrepTimeMinutes());
        if (request.getOpensAt() != null) restaurant.setOpensAt(request.getOpensAt());
        if (request.getClosesAt() != null) restaurant.setClosesAt(request.getClosesAt());

        restaurant = restaurantRepository.save(restaurant);
        log.info("Restaurant partially updated: {}", restaurant.getId());

        return restaurantMapper.toDto(restaurant);
    }

    /**
     * Upload and update restaurant image (logo or cover).
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "restaurants", key = "#id"),
            // The SAME restaurant is cached under two keys. Evicting only #id
            // left GET /restaurants/slug/{slug} serving the pre-update row for
            // the rest of the 5-minute TTL — most visibly, a restaurant the
            // owner had just closed still showed as open. The slug is assigned
            // once at creation and never regenerated, so #result.slug is the
            // key the entry actually lives under.
            @CacheEvict(value = "restaurants", key = "'slug:' + #result.slug",
                        condition = "#result != null")
    })
    public RestaurantDto updateRestaurantImage(Long id, MultipartFile file, String imageType) {
        Restaurant restaurant = getRestaurantEntityById(id);

        String category = "restaurants/" + id + "/" + imageType;
        ImageStorageService.ImageInfo imageInfo = imageStorageService.storeImage(file, category);

        if ("logo".equals(imageType)) {
            restaurant.setLogoUrl(imageInfo.getUrl());
        } else if ("cover".equals(imageType)) {
            restaurant.setCoverImageUrl(imageInfo.getUrl());
        }

        restaurant = restaurantRepository.save(restaurant);
        log.info("Restaurant {} {} image updated: {}", id, imageType, imageInfo.getUrl());

        return restaurantMapper.toDto(restaurant);
    }

    /**
     * Update restaurant status.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "restaurants", key = "#id"),
            // The SAME restaurant is cached under two keys. Evicting only #id
            // left GET /restaurants/slug/{slug} serving the pre-update row for
            // the rest of the 5-minute TTL — most visibly, a restaurant the
            // owner had just closed still showed as open. The slug is assigned
            // once at creation and never regenerated, so #result.slug is the
            // key the entry actually lives under.
            @CacheEvict(value = "restaurants", key = "'slug:' + #result.slug",
                        condition = "#result != null")
    })
    @Auditable(action = "UPDATE_RESTAURANT_STATUS", entityType = "Restaurant")
    public RestaurantDto updateStatus(Long id, RestaurantStatus status) {
        Restaurant restaurant = getRestaurantEntityById(id);

        if (status == RestaurantStatus.ACTIVE && restaurant.getStatus() == RestaurantStatus.PENDING) {
            log.info("Approving restaurant: {}", restaurant.getName());
        }

        restaurant.setStatus(status);
        restaurant = restaurantRepository.save(restaurant);

        return restaurantMapper.toDto(restaurant);
    }

    /**
     * Toggle restaurant open/closed status.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "restaurants", key = "#id"),
            // The SAME restaurant is cached under two keys. Evicting only #id
            // left GET /restaurants/slug/{slug} serving the pre-update row for
            // the rest of the 5-minute TTL — most visibly, a restaurant the
            // owner had just closed still showed as open. The slug is assigned
            // once at creation and never regenerated, so #result.slug is the
            // key the entry actually lives under.
            @CacheEvict(value = "restaurants", key = "'slug:' + #result.slug",
                        condition = "#result != null")
    })
    public RestaurantDto toggleOpenStatus(Long id, Boolean isOpen) {
        Restaurant restaurant = getRestaurantEntityById(id);

        if (restaurant.getStatus() != RestaurantStatus.ACTIVE) {
            throw new BusinessException("Cannot change open status for inactive restaurant");
        }

        restaurant.setIsOpen(isOpen);
        restaurant = restaurantRepository.save(restaurant);
        log.info("Restaurant {} is now {}", restaurant.getName(), isOpen ? "open" : "closed");

        return restaurantMapper.toDto(restaurant);
    }

    /**
     * Move a restaurant to a different owner.
     *
     * <p>Before this existed, {@code owner_id} was written exactly once — from
     * whoever called {@code POST /restaurants} — and could only be changed with
     * a hand-written UPDATE against production, which also silently skipped the
     * cache eviction below.
     *
     * <p>Ownership is exclusive: there is no co-ownership and no handover
     * period, so this removes the previous owner's access completely and at
     * once. The previous owner KEEPS the RESTAURANT_OWNER role — they may still
     * own other restaurants, and revoking it here would break them. Strip it
     * separately via {@code DELETE /users/{id}/roles/RESTAURANT_OWNER} if they
     * are leaving the platform entirely.
     *
     * @return the restaurant, with {@code ownerId} updated
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "restaurants", key = "#restaurantId"),
            @CacheEvict(value = "restaurants", key = "'slug:' + #result.slug",
                        condition = "#result != null")
    })
    @Auditable(action = "TRANSFER_RESTAURANT_OWNERSHIP", entityType = "Restaurant")
    public RestaurantDto transferOwnership(Long restaurantId, Long newOwnerId) {
        Restaurant restaurant = getRestaurantEntityById(restaurantId);
        Long previousOwnerId = restaurant.getOwner().getId();

        // Idempotent: an admin retrying after a timeout should not get an error
        // for a transfer that already succeeded.
        if (previousOwnerId.equals(newOwnerId)) {
            log.info("Restaurant {} is already owned by user {} — nothing to do",
                    restaurantId, newOwnerId);
            return restaurantMapper.toDto(restaurant);
        }

        User newOwner = userService.getUserEntityById(newOwnerId);

        // A restaurant handed to a suspended or deleted account is unreachable
        // by its owner and unrecoverable without another transfer. Refuse
        // rather than let an admin strand a live business.
        if (newOwner.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Cannot transfer to a " + newOwner.getStatus()
                    + " account. Activate the user first.");
        }

        // The role is what lets them reach the owner endpoints at all. Granting
        // it here is the difference between a transfer that works and one that
        // leaves the new owner with 403 on everything.
        if (!newOwner.hasRole(Role.RESTAURANT_OWNER)) {
            newOwner.addRole(Role.RESTAURANT_OWNER);
            log.info("Granted RESTAURANT_OWNER to user {} as part of the transfer", newOwnerId);
        }

        restaurant.setOwner(newOwner);
        restaurant = restaurantRepository.save(restaurant);

        log.info("Restaurant {} ('{}') transferred from user {} to user {}",
                restaurantId, restaurant.getName(), previousOwnerId, newOwnerId);

        return restaurantMapper.toDto(restaurant);
    }

    /**
     * Get restaurants by owner.
     */
    @Transactional(readOnly = true)
    public List<RestaurantDto> getRestaurantsByOwner(Long ownerId) {
        List<Restaurant> restaurants = restaurantRepository.findByOwnerId(ownerId);
        return restaurantMapper.toDtoList(restaurants);
    }

    /**
     * Get all restaurants with pagination.
     */
    @Transactional(readOnly = true)
    public PagedResponse<RestaurantDto> getAllRestaurants(Pageable pageable) {
        Page<Restaurant> restaurants = restaurantRepository.findAll(pageable);
        return PagedResponse.from(restaurants, restaurantMapper.toDtoList(restaurants.getContent()));
    }

    /**
     * Get active restaurants.
     */
    @Transactional(readOnly = true)
    public PagedResponse<RestaurantDto> getActiveRestaurants(Pageable pageable) {
        Page<Restaurant> restaurants = restaurantRepository.findActiveAndOpen(pageable);
        return PagedResponse.from(restaurants, restaurantMapper.toDtoList(restaurants.getContent()));
    }

    /**
     * Search restaurants.
     */
    @Transactional(readOnly = true)
    public PagedResponse<RestaurantDto> searchRestaurants(String query, Pageable pageable) {
        Page<Restaurant> restaurants = restaurantRepository.searchByNameOrDescription(query, pageable);
        return PagedResponse.from(restaurants, restaurantMapper.toDtoList(restaurants.getContent()));
    }

    /**
     * Get featured restaurants.
     */
    @Transactional(readOnly = true)
    public PagedResponse<RestaurantDto> getFeaturedRestaurants(Pageable pageable) {
        Page<Restaurant> restaurants = restaurantRepository.findFeatured(pageable);
        return PagedResponse.from(restaurants, restaurantMapper.toDtoList(restaurants.getContent()));
    }

    /**
     * Get nearby restaurants.
     */
    @Transactional(readOnly = true)
    public List<RestaurantDto> getNearbyRestaurants(BigDecimal latitude, BigDecimal longitude, double radiusKm) {
        List<Restaurant> restaurants = restaurantRepository.findNearbyRestaurants(latitude, longitude, radiusKm);
        return restaurantMapper.toDtoList(restaurants);
    }

    /**
     * Check if user is restaurant owner.
     */
    @Transactional(readOnly = true)
    public boolean isRestaurantOwner(Long restaurantId, Long userId) {
        Restaurant restaurant = getRestaurantEntityById(restaurantId);
        return restaurant.getOwner().getId().equals(userId);
    }

    /**
     * Validate that user owns the restaurant or has admin/platform role.
     * Throws BusinessException if not authorized.
     * This ensures Restaurant X cannot access Restaurant Y's data.
     */
    @Transactional(readOnly = true)
    public void validateRestaurantAccess(Long restaurantId, Long userId, boolean isAdminOrPlatform) {
        if (isAdminOrPlatform) {
            return; // Admin and Platform can access any restaurant
        }

        Restaurant restaurant = getRestaurantEntityById(restaurantId);
        Long actualOwnerId = restaurant.getOwner().getId();
        if (!actualOwnerId.equals(userId)) {
            log.warn("SECURITY: User {} (not owner) attempted to access restaurant {} owned by user {}",
                    userId, restaurantId, actualOwnerId);
            throw new BusinessException("You don't have permission to access this restaurant");
        }
    }

    /**
     * Check if user has access to restaurant (owner or staff).
     */
    @Transactional(readOnly = true)
    public boolean hasRestaurantAccess(Long restaurantId, Long userId) {
        Restaurant restaurant = getRestaurantEntityById(restaurantId);
        // Owner always has access
        if (restaurant.getOwner().getId().equals(userId)) {
            return true;
        }
        // TODO: Check if user is staff of this restaurant
        // return restaurantStaffRepository.existsByRestaurantIdAndUserId(restaurantId, userId);
        return false;
    }

    private String generateUniqueSlug(String name) {
        String baseSlug = SlugUtils.toSlug(name);
        String slug = baseSlug;
        int counter = 1;

        while (restaurantRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        return slug;
    }
}
