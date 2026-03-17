package com.fooddelivery.order.controller;

import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.order.dto.*;
import com.fooddelivery.order.service.OrderService;
import com.fooddelivery.order.service.PaymentService;
import com.fooddelivery.order.service.PromoService;
import com.fooddelivery.order.service.ReviewService;
import com.fooddelivery.restaurant.service.RestaurantService;
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

import java.util.List;

/**
 * REST controller for order operations.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Order management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final PromoService promoService;
    private final ReviewService reviewService;
    private final RestaurantService restaurantService;

    /**
     * Helper method to validate restaurant access for order endpoints.
     */
    private void validateRestaurantAccess(Long restaurantId, UserPrincipal currentUser) {
        boolean isAdminOrPlatform = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_PLATFORM"));
        restaurantService.validateRestaurantAccess(restaurantId, currentUser.getId(), isAdminOrPlatform);
    }

    /**
     * Helper method to validate order access based on user roles.
     */
    private void validateAccess(Long orderId, UserPrincipal currentUser) {
        boolean isAdminOrPlatform = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_PLATFORM"));
        boolean isRestaurantRole = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESTAURANT_OWNER") || a.getAuthority().equals("ROLE_RESTAURANT_STAFF"));
        boolean isCourier = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COURIER"));

        orderService.validateOrderAccess(orderId, currentUser.getId(), isAdminOrPlatform, isRestaurantRole, isCourier);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM', 'ADMIN')")
    @Operation(summary = "Get all orders", description = "Get all orders (admin/platform only)")
    public ResponseEntity<ApiResponse<PagedResponse<OrderDto>>> getAllOrders(
            @PageableDefault(size = 20) Pageable pageable) {

        PagedResponse<OrderDto> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CONSUMER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Create order", description = "Create a new order")
    public ResponseEntity<ApiResponse<OrderDto>> createOrder(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateOrderRequest request) {

        OrderDto order = orderService.createOrder(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", order));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('CONSUMER', 'RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'COURIER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Get order by ID", description = "Get order details by ID")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long orderId) {

        validateAccess(orderId, currentUser);
        OrderDto order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/number/{orderNo}")
    @PreAuthorize("hasAnyRole('CONSUMER', 'RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'COURIER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Get order by number", description = "Get order details by external order number")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderByNumber(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable String orderNo) {

        OrderDto order = orderService.getOrderByExternalNo(orderNo);
        // Validate access using order ID
        validateAccess(order.getId(), currentUser);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CONSUMER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Get my orders", description = "Get orders for the current user")
    public ResponseEntity<ApiResponse<PagedResponse<OrderDto>>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PageableDefault(size = 20) Pageable pageable) {

        PagedResponse<OrderDto> orders = orderService.getOrdersByConsumer(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/restaurant/{restaurantId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Get restaurant orders", description = "Get orders for a restaurant")
    public ResponseEntity<ApiResponse<PagedResponse<OrderDto>>> getRestaurantOrders(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId,
            @PageableDefault(size = 20) Pageable pageable) {

        validateRestaurantAccess(restaurantId, currentUser);
        PagedResponse<OrderDto> orders = orderService.getOrdersByRestaurant(restaurantId, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/restaurant/{restaurantId}/active")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Get active restaurant orders", description = "Get active orders for a restaurant")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getActiveRestaurantOrders(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long restaurantId) {

        validateRestaurantAccess(restaurantId, currentUser);
        List<OrderDto> orders = orderService.getActiveOrdersForRestaurant(restaurantId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'COURIER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Update order status", description = "Update order status")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        validateAccess(orderId, currentUser);
        OrderDto order = orderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", order));
    }

    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('CONSUMER', 'RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Cancel order", description = "Cancel an order")
    public ResponseEntity<ApiResponse<OrderDto>> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CancelOrderRequest request) {

        validateAccess(orderId, currentUser);
        OrderDto order = orderService.cancelOrder(orderId, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", order));
    }

    // Payment endpoints
    @PostMapping("/{orderId}/pay")
    @PreAuthorize("hasAnyRole('CONSUMER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Initiate payment", description = "Initiate a payment for an order")
    public ResponseEntity<ApiResponse<PaymentDto>> createPayment(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long orderId,
            @Valid @RequestBody CreatePaymentRequest request) {

        validateAccess(orderId, currentUser);
        PaymentDto payment = paymentService.createPaymentIntent(orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment intent created", payment));
    }

    @GetMapping("/{orderId}/payment")
    @PreAuthorize("hasAnyRole('CONSUMER', 'RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Get payment", description = "Get payment details for an order")
    public ResponseEntity<ApiResponse<PaymentDto>> getPayment(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long orderId) {

        validateAccess(orderId, currentUser);
        PaymentDto payment = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    // Tracking endpoint
    @GetMapping("/{orderId}/track")
    @PreAuthorize("hasAnyRole('CONSUMER', 'RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'COURIER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Track order", description = "Get live order tracking information")
    public ResponseEntity<ApiResponse<OrderTrackingDto>> trackOrder(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long orderId) {

        validateAccess(orderId, currentUser);
        OrderTrackingDto tracking = orderService.getOrderTracking(orderId);
        return ResponseEntity.ok(ApiResponse.success(tracking));
    }

    // Reorder endpoint
    @PostMapping("/{orderId}/reorder")
    @PreAuthorize("hasAnyRole('CONSUMER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Reorder", description = "Create a new order based on a previous order")
    public ResponseEntity<ApiResponse<OrderDto>> reorder(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long orderId) {

        OrderDto order = orderService.reorder(orderId, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created from previous order", order));
    }

    // Review endpoint
    @PostMapping("/{orderId}/review")
    @PreAuthorize("hasAnyRole('CONSUMER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Submit review", description = "Submit a review for an order")
    public ResponseEntity<ApiResponse<ReviewDto>> submitReview(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long orderId,
            @Valid @RequestBody CreateReviewRequest request) {

        ReviewDto review = reviewService.submitReview(orderId, currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted successfully", review));
    }

    // Promo code validation endpoint
    @PostMapping("/validate-promo")
    @PreAuthorize("hasAnyRole('CONSUMER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Validate promo code", description = "Validate a promo code and calculate discount")
    public ResponseEntity<ApiResponse<PromoValidationResponse>> validatePromoCode(
            @Valid @RequestBody PromoValidationRequest request) {

        PromoValidationResponse response = promoService.validatePromoCode(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Payment confirmation endpoint
    @PostMapping("/{orderId}/pay/confirm")
    @PreAuthorize("hasAnyRole('CONSUMER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Confirm payment", description = "Confirm payment for an order")
    public ResponseEntity<ApiResponse<PaymentDto>> confirmPayment(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long orderId,
            @Valid @RequestBody ConfirmPaymentRequest request) {

        validateAccess(orderId, currentUser);
        PaymentDto payment = paymentService.confirmPayment(
                request.getPaymentIntentId(),
                request.getPaymentMethodId(),
                null
        );
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed", payment));
    }
}
