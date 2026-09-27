package com.fooddelivery.order.repository;

import com.fooddelivery.order.entity.DeliveryCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryCreditRepository extends JpaRepository<DeliveryCredit, Long> {

    /**
     * The oldest credit this customer can still spend.
     *
     * <p>Oldest first so a credit with an expiry is used before one without,
     * which is the order that wastes the fewest of them.
     */
    @Query("SELECT c FROM DeliveryCredit c WHERE c.userId = :userId AND c.usedAt IS NULL "
            + "AND (c.expiresAt IS NULL OR c.expiresAt > :now) ORDER BY c.grantedAt ASC")
    List<DeliveryCredit> findSpendable(@Param("userId") Long userId,
                                       @Param("now") LocalDateTime now);

    boolean existsByUserIdAndReason(Long userId, DeliveryCredit.Reason reason);

    Optional<DeliveryCredit> findBySourceReferralId(Long referralId);

    long countByUserIdAndUsedAtIsNull(Long userId);

    @Query("SELECT COUNT(c) FROM DeliveryCredit c WHERE c.userId = :userId AND c.usedAt IS NOT NULL")
    long countSpentByUserId(@Param("userId") Long userId);
}
