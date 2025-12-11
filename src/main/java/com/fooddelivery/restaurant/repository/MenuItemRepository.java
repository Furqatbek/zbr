package com.fooddelivery.restaurant.repository;

import com.fooddelivery.restaurant.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for MenuItem entity operations.
 */
@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByCategoryIdOrderBySortOrderAsc(Long categoryId);

    List<MenuItem> findByCategoryIdAndActiveOrderBySortOrderAsc(Long categoryId, Boolean active);

    @Query("SELECT mi FROM MenuItem mi " +
            "JOIN mi.category mc " +
            "WHERE mc.restaurant.id = :restaurantId " +
            "AND mi.active = true " +
            "ORDER BY mi.sortOrder ASC")
    Page<MenuItem> findActiveByRestaurantId(@Param("restaurantId") Long restaurantId, Pageable pageable);

    @Query("SELECT mi FROM MenuItem mi " +
            "JOIN mi.category mc " +
            "WHERE mc.restaurant.id = :restaurantId " +
            "AND mi.featured = true " +
            "AND mi.active = true")
    List<MenuItem> findFeaturedByRestaurantId(@Param("restaurantId") Long restaurantId);

    @Query("SELECT mi FROM MenuItem mi " +
            "JOIN mi.category mc " +
            "WHERE mc.restaurant.id = :restaurantId " +
            "AND mi.inStock = true " +
            "AND mi.active = true")
    List<MenuItem> findInStockByRestaurantId(@Param("restaurantId") Long restaurantId);

    @Query("SELECT mi FROM MenuItem mi " +
            "LEFT JOIN FETCH mi.variants " +
            "LEFT JOIN FETCH mi.options " +
            "WHERE mi.id = :id")
    Optional<MenuItem> findByIdWithVariantsAndOptions(@Param("id") Long id);

    @Query("SELECT mi FROM MenuItem mi " +
            "JOIN mi.category mc " +
            "WHERE mc.restaurant.id = :restaurantId " +
            "AND (LOWER(mi.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(mi.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<MenuItem> searchByRestaurantId(
            @Param("restaurantId") Long restaurantId,
            @Param("query") String query,
            Pageable pageable);

    @Modifying
    @Query("UPDATE MenuItem mi SET mi.inStock = :inStock WHERE mi.id = :id")
    int updateStockStatus(@Param("id") Long id, @Param("inStock") Boolean inStock);

    @Query("SELECT MAX(mi.sortOrder) FROM MenuItem mi WHERE mi.category.id = :categoryId")
    Integer findMaxSortOrderByCategoryId(@Param("categoryId") Long categoryId);

    boolean existsByCategoryIdAndName(Long categoryId, String name);
}
