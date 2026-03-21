package com.fooddelivery.selfservice.controller;

import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.selfservice.entity.SelfServiceResolution;
import com.fooddelivery.selfservice.service.SelfServiceResolutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for customer self-service resolution endpoints.
 */
@RestController
@RequestMapping("/api/v1/customer/self-service")
@RequiredArgsConstructor
@Tag(name = "Self-Service", description = "Customer self-service resolution endpoints")
@PreAuthorize("hasRole('CONSUMER')")
public class SelfServiceController {

    private final SelfServiceResolutionService resolutionService;

    @GetMapping("/orders/{orderId}/eligibility")
    @Operation(summary = "Check self-service eligibility for an order")
    public ResponseEntity<Map<String, Boolean>> checkEligibility(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(resolutionService.checkEligibility(user.getId(), orderId));
    }

    @PostMapping("/orders/{orderId}/auto-refund")
    @Operation(summary = "Request auto-refund for small orders")
    public ResponseEntity<SelfServiceResolutionDto> requestAutoRefund(
            @PathVariable Long orderId,
            @RequestBody AutoRefundRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        SelfServiceResolution resolution = resolutionService.requestAutoRefund(
                user.getId(), orderId, request.getReason());
        return ResponseEntity.ok(toDto(resolution));
    }

    @PostMapping("/orders/{orderId}/missing-items")
    @Operation(summary = "Report missing items and get refund")
    public ResponseEntity<SelfServiceResolutionDto> reportMissingItems(
            @PathVariable Long orderId,
            @RequestBody MissingItemsRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        SelfServiceResolution resolution = resolutionService.reportMissingItems(
                user.getId(), orderId, request.getMissingItemIds());
        return ResponseEntity.ok(toDto(resolution));
    }

    @PostMapping("/orders/{orderId}/cancel")
    @Operation(summary = "Cancel order before preparation")
    public ResponseEntity<SelfServiceResolutionDto> cancelOrder(
            @PathVariable Long orderId,
            @RequestBody CancelRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        SelfServiceResolution resolution = resolutionService.cancelPrePrep(
                user.getId(), orderId, request.getReason());
        return ResponseEntity.ok(toDto(resolution));
    }

    @GetMapping("/history")
    @Operation(summary = "Get self-service resolution history")
    public ResponseEntity<List<SelfServiceResolutionDto>> getHistory(
            @AuthenticationPrincipal UserPrincipal user) {
        List<SelfServiceResolution> resolutions = resolutionService.getResolutionHistory(user.getId());
        return ResponseEntity.ok(resolutions.stream().map(this::toDto).toList());
    }

    private SelfServiceResolutionDto toDto(SelfServiceResolution resolution) {
        SelfServiceResolutionDto dto = new SelfServiceResolutionDto();
        dto.setId(resolution.getId());
        dto.setOrderId(resolution.getOrder().getId());
        dto.setResolutionType(resolution.getResolutionType().name());
        dto.setAmount(resolution.getAmount() != null ? resolution.getAmount().toString() : null);
        dto.setStatus(resolution.getStatus());
        dto.setCreatedAt(resolution.getCreatedAt().toString());
        return dto;
    }

    @Data
    public static class AutoRefundRequest {
        @NotBlank
        private String reason;
    }

    @Data
    public static class MissingItemsRequest {
        @NotEmpty
        private List<Long> missingItemIds;
    }

    @Data
    public static class CancelRequest {
        @NotBlank
        private String reason;
    }

    @Data
    public static class SelfServiceResolutionDto {
        private Long id;
        private Long orderId;
        private String resolutionType;
        private String amount;
        private String status;
        private String createdAt;
    }
}
