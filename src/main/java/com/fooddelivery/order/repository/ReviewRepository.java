package com.fooddelivery.order.repository;

import com.fooddelivery.order.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Review entity.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Find review by order ID.
     */
    Optional<Review> findByOrderId(Long orderId);

    /**
     * Check if review exists for an order.
     */
    boolean existsByOrderId(Long orderId);

    /**
     * Find reviews by restaurant ID with pagination.
     */
    Page<Review> findByRestaurantIdAndIsVisibleTrueOrderByCreatedAtDesc(Long restaurantId, Pageable pageable);

    /**
     * Find reviews by consumer ID.
     */
    Page<Review> findByConsumerIdOrderByCreatedAtDesc(Long consumerId, Pageable pageable);

    /**
     * Find reviews by courier ID.
     */
    Page<Review> findByCourierIdOrderByCreatedAtDesc(Long courierId, Pageable pageable);

    /**
     * Calculate average restaurant rating.
     */
    @Query("SELECT AVG(r.restaurantRating) FROM Review r WHERE r.restaurantId = :restaurantId AND r.restaurantRating IS NOT NULL AND r.isVisible = true")
    Double getAverageRestaurantRating(@Param("restaurantId") Long restaurantId);

    /**
     * Calculate average food rating.
     */
    @Query("SELECT AVG(r.foodRating) FROM Review r WHERE r.restaurantId = :restaurantId AND r.foodRating IS NOT NULL AND r.isVisible = true")
    Double getAverageFoodRating(@Param("restaurantId") Long restaurantId);

    /**
     * Calculate average courier rating.
     */
    @Query("SELECT AVG(r.courierRating) FROM Review r WHERE r.courierId = :courierId AND r.courierRating IS NOT NULL AND r.isVisible = true")
    Double getAverageCourierRating(@Param("courierId") Long courierId);

    /**
     * Count reviews for restaurant.
     */
    long countByRestaurantIdAndIsVisibleTrue(Long restaurantId);
}
