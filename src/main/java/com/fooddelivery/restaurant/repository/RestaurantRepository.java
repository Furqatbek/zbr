package com.fooddelivery.restaurant.repository;

import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.entity.RestaurantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Restaurant entity operations.
 */
@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Restaurant> findByOwnerId(Long ownerId);

    Page<Restaurant> findByOwnerId(Long ownerId, Pageable pageable);

    Page<Restaurant> findByStatus(RestaurantStatus status, Pageable pageable);

    @Query("SELECT r FROM Restaurant r WHERE r.status = 'ACTIVE' AND r.isOpen = true")
    Page<Restaurant> findActiveAndOpen(Pageable pageable);

    @Query("SELECT r FROM Restaurant r WHERE r.status = 'ACTIVE' " +
            "AND (LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Restaurant> searchByNameOrDescription(@Param("query") String query, Pageable pageable);

    @Query("SELECT r FROM Restaurant r WHERE r.status = 'ACTIVE' " +
            "AND r.city = :city ORDER BY r.averageRating DESC")
    Page<Restaurant> findByCityOrderByRating(@Param("city") String city, Pageable pageable);

    @Query("SELECT r FROM Restaurant r WHERE r.status = 'ACTIVE' AND r.featured = true")
    Page<Restaurant> findFeatured(Pageable pageable);

    @Query(value = "SELECT r.* FROM restaurants r " +
            "WHERE r.status = 'ACTIVE' " +
            "AND (6371 * acos(cos(radians(:lat)) * cos(radians(r.latitude)) * " +
            "cos(radians(r.longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(r.latitude)))) < :radius",
            nativeQuery = true)
    List<Restaurant> findNearbyRestaurants(
            @Param("lat") BigDecimal latitude,
            @Param("lng") BigDecimal longitude,
            @Param("radius") double radiusKm);

    @Modifying
    @Query("UPDATE Restaurant r SET r.isOpen = :isOpen WHERE r.id = :id")
    int updateOpenStatus(@Param("id") Long id, @Param("isOpen") Boolean isOpen);

    @Modifying
    @Query("UPDATE Restaurant r SET r.status = :status WHERE r.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") RestaurantStatus status);

    @Modifying
    @Query("UPDATE Restaurant r SET r.totalOrders = r.totalOrders + 1 WHERE r.id = :id")
    int incrementOrderCount(@Param("id") Long id);

    @Query("SELECT COUNT(r) FROM Restaurant r WHERE r.status = :status")
    long countByStatus(@Param("status") RestaurantStatus status);
}
