package com.fooddelivery.restaurant.repository;

import com.fooddelivery.restaurant.entity.RestaurantCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantCategoryRepository extends JpaRepository<RestaurantCategory, Long> {

    Optional<RestaurantCategory> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<RestaurantCategory> findByActiveTrueOrderBySortOrderAscIdAsc();

    /**
     * Categories a customer can actually use right now.
     *
     * <p>A chip that filters to an empty list is a dead end, so a category is
     * only offered when at least one restaurant in it is active AND open. That
     * makes the rail change through the day, which is correct: a cuisine whose
     * only venue closes at 22:00 should stop being offered at 22:00.
     */
    @Query("SELECT DISTINCT c FROM RestaurantCategory c "
            + "JOIN Restaurant r ON r.category = c "
            + "WHERE c.active = true AND r.status = 'ACTIVE' AND r.isOpen = true "
            + "ORDER BY c.sortOrder ASC, c.id ASC")
    List<RestaurantCategory> findWithOpenRestaurants();
}
