package com.fooddelivery.order.controller;

import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.order.dto.*;
import com.fooddelivery.order.service.OrderService;
import com.fooddelivery.order.service.PaymentService;
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

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Get order details by ID")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id) {

        validateAccess(id, currentUser);
        OrderDto order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/number/{orderNo}")
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

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'COURIER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Update order status", description = "Update order status")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        validateAccess(id, currentUser);
        OrderDto order = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", order));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel order", description = "Cancel an order")
    public ResponseEntity<ApiResponse<OrderDto>> cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CancelOrderRequest request) {

        validateAccess(id, currentUser);
        OrderDto order = orderService.cancelOrder(id, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", order));
    }

    // Payment endpoints
    @PostMapping("/{orderId}/payment")
    @Operation(summary = "Create payment", description = "Create a payment intent for an order")
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
    @Operation(summary = "Get payment", description = "Get payment details for an order")
    public ResponseEntity<ApiResponse<PaymentDto>> getPayment(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long orderId) {

        validateAccess(orderId, currentUser);
        PaymentDto payment = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }
}
