package com.fooddelivery.restaurant.repository;

import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.entity.RestaurantStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * The filters behind the restaurant list: open, cuisine, featured, search.
 *
 * <p>Composed rather than enumerated. Each one is optional, and an absent
 * filter returns null so Spring Data drops it — no {@code IS NULL OR} clause
 * whose parameter type Postgres then has to guess at.
 */
public final class RestaurantSpecifications {

    private RestaurantSpecifications() {
    }

    /** Active and open — what a customer may order from right now. */
    public static Specification<Restaurant> openForOrders() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), RestaurantStatus.ACTIVE),
                cb.isTrue(root.get("isOpen")));
    }

    /** Active, whether or not the doors are open. */
    public static Specification<Restaurant> active() {
        return (root, query, cb) -> cb.equal(root.get("status"), RestaurantStatus.ACTIVE);
    }

    public static Specification<Restaurant> inCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Restaurant> featured(Boolean featured) {
        if (featured == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("featured"), featured);
    }

    /** Name or description contains the text, case-insensitively. */
    public static Specification<Restaurant> matching(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String pattern = "%" + text.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern));
    }
}
