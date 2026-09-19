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

    Optional<MenuItem> findByCategoryIdAndExternalSourceAndExternalId(
            Long categoryId, String externalSource, Long externalId);

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

    /**
     * Every live item in this restaurant that came from an external system, for
     * reconciling against a fresh snapshot of that system's menu.
     *
     * <p>{@code externalId IS NOT NULL} is what protects items the restaurant
     * created by hand: they carry no external id, were never in the upstream
     * menu, and must never be retired by a sync that does not know about them.
     */
    @Query("SELECT mi FROM MenuItem mi " +
            "JOIN mi.category mc " +
            "WHERE mc.restaurant.id = :restaurantId " +
            "AND mi.externalSource = :externalSource " +
            "AND mi.externalId IS NOT NULL " +
            "AND mi.active = true")
    List<MenuItem> findActiveExternalItems(@Param("restaurantId") Long restaurantId,
                                           @Param("externalSource") String externalSource);

    /**
     * Live items stamped with an external system but carrying no id from it.
     *
     * <p>These are wreckage from before unkeyed products were refused: a null
     * external id made the upsert lookup match on {@code IS NULL}, so every
     * unkeyed product landed on the same row. They can no longer be matched to
     * anything upstream, so a sync can neither update nor retire them — they
     * just sit there. Counted so a sync can say so rather than leave them
     * invisible.
     */
    @Query("SELECT COUNT(mi) FROM MenuItem mi " +
            "JOIN mi.category mc " +
            "WHERE mc.restaurant.id = :restaurantId " +
            "AND mi.externalSource = :externalSource " +
            "AND mi.externalId IS NULL " +
            "AND mi.active = true")
    long countUnkeyedExternalItems(@Param("restaurantId") Long restaurantId,
                                   @Param("externalSource") String externalSource);
}
