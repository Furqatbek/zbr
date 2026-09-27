package com.fooddelivery.restaurant.repository;

import com.fooddelivery.restaurant.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for MenuCategory entity operations.
 */
@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    Optional<MenuCategory> findByRestaurantIdAndExternalSourceAndExternalId(
            Long restaurantId, String externalSource, Long externalId);

    List<MenuCategory> findByRestaurantIdAndExternalSource(Long restaurantId, String externalSource);

    List<MenuCategory> findByRestaurantIdOrderBySortOrderAsc(Long restaurantId);

    List<MenuCategory> findByRestaurantIdAndActiveOrderBySortOrderAsc(Long restaurantId, Boolean active);

    @Query("SELECT mc FROM MenuCategory mc " +
            "LEFT JOIN FETCH mc.items mi " +
            "WHERE mc.restaurant.id = :restaurantId " +
            "AND mc.active = true " +
            "AND (mi IS NULL OR mi.active = true) " +
            // Tie-breakers, not decoration. Two categories sharing sortOrder 1 and
            // most items sitting at 0 is the normal state of an imported menu, and
            // without a second key Postgres is free to return ties in a different
            // order each time — a menu that reshuffles between page loads.
            //
            // The item keys mirror @OrderBy("sortOrder ASC, name ASC") on the
            // collection: a fetch join's ORDER BY governs the fetched order and
            // @OrderBy is ignored, so the two have to agree or the same menu comes
            // back differently depending on which path loaded it.
            "ORDER BY mc.sortOrder ASC, mc.id ASC, mi.sortOrder ASC, mi.name ASC")
    List<MenuCategory> findActiveMenuWithItems(@Param("restaurantId") Long restaurantId);

    boolean existsByRestaurantIdAndName(Long restaurantId, String name);

    @Query("SELECT MAX(mc.sortOrder) FROM MenuCategory mc WHERE mc.restaurant.id = :restaurantId")
    Integer findMaxSortOrderByRestaurantId(@Param("restaurantId") Long restaurantId);
}
