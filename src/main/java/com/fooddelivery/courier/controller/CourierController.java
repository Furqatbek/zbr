package com.fooddelivery.courier.controller;

import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.courier.dto.CourierDto;
import com.fooddelivery.courier.dto.CreateCourierRequest;
import com.fooddelivery.courier.entity.CourierStatus;
import com.fooddelivery.courier.service.CourierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/couriers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Couriers", description = "Courier management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class CourierController {

    private final CourierService courierService;

    @PostMapping("/register")
    @Operation(summary = "Register as courier", description = "Register current user as a courier")
    public ResponseEntity<ApiResponse<CourierDto>> register(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateCourierRequest request) {

        CourierDto courier = courierService.registerCourier(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Courier registration submitted", courier));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Get my courier profile")
    public ResponseEntity<ApiResponse<CourierDto>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        CourierDto courier = courierService.getCourierByUserId(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(courier));
    }

    @PatchMapping("/me/status")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Update status", description = "Go online/offline")
    public ResponseEntity<ApiResponse<CourierDto>> updateStatus(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam CourierStatus status) {

        CourierDto courier = courierService.getCourierByUserId(currentUser.getId());
        courier = courierService.updateStatus(courier.getId(), status);
        return ResponseEntity.ok(ApiResponse.success("Status updated", courier));
    }

    @PostMapping("/me/location")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Update location", description = "Update current location")
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng) {

        CourierDto courier = courierService.getCourierByUserId(currentUser.getId());
        courierService.updateLocation(courier.getId(), lat, lng);
        return ResponseEntity.ok(ApiResponse.success("Location updated"));
    }

    @PostMapping("/{courierId}/accept/{orderId}")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Accept order", description = "Accept an order assignment")
    public ResponseEntity<ApiResponse<CourierDto>> acceptOrder(
            @PathVariable Long courierId,
            @PathVariable Long orderId) {

        CourierDto courier = courierService.acceptOrder(courierId, orderId);
        return ResponseEntity.ok(ApiResponse.success("Order accepted", courier));
    }

    @PostMapping("/{courierId}/complete/{orderId}")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Complete delivery", description = "Mark delivery as completed")
    public ResponseEntity<ApiResponse<CourierDto>> completeDelivery(
            @PathVariable Long courierId,
            @PathVariable Long orderId) {

        CourierDto courier = courierService.completeDelivery(courierId, orderId);
        return ResponseEntity.ok(ApiResponse.success("Delivery completed", courier));
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('PLATFORM', 'ADMIN', 'RESTAURANT_OWNER')")
    @Operation(summary = "Find available couriers", description = "Find available couriers near a location")
    public ResponseEntity<ApiResponse<List<CourierDto>>> findAvailable(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng,
            @RequestParam(defaultValue = "5") double radius) {

        List<CourierDto> couriers = courierService.findAvailableCouriers(lat, lng, radius);
        return ResponseEntity.ok(ApiResponse.success(couriers));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM', 'ADMIN')")
    @Operation(summary = "Get all couriers")
    public ResponseEntity<ApiResponse<PagedResponse<CourierDto>>> getAllCouriers(
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<CourierDto> couriers = courierService.getAllCouriers(pageable);
        return ResponseEntity.ok(ApiResponse.success(couriers));
    }

    @PostMapping("/{courierId}/verify")
    @PreAuthorize("hasAnyRole('PLATFORM', 'ADMIN')")
    @Operation(summary = "Verify courier", description = "Verify a courier (admin)")
    public ResponseEntity<ApiResponse<CourierDto>> verifyCourier(@PathVariable Long courierId) {
        CourierDto courier = courierService.verifyCourier(courierId);
        return ResponseEntity.ok(ApiResponse.success("Courier verified", courier));
    }
}
