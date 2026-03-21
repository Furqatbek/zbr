package com.fooddelivery.delivery.proof.repository;

import com.fooddelivery.delivery.proof.entity.DeliveryProof;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for delivery proofs.
 */
@Repository
public interface DeliveryProofRepository extends JpaRepository<DeliveryProof, Long> {

    /**
     * Find proof by order ID.
     */
    Optional<DeliveryProof> findByOrderId(Long orderId);

    /**
     * Check if proof exists for order.
     */
    boolean existsByOrderId(Long orderId);

    /**
     * Find proofs by courier.
     */
    List<DeliveryProof> findByCourierIdOrderByCreatedAtDesc(Long courierId);

    /**
     * Find unverified proofs.
     */
    List<DeliveryProof> findByVerifiedFalse();
}
