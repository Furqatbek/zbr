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
}
