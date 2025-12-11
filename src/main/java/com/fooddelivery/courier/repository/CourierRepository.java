package com.fooddelivery.courier.repository;

import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.courier.entity.CourierStatus;
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
 * Repository for Courier entity operations.
 */
@Repository
public interface CourierRepository extends JpaRepository<Courier, Long> {

    Optional<Courier> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Page<Courier> findByStatus(CourierStatus status, Pageable pageable);

    @Query("SELECT c FROM Courier c WHERE c.status = 'AVAILABLE' AND c.verified = true " +
            "AND c.currentOrderCount < c.maxConcurrentOrders")
    List<Courier> findAvailableCouriers();

    @Query(value = "SELECT c.* FROM couriers c " +
            "WHERE c.status = 'AVAILABLE' " +
            "AND c.is_verified = true " +
            "AND c.current_order_count < c.max_concurrent_orders " +
            "AND (6371 * acos(cos(radians(:lat)) * cos(radians(c.current_lat)) * " +
            "cos(radians(c.current_lng) - radians(:lng)) + sin(radians(:lat)) * sin(radians(c.current_lat)))) < :radius " +
            "ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(c.current_lat)) * " +
            "cos(radians(c.current_lng) - radians(:lng)) + sin(radians(:lat)) * sin(radians(c.current_lat)))) ASC",
            nativeQuery = true)
    List<Courier> findNearbyCouriers(
            @Param("lat") BigDecimal latitude,
            @Param("lng") BigDecimal longitude,
            @Param("radius") double radiusKm);

    @Query("SELECT c FROM Courier c WHERE c.verified = false AND c.documentsSubmitted = true")
    Page<Courier> findPendingVerification(Pageable pageable);

    @Modifying
    @Query("UPDATE Courier c SET c.status = :status WHERE c.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") CourierStatus status);

    @Modifying
    @Query("UPDATE Courier c SET c.currentLat = :lat, c.currentLng = :lng, " +
            "c.locationUpdatedAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    int updateLocation(@Param("id") Long id, @Param("lat") BigDecimal lat, @Param("lng") BigDecimal lng);

    @Query("SELECT COUNT(c) FROM Courier c WHERE c.status = :status")
    long countByStatus(@Param("status") CourierStatus status);
}
