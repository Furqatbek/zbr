package com.fooddelivery.compensation.repository;

import com.fooddelivery.compensation.entity.CustomerCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

/**
 * Repository for customer credits.
 */
@Repository
public interface CustomerCreditRepository extends JpaRepository<CustomerCredit, Long> {

    /**
     * Find credit by user ID.
     */
    Optional<CustomerCredit> findByUserId(Long userId);

    /**
     * Find credit by user ID with pessimistic lock for safe updates.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CustomerCredit c WHERE c.user.id = :userId")
    Optional<CustomerCredit> findByUserIdWithLock(Long userId);

    /**
     * Check if user has a credit record.
     */
    boolean existsByUserId(Long userId);
}
