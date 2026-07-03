package com.fooddelivery.courier.controller;

import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.courier.dto.CourierDto;
import com.fooddelivery.courier.dto.CourierEarningsDto;
import com.fooddelivery.courier.dto.CourierOrderDto;
import com.fooddelivery.courier.dto.CourierStatisticsDto;
import com.fooddelivery.courier.dto.CreateCourierRequest;
import com.fooddelivery.courier.dto.UpdateCourierRequest;
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
    private final com.fooddelivery.order.service.ReviewService reviewService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('CONSUMER')")
    @Operation(summary = "Register as courier", description = "Register current user as a courier. Only consumers can register as couriers.")
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

    @PutMapping("/me")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Update my courier profile", description = "Update courier's own profile")
    public ResponseEntity<ApiResponse<CourierDto>> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdateCourierRequest request) {
        CourierDto courier = courierService.getCourierByUserId(currentUser.getId());
        courier = courierService.updateCourierProfile(courier.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", courier));
    }

    @PutMapping("/me/status")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Update status (PUT)", description = "Go online/offline")
    public ResponseEntity<ApiResponse<CourierDto>> updateStatusPut(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody java.util.Map<String, String> body) {
        CourierStatus status = CourierStatus.valueOf(body.get("status"));
        CourierDto courier = courierService.getCourierByUserId(currentUser.getId());
        courier = courierService.updateStatus(courier.getId(), status);
        return ResponseEntity.ok(ApiResponse.success("Status updated", courier));
    }

    @PatchMapping("/me/status")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Update status (PATCH)", description = "Go online/offline")
    public ResponseEntity<ApiResponse<CourierDto>> updateStatus(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam CourierStatus status) {

        CourierDto courier = courierService.getCourierByUserId(currentUser.getId());
        courier = courierService.updateStatus(courier.getId(), status);
        return ResponseEntity.ok(ApiResponse.success("Status updated", courier));
    }

    @PutMapping("/me/location")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Update location (PUT)", description = "Update current location")
    public ResponseEntity<ApiResponse<Void>> updateLocationPut(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody java.util.Map<String, BigDecimal> body) {
        CourierDto courier = courierService.getCourierByUserId(currentUser.getId());
        courierService.updateLocation(courier.getId(), body.get("latitude"), body.get("longitude"));
        return ResponseEntity.ok(ApiResponse.success("Location updated"));
    }

    @PostMapping("/me/location")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Update location (POST)", description = "Update current location")
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng) {

        CourierDto courier = courierService.getCourierByUserId(currentUser.getId());
        courierService.updateLocation(courier.getId(), lat, lng);
        return ResponseEntity.ok(ApiResponse.success("Location updated"));
    }

    @GetMapping("/me/available-orders")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Get available orders", description = "Get orders available for pickup")
    public ResponseEntity<ApiResponse<List<CourierOrderDto>>> getAvailableOrders(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        CourierDto courier = courierService.getCourierByUserId(currentUser.getId());
        List<CourierOrderDto> orders = courierService.getAvailableOrders(courier.getId());
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/me/orders/active")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Get active orders", description = "Get current orders in progress")
    public ResponseEntity<ApiResponse<List<CourierOrderDto>>> getActiveOrders(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        CourierDto courier = courierService.getCourierByUserId(currentUser.getId());
        log.info("Getting active orders: userId={}, courierId={}", currentUser.getId(), courier.getId());
        List<CourierOrderDto> orders = courierService.getActiveOrders(courier.getId());
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/me/orders/history")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Get order history", description = "Get completed and cancelled orders")
    public ResponseEntity<ApiResponse<PagedResponse<CourierOrderDto>>> getOrderHistory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        CourierDto courier = courierService.getCourierByUserId(currentUser.getId());
        PagedResponse<CourierOrderDto> orders = courierService.getOrderHistory(courier.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/me/earnings")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Get earnings", description = "Get earnings summary")
    public ResponseEntity<ApiResponse<CourierEarningsDto>> getEarnings(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        CourierDto courier = courierService.getCourierByUserId(currentUser.getId());
        CourierEarningsDto earnings = courierService.getEarnings(courier.getId());
        return ResponseEntity.ok(ApiResponse.success(earnings));
    }

    @GetMapping("/me/reviews")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Get my reviews", description = "Get customer reviews for this courier")
    public ResponseEntity<ApiResponse<PagedResponse<com.fooddelivery.order.dto.ReviewDto>>> getMyReviews(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        CourierDto courier = courierService.getCourierByUserId(currentUser.getId());
        PagedResponse<com.fooddelivery.order.dto.ReviewDto> reviews =
                reviewService.getCourierReviews(courier.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    // ===== Order Management (Courier App) =====

    @PostMapping("/me/orders/{orderId}/accept")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Accept order", description = "Accept an available order")
    public ResponseEntity<ApiResponse<CourierOrderDto>> acceptOrderMe(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long orderId) {
        CourierDto courier = courierService.getCourierByUserId(currentUser.getId());
        CourierOrderDto order = courierService.acceptOrderAndGetDetails(courier.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Order accepted", order));
    }

    @GetMapping("/me/orders/{orderId}")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Get order details", description = "Get details of a specific order")
    public ResponseEntity<ApiResponse<CourierOrderDto>> getOrderDetails(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long orderId) {
        CourierDto courier = courierService.getCourierByUserId(currentUser.getId());
        CourierOrderDto order = courierService.getOrderDetails(courier.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PutMapping("/me/orders/{orderId}/pickup")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Confirm pickup", description = "Confirm order pickup from restaurant")
    public ResponseEntity<ApiResponse<CourierOrderDto>> confirmPickup(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long orderId) {
        CourierDto courier = courierService.getCourierByUserId(currentUser.getId());
        CourierOrderDto order = courierService.confirmPickup(courier.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Order picked up", order));
    }

    @PutMapping("/me/orders/{orderId}/transit")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Start transit", description = "Mark order as in transit to customer")
    public ResponseEntity<ApiResponse<CourierOrderDto>> startTransit(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long orderId) {
        CourierDto courier = courierService.getCourierByUserId(currentUser.getId());
        CourierOrderDto order = courierService.startTransit(courier.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success("In transit to customer", order));
    }

    @PostMapping("/me/orders/{orderId}/complete")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Complete delivery", description = "Mark delivery as completed")
    public ResponseEntity<ApiResponse<CourierOrderDto>> completeDeliveryMe(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long orderId) {
        CourierDto courier = courierService.getCourierByUserId(currentUser.getId());
        CourierOrderDto order = courierService.completeDeliveryAndGetDetails(courier.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Delivery completed", order));
    }

    @PostMapping("/me/orders/{orderId}/issue")
    @PreAuthorize("hasRole('COURIER')")
    @Operation(summary = "Report issue", description = "Report an issue with the order")
    public ResponseEntity<ApiResponse<Void>> reportIssue(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long orderId,
            @RequestBody java.util.Map<String, String> body) {
        CourierDto courier = courierService.getCourierByUserId(currentUser.getId());
        courierService.reportOrderIssue(courier.getId(), orderId, body.get("issueType"), body.get("description"));
        return ResponseEntity.ok(ApiResponse.success("Issue reported"));
    }

    // NOTE: The legacy POST /{courierId}/accept/{orderId} and
    // /{courierId}/complete/{orderId} endpoints were removed — they took the
    // courier id from the URL (IDOR: any courier could act as any other).
    // Use the token-derived /me/orders/{orderId}/accept and /complete instead.

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

    // ===== Platform/Admin Courier Management =====

    @GetMapping("/{courierId}")
    @PreAuthorize("hasAnyRole('PLATFORM', 'ADMIN')")
    @Operation(summary = "Get courier by ID", description = "Get a specific courier by ID")
    public ResponseEntity<ApiResponse<CourierDto>> getCourierById(@PathVariable Long courierId) {
        CourierDto courier = courierService.getCourierById(courierId);
        return ResponseEntity.ok(ApiResponse.success(courier));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('PLATFORM', 'ADMIN')")
    @Operation(summary = "Get pending couriers", description = "Get all couriers awaiting approval")
    public ResponseEntity<ApiResponse<PagedResponse<CourierDto>>> getPendingCouriers(
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<CourierDto> couriers = courierService.getPendingCouriers(pageable);
        return ResponseEntity.ok(ApiResponse.success(couriers));
    }

    @GetMapping("/online")
    @PreAuthorize("hasAnyRole('PLATFORM', 'ADMIN')")
    @Operation(summary = "Get online couriers", description = "Get all online couriers with location data (for map display)")
    public ResponseEntity<ApiResponse<List<CourierDto>>> getOnlineCouriers() {
        List<CourierDto> couriers = courierService.getOnlineCouriers();
        return ResponseEntity.ok(ApiResponse.success(couriers));
    }

    @GetMapping("/by-status/{status}")
    @PreAuthorize("hasAnyRole('PLATFORM', 'ADMIN')")
    @Operation(summary = "Get couriers by status", description = "Get all couriers with a specific status")
    public ResponseEntity<ApiResponse<PagedResponse<CourierDto>>> getCouriersByStatus(
            @PathVariable CourierStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<CourierDto> couriers = courierService.getCouriersByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(couriers));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('PLATFORM', 'ADMIN')")
    @Operation(summary = "Get courier statistics", description = "Get courier statistics for dashboard")
    public ResponseEntity<ApiResponse<CourierStatisticsDto>> getCourierStatistics() {
        CourierStatisticsDto stats = courierService.getCourierStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PostMapping("/{courierId}/reject")
    @PreAuthorize("hasAnyRole('PLATFORM', 'ADMIN')")
    @Operation(summary = "Reject courier", description = "Reject a pending courier application")
    public ResponseEntity<ApiResponse<CourierDto>> rejectCourier(
            @PathVariable Long courierId,
            @RequestParam(required = false) String reason) {
        CourierDto courier = courierService.rejectCourier(courierId, reason);
        return ResponseEntity.ok(ApiResponse.success("Courier rejected", courier));
    }

    @PostMapping("/{courierId}/suspend")
    @PreAuthorize("hasAnyRole('PLATFORM', 'ADMIN')")
    @Operation(summary = "Suspend courier", description = "Suspend a courier account")
    public ResponseEntity<ApiResponse<CourierDto>> suspendCourier(
            @PathVariable Long courierId,
            @RequestParam(required = false) String reason) {
        CourierDto courier = courierService.suspendCourier(courierId, reason);
        return ResponseEntity.ok(ApiResponse.success("Courier suspended", courier));
    }

    @PostMapping("/{courierId}/activate")
    @PreAuthorize("hasAnyRole('PLATFORM', 'ADMIN')")
    @Operation(summary = "Activate courier", description = "Activate/reactivate a suspended or pending courier")
    public ResponseEntity<ApiResponse<CourierDto>> activateCourier(@PathVariable Long courierId) {
        CourierDto courier = courierService.activateCourier(courierId);
        return ResponseEntity.ok(ApiResponse.success("Courier activated", courier));
    }

    @PutMapping("/{courierId}")
    @PreAuthorize("hasAnyRole('PLATFORM', 'ADMIN')")
    @Operation(summary = "Update courier profile", description = "Update a courier's profile (admin)")
    public ResponseEntity<ApiResponse<CourierDto>> updateCourierProfile(
            @PathVariable Long courierId,
            @Valid @RequestBody UpdateCourierRequest request) {
        CourierDto courier = courierService.updateCourierProfile(courierId, request);
        return ResponseEntity.ok(ApiResponse.success("Courier profile updated", courier));
    }
}
