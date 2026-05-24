package com.fooddelivery.order.service;

import com.fooddelivery.auth.service.UserService;
import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.courier.repository.CourierRepository;
import com.fooddelivery.order.dto.CreateReviewRequest;
import com.fooddelivery.order.dto.ReviewDto;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.entity.Review;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.order.repository.ReviewRepository;
import com.fooddelivery.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Service for review operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final CourierRepository courierRepository;
    private final UserService userService;

    /**
     * Submit a review for an order.
     */
    @Transactional
    public ReviewDto submitReview(Long orderId, Long consumerId, CreateReviewRequest request) {
        // Validate order exists and belongs to consumer
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getConsumer().getId().equals(consumerId)) {
            throw new BusinessException("You can only review your own orders");
        }

        // Validate order is completed or delivered
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException("You can only review delivered or completed orders");
        }

        // Check if review already exists
        if (reviewRepository.existsByOrderId(orderId)) {
            throw new BusinessException("A review already exists for this order");
        }

        // Validate at least one rating is provided
        if (request.getRestaurantRating() == null && request.getFoodRating() == null && request.getCourierRating() == null) {
            throw new BusinessException("At least one rating is required");
        }

        // Create review
        Review review = Review.builder()
                .orderId(orderId)
                .consumerId(consumerId)
                .restaurantId(order.getRestaurant().getId())
                .courierId(order.getCourier() != null ? order.getCourier().getId() : null)
                .restaurantRating(request.getRestaurantRating())
                .courierRating(request.getCourierRating())
                .foodRating(request.getFoodRating())
                .comment(request.getComment())
                .tags(request.getTags() != null ? String.join(",", request.getTags()) : null)
                .build();

        review = reviewRepository.save(review);
        log.info("Review created for order {}: restaurant={}, food={}, courier={}",
                orderId, request.getRestaurantRating(), request.getFoodRating(), request.getCourierRating());

        // Update restaurant average rating
        updateRestaurantRating(order.getRestaurant().getId());

        // Update courier average rating
        if (request.getCourierRating() != null && order.getCourier() != null) {
            updateCourierRating(order.getCourier().getId());
        }

        return mapToDto(review, order.getConsumer().getFullName());
    }

    /**
     * Get reviews for a restaurant.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ReviewDto> getRestaurantReviews(Long restaurantId, Pageable pageable) {
        // Validate restaurant exists
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant", "id", restaurantId);
        }

        Page<Review> reviews = reviewRepository.findByRestaurantIdAndIsVisibleTrueOrderByCreatedAtDesc(restaurantId, pageable);

        List<ReviewDto> dtos = reviews.getContent().stream()
                .map(review -> {
                    String consumerName = getConsumerName(review.getConsumerId());
                    return mapToDto(review, consumerName);
                })
                .toList();

        return PagedResponse.from(reviews, dtos);
    }

    /**
     * Get review by order ID.
     */
    @Transactional(readOnly = true)
    public ReviewDto getReviewByOrderId(Long orderId) {
        Review review = reviewRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "orderId", orderId));

        String consumerName = getConsumerName(review.getConsumerId());
        return mapToDto(review, consumerName);
    }

    /**
     * Update restaurant's average rating.
     */
    private void updateRestaurantRating(Long restaurantId) {
        try {
            Double avgRating = reviewRepository.getAverageRestaurantRating(restaurantId);
            long reviewCount = reviewRepository.countByRestaurantIdAndIsVisibleTrue(restaurantId);

            if (avgRating != null) {
                restaurantRepository.findById(restaurantId).ifPresent(restaurant -> {
                    restaurant.setAverageRating(java.math.BigDecimal.valueOf(avgRating));
                    restaurant.setTotalRatings((int) reviewCount);
                    restaurantRepository.save(restaurant);
                });
            }
        } catch (Exception e) {
            log.warn("Failed to update restaurant rating: {}", e.getMessage());
        }
    }

    private void updateCourierRating(Long courierId) {
        try {
            Double avgRating = reviewRepository.getAverageCourierRating(courierId);
            if (avgRating != null) {
                courierRepository.findById(courierId).ifPresent(courier -> {
                    courier.setAverageRating(java.math.BigDecimal.valueOf(avgRating));
                    courierRepository.save(courier);
                });
            }
        } catch (Exception e) {
            log.warn("Failed to update courier rating: {}", e.getMessage());
        }
    }

    private String getConsumerName(Long consumerId) {
        try {
            return userService.getUserById(consumerId).getFullName();
        } catch (Exception e) {
            return "Anonymous";
        }
    }

    private ReviewDto mapToDto(Review review, String consumerName) {
        return ReviewDto.builder()
                .id(review.getId())
                .orderId(review.getOrderId())
                .consumerId(review.getConsumerId())
                .consumerName(maskName(consumerName))
                .restaurantId(review.getRestaurantId())
                .courierId(review.getCourierId())
                .restaurantRating(review.getRestaurantRating())
                .courierRating(review.getCourierRating())
                .foodRating(review.getFoodRating())
                .comment(review.getComment())
                .tags(review.getTags() != null ? Arrays.asList(review.getTags().split(",")) : null)
                .createdAt(review.getCreatedAt())
                .build();
    }

    /**
     * Mask consumer name for privacy (e.g., "John Doe" -> "J***n D.")
     */
    private String maskName(String fullName) {
        if (fullName == null || fullName.length() < 2) {
            return "Anonymous";
        }

        String[] parts = fullName.split(" ");
        StringBuilder masked = new StringBuilder();

        for (String part : parts) {
            if (part.length() > 0) {
                if (masked.length() > 0) masked.append(" ");
                if (part.length() == 1) {
                    masked.append(part.charAt(0)).append(".");
                } else if (part.length() == 2) {
                    masked.append(part.charAt(0)).append("*");
                } else {
                    masked.append(part.charAt(0))
                            .append("*".repeat(part.length() - 2))
                            .append(part.charAt(part.length() - 1));
                }
            }
        }

        return masked.toString();
    }
}
