package com.fooddelivery.auth.repository;

import com.fooddelivery.auth.entity.ConsumerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ConsumerAddress entity.
 */
@Repository
public interface ConsumerAddressRepository extends JpaRepository<ConsumerAddress, Long> {

    /**
     * Find all addresses for a user ordered by default first, then by creation date.
     */
    @Query("SELECT a FROM ConsumerAddress a WHERE a.userId = :userId ORDER BY a.isDefault DESC, a.createdAt DESC")
    List<ConsumerAddress> findByUserIdOrderByIsDefaultDescCreatedAtDesc(@Param("userId") Long userId);

    /**
     * Find address by ID and user ID.
     */
    Optional<ConsumerAddress> findByIdAndUserId(Long id, Long userId);

    /**
     * Find the default address for a user.
     */
    Optional<ConsumerAddress> findByUserIdAndIsDefaultTrue(Long userId);

    /**
     * Count addresses for a user.
     */
    long countByUserId(Long userId);

    /**
     * Reset all addresses to non-default for a user.
     */
    @Modifying
    @Query("UPDATE ConsumerAddress a SET a.isDefault = false WHERE a.userId = :userId")
    void resetDefaultForUser(@Param("userId") Long userId);

    /**
     * Delete all addresses for a user.
     */
    void deleteByUserId(Long userId);
}
