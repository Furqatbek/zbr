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

    @Query("SELECT c FROM Courier c JOIN FETCH c.user WHERE c.user.id = :userId")
    Optional<Courier> findByUserId(@Param("userId") Long userId);

    boolean existsByUserId(Long userId);

    @Query(value = "SELECT c FROM Courier c JOIN FETCH c.user WHERE c.status = :status",
           countQuery = "SELECT COUNT(c) FROM Courier c WHERE c.status = :status")
    Page<Courier> findByStatus(@Param("status") CourierStatus status, Pageable pageable);

    @Query("SELECT c FROM Courier c JOIN FETCH c.user " +
            "WHERE c.status = com.fooddelivery.courier.entity.CourierStatus.AVAILABLE " +
            "AND c.verified = true AND c.currentOrderCount < c.maxConcurrentOrders")
    List<Courier> findAvailableCouriers();

    @Query(value = "SELECT c.id FROM couriers c " +
            "WHERE c.status = 'AVAILABLE' " +
            "AND c.is_verified = true " +
            "AND c.current_order_count < c.max_concurrent_orders " +
            "AND (6371 * acos(cos(radians(:lat)) * cos(radians(c.current_lat)) * " +
            "cos(radians(c.current_lng) - radians(:lng)) + sin(radians(:lat)) * sin(radians(c.current_lat)))) < :radius " +
            "ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(c.current_lat)) * " +
            "cos(radians(c.current_lng) - radians(:lng)) + sin(radians(:lat)) * sin(radians(c.current_lat)))) ASC",
            nativeQuery = true)
    List<Long> findNearbyCourierIds(
            @Param("lat") BigDecimal latitude,
            @Param("lng") BigDecimal longitude,
            @Param("radius") double radiusKm);

    /**
     * Find couriers by IDs with user data.
     */
    @Query("SELECT c FROM Courier c JOIN FETCH c.user WHERE c.id IN :ids")
    List<Courier> findByIdsWithUser(@Param("ids") List<Long> ids);

    @Query(value = "SELECT c FROM Courier c JOIN FETCH c.user WHERE c.verified = false AND c.documentsSubmitted = true",
           countQuery = "SELECT COUNT(c) FROM Courier c WHERE c.verified = false AND c.documentsSubmitted = true")
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

    /**
     * Find all online couriers (AVAILABLE or BUSY).
     */
    @Query("SELECT c FROM Courier c JOIN FETCH c.user " +
            "WHERE (c.status = com.fooddelivery.courier.entity.CourierStatus.AVAILABLE " +
            "OR c.status = com.fooddelivery.courier.entity.CourierStatus.BUSY) AND c.verified = true")
    List<Courier> findOnlineCouriers();

    /**
     * Find all pending approval couriers.
     */
    @Query(value = "SELECT c FROM Courier c JOIN FETCH c.user WHERE c.status = com.fooddelivery.courier.entity.CourierStatus.PENDING_APPROVAL",
           countQuery = "SELECT COUNT(c) FROM Courier c WHERE c.status = com.fooddelivery.courier.entity.CourierStatus.PENDING_APPROVAL")
    Page<Courier> findPendingApproval(Pageable pageable);

    /**
     * Find couriers by multiple statuses.
     */
    @Query(value = "SELECT c FROM Courier c JOIN FETCH c.user WHERE c.status IN :statuses",
           countQuery = "SELECT COUNT(c) FROM Courier c WHERE c.status IN :statuses")
    Page<Courier> findByStatusIn(@Param("statuses") List<CourierStatus> statuses, Pageable pageable);

    /**
     * Count couriers by verified status.
     */
    long countByVerified(boolean verified);

    /**
     * Find all couriers with user data.
     */
    @Query(value = "SELECT c FROM Courier c JOIN FETCH c.user",
           countQuery = "SELECT COUNT(c) FROM Courier c")
    Page<Courier> findAllWithUser(Pageable pageable);

    /**
     * Find courier by ID with user data.
     */
    @Query("SELECT c FROM Courier c JOIN FETCH c.user WHERE c.id = :id")
    Optional<Courier> findByIdWithUser(@Param("id") Long id);

    /**
     * Find couriers with location data for map display.
     */
    @Query("SELECT c FROM Courier c JOIN FETCH c.user " +
            "WHERE (c.status = com.fooddelivery.courier.entity.CourierStatus.AVAILABLE " +
            "OR c.status = com.fooddelivery.courier.entity.CourierStatus.BUSY) " +
            "AND c.verified = true AND c.currentLat IS NOT NULL AND c.currentLng IS NOT NULL")
    List<Courier> findOnlineCouriersWithLocation();
}
