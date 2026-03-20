package com.fooddelivery.notification.service;

import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.courier.repository.CourierRepository;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for fetching data needed for notifications with retry logic.
 * Provides resilient database operations for notification event handlers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDataFetcher {

    private final RestaurantRepository restaurantRepository;
    private final CourierRepository courierRepository;

    /**
     * Fetch restaurant with retry logic for transient database failures.
     *
     * @param restaurantId the restaurant ID
     * @return Optional containing the restaurant if found
     */
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Transactional(readOnly = true)
    public Optional<Restaurant> fetchRestaurant(Long restaurantId) {
        if (restaurantId == null) {
            return Optional.empty();
        }
        log.debug("Fetching restaurant: {}", restaurantId);
        return restaurantRepository.findByIdWithOwner(restaurantId);
    }

    /**
     * Recovery method when restaurant fetch fails after all retries.
     */
    @Recover
    public Optional<Restaurant> recoverFetchRestaurant(Exception e, Long restaurantId) {
        log.error("Failed to fetch restaurant {} after retries: {}", restaurantId, e.getMessage());
        return Optional.empty();
    }

    /**
     * Fetch courier with retry logic for transient database failures.
     *
     * @param courierId the courier ID
     * @return Optional containing the courier if found
     */
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Transactional(readOnly = true)
    public Optional<Courier> fetchCourier(Long courierId) {
        if (courierId == null) {
            return Optional.empty();
        }
        log.debug("Fetching courier: {}", courierId);
        return courierRepository.findByIdWithUser(courierId);
    }

    /**
     * Recovery method when courier fetch fails after all retries.
     */
    @Recover
    public Optional<Courier> recoverFetchCourier(Exception e, Long courierId) {
        log.error("Failed to fetch courier {} after retries: {}", courierId, e.getMessage());
        return Optional.empty();
    }

    /**
     * Helper class to hold restaurant data for notifications.
     */
    public record RestaurantData(Long ownerId, String name) {
        public static RestaurantData empty() {
            return new RestaurantData(null, null);
        }

        public static RestaurantData from(Restaurant restaurant) {
            if (restaurant == null) {
                return empty();
            }
            return new RestaurantData(
                    restaurant.getOwner() != null ? restaurant.getOwner().getId() : null,
                    restaurant.getName()
            );
        }
    }

    /**
     * Helper class to hold courier data for notifications.
     */
    public record CourierData(Long userId, String name) {
        public static CourierData empty() {
            return new CourierData(null, null);
        }

        public static CourierData from(Courier courier) {
            if (courier == null || courier.getUser() == null) {
                return empty();
            }
            return new CourierData(
                    courier.getUser().getId(),
                    courier.getUser().getFullName()
            );
        }
    }

    /**
     * Fetch restaurant data for notifications with retry logic.
     */
    public RestaurantData fetchRestaurantData(Long restaurantId) {
        return fetchRestaurant(restaurantId)
                .map(RestaurantData::from)
                .orElse(RestaurantData.empty());
    }

    /**
     * Fetch courier data for notifications with retry logic.
     */
    public CourierData fetchCourierData(Long courierId) {
        return fetchCourier(courierId)
                .map(CourierData::from)
                .orElse(CourierData.empty());
    }
}
