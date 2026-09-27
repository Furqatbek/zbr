package com.fooddelivery.order.repository;

import com.fooddelivery.order.entity.PromoCodeUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromoCodeUsageRepository extends JpaRepository<PromoCodeUsage, Long> {

    /** How many times this customer has already used this code. */
    long countByPromoCodeIdAndUserId(Long promoCodeId, Long userId);
}
