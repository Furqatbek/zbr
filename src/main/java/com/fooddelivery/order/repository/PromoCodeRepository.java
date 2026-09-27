package com.fooddelivery.order.repository;

import com.fooddelivery.order.entity.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for PromoCode entity.
 */
@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {

    /**
     * Find promo code by code (case-insensitive).
     */
    Optional<PromoCode> findByCodeIgnoreCase(String code);

    /**
     * Check if promo code exists.
     */
    boolean existsByCodeIgnoreCase(String code);

    /**
     * Take one use off the code, or report that there were none left.
     *
     * <p>Read-then-increment loses a race: two checkouts on the last use of a
     * code both read {@code usageCount} below the limit and both spend it. The
     * limit is the whole point of a campaign budget, so the check and the
     * increment happen in one statement and the database decides.
     *
     * @return 1 when a use was taken, 0 when the code is exhausted
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
            "UPDATE PromoCode p SET p.usageCount = p.usageCount + 1 "
                    + "WHERE p.id = :id AND (p.usageLimit IS NULL OR p.usageCount < p.usageLimit)")
    int claimOneUse(@org.springframework.data.repository.query.Param("id") Long id);
}
