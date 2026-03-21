package com.fooddelivery.delivery.proof.service;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.courier.repository.CourierRepository;
import com.fooddelivery.delivery.proof.entity.DeliveryProof;
import com.fooddelivery.delivery.proof.repository.DeliveryProofRepository;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing delivery proofs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryProofService {

    private final DeliveryProofRepository proofRepository;
    private final OrderRepository orderRepository;
    private final CourierRepository courierRepository;

    /**
     * Submit delivery proof with photo.
     */
    @Transactional
    public DeliveryProof submitProof(Long orderId, Long courierId,
                                      String photoUrl,
                                      BigDecimal latitude, BigDecimal longitude,
                                      String recipientName, String notes) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        Courier courier = courierRepository.findById(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "id", courierId));

        // Validate courier is assigned to this order
        if (order.getCourier() == null || !order.getCourier().getId().equals(courierId)) {
            throw new BusinessException("Courier is not assigned to this order");
        }

        // Check if proof already exists
        if (proofRepository.existsByOrderId(orderId)) {
            throw new BusinessException("Delivery proof already submitted for this order");
        }

        DeliveryProof proof = DeliveryProof.builder()
                .order(order)
                .courier(courier)
                .photoUrl(photoUrl)
                .photoHash(generatePhotoHash(photoUrl))
                .deliveryLatitude(latitude)
                .deliveryLongitude(longitude)
                .recipientName(recipientName)
                .deliveryNotes(notes)
                .build();

        proof = proofRepository.save(proof);
        log.info("Delivery proof submitted for order {}: photo={}, location={},{}",
                orderId, photoUrl != null, latitude, longitude);

        return proof;
    }

    /**
     * Get delivery proof for an order.
     */
    @Transactional(readOnly = true)
    public Optional<DeliveryProof> getProofByOrderId(Long orderId) {
        return proofRepository.findByOrderId(orderId);
    }

    /**
     * Check if delivery proof exists for order.
     */
    @Transactional(readOnly = true)
    public boolean hasProof(Long orderId) {
        return proofRepository.existsByOrderId(orderId);
    }

    /**
     * Verify a delivery proof (admin action).
     */
    @Transactional
    public DeliveryProof verifyProof(Long proofId) {
        DeliveryProof proof = proofRepository.findById(proofId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryProof", "id", proofId));

        proof.markVerified();
        return proofRepository.save(proof);
    }

    /**
     * Get unverified proofs.
     */
    @Transactional(readOnly = true)
    public List<DeliveryProof> getUnverifiedProofs() {
        return proofRepository.findByVerifiedFalse();
    }

    /**
     * Get proofs by courier.
     */
    @Transactional(readOnly = true)
    public List<DeliveryProof> getProofsByCourier(Long courierId) {
        return proofRepository.findByCourierIdOrderByCreatedAtDesc(courierId);
    }

    /**
     * Check if proof is required for an order (high-value orders).
     */
    public boolean isProofRequired(Order order) {
        // Require proof for orders over $50
        return order.getTotal().compareTo(new BigDecimal("50.00")) > 0;
    }

    /**
     * Generate hash of photo content for verification.
     */
    private String generatePhotoHash(String photoUrl) {
        if (photoUrl == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(photoUrl.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.warn("Failed to generate photo hash: {}", e.getMessage());
            return null;
        }
    }
}
