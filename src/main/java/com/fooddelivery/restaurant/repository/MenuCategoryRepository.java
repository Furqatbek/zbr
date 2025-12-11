package com.fooddelivery.restaurant.repository;

import com.fooddelivery.restaurant.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for MenuCategory entity operations.
 */
@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    List<MenuCategory> findByRestaurantIdOrderBySortOrderAsc(Long restaurantId);

    List<MenuCategory> findByRestaurantIdAndActiveOrderBySortOrderAsc(Long restaurantId, Boolean active);

    @Query("SELECT mc FROM MenuCategory mc " +
            "LEFT JOIN FETCH mc.items mi " +
            "WHERE mc.restaurant.id = :restaurantId " +
            "AND mc.active = true " +
            "AND (mi IS NULL OR mi.active = true) " +
            "ORDER BY mc.sortOrder ASC, mi.sortOrder ASC")
    List<MenuCategory> findActiveMenuWithItems(@Param("restaurantId") Long restaurantId);

    boolean existsByRestaurantIdAndName(Long restaurantId, String name);

    @Query("SELECT MAX(mc.sortOrder) FROM MenuCategory mc WHERE mc.restaurant.id = :restaurantId")
    Integer findMaxSortOrderByRestaurantId(@Param("restaurantId") Long restaurantId);
}
