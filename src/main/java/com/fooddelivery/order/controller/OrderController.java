package com.fooddelivery.order.controller;

import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.order.dto.*;
import com.fooddelivery.order.service.OrderService;
import com.fooddelivery.order.service.PaymentService;
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
    public ResponseEntity<ApiResponse<OrderDto>> getOrderById(@PathVariable Long id) {
        OrderDto order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/number/{orderNo}")
    @Operation(summary = "Get order by number", description = "Get order details by external order number")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderByNumber(@PathVariable String orderNo) {
        OrderDto order = orderService.getOrderByExternalNo(orderNo);
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
            @PathVariable Long restaurantId,
            @PageableDefault(size = 20) Pageable pageable) {

        PagedResponse<OrderDto> orders = orderService.getOrdersByRestaurant(restaurantId, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/restaurant/{restaurantId}/active")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Get active restaurant orders", description = "Get active orders for a restaurant")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getActiveRestaurantOrders(
            @PathVariable Long restaurantId) {

        List<OrderDto> orders = orderService.getActiveOrdersForRestaurant(restaurantId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'RESTAURANT_STAFF', 'COURIER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Update order status", description = "Update order status")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        OrderDto order = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", order));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel order", description = "Cancel an order")
    public ResponseEntity<ApiResponse<OrderDto>> cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CancelOrderRequest request) {

        OrderDto order = orderService.cancelOrder(id, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", order));
    }

    // Payment endpoints
    @PostMapping("/{orderId}/payment")
    @Operation(summary = "Create payment", description = "Create a payment intent for an order")
    public ResponseEntity<ApiResponse<PaymentDto>> createPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody CreatePaymentRequest request) {

        PaymentDto payment = paymentService.createPaymentIntent(orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment intent created", payment));
    }

    @GetMapping("/{orderId}/payment")
    @Operation(summary = "Get payment", description = "Get payment details for an order")
    public ResponseEntity<ApiResponse<PaymentDto>> getPayment(@PathVariable Long orderId) {
        PaymentDto payment = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }
}
