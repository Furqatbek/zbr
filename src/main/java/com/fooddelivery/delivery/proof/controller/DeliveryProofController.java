package com.fooddelivery.delivery.proof.controller;

import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.courier.repository.CourierRepository;
import com.fooddelivery.delivery.proof.entity.DeliveryProof;
import com.fooddelivery.delivery.proof.service.DeliveryProofService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Controller for delivery proof management.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Delivery Proof", description = "Proof of delivery endpoints")
public class DeliveryProofController {

    private final DeliveryProofService proofService;
    private final CourierRepository courierRepository;

    // === Courier Endpoints ===

    @PostMapping("/courier/deliveries/{orderId}/proof")
    @Operation(summary = "Submit delivery proof")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<DeliveryProofDto> submitProof(
            @PathVariable Long orderId,
            @Valid @RequestBody SubmitProofRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        Courier courier = courierRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Courier not found"));

        DeliveryProof proof = proofService.submitProof(
                orderId,
                courier.getId(),
                request.getPhotoUrl(),
                request.getLatitude(),
                request.getLongitude(),
                request.getRecipientName(),
                request.getNotes()
        );

        return ResponseEntity.ok(toDto(proof));
    }

    @GetMapping("/courier/deliveries/{orderId}/proof")
    @Operation(summary = "Get delivery proof for an order (courier)")
    @PreAuthorize("hasRole('COURIER')")
    public ResponseEntity<DeliveryProofDto> getProof(@PathVariable Long orderId) {
        return proofService.getProofByOrderId(orderId)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // === Customer Endpoints ===

    @GetMapping("/customer/orders/{orderId}/proof")
    @Operation(summary = "Get delivery proof for an order (customer)")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<DeliveryProofDto> getProofForCustomer(@PathVariable Long orderId) {
        return proofService.getProofByOrderId(orderId)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // === Admin Endpoints ===

    @PostMapping("/admin/delivery-proofs/{proofId}/verify")
    @Operation(summary = "Verify a delivery proof")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryProofDto> verifyProof(@PathVariable Long proofId) {
        DeliveryProof proof = proofService.verifyProof(proofId);
        return ResponseEntity.ok(toDto(proof));
    }

    @GetMapping("/admin/delivery-proofs/unverified")
    @Operation(summary = "Get unverified delivery proofs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeliveryProofDto>> getUnverifiedProofs() {
        List<DeliveryProof> proofs = proofService.getUnverifiedProofs();
        return ResponseEntity.ok(proofs.stream().map(this::toDto).toList());
    }

    private DeliveryProofDto toDto(DeliveryProof proof) {
        DeliveryProofDto dto = new DeliveryProofDto();
        dto.setId(proof.getId());
        dto.setOrderId(proof.getOrder().getId());
        dto.setCourierId(proof.getCourier().getId());
        dto.setPhotoUrl(proof.getPhotoUrl());
        dto.setLatitude(proof.getDeliveryLatitude());
        dto.setLongitude(proof.getDeliveryLongitude());
        dto.setRecipientName(proof.getRecipientName());
        dto.setNotes(proof.getDeliveryNotes());
        dto.setVerified(proof.getVerified());
        dto.setCreatedAt(proof.getCreatedAt().toString());
        return dto;
    }

    @Data
    public static class SubmitProofRequest {
        private String photoUrl;
        @NotNull
        private BigDecimal latitude;
        @NotNull
        private BigDecimal longitude;
        private String recipientName;
        private String notes;
    }

    @Data
    public static class DeliveryProofDto {
        private Long id;
        private Long orderId;
        private Long courierId;
        private String photoUrl;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String recipientName;
        private String notes;
        private Boolean verified;
        private String createdAt;
    }
}
