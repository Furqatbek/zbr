package com.fooddelivery.order.service;

import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.service.UserService;
import com.fooddelivery.common.annotation.Auditable;
import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.common.event.EventPublisher;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.InvalidOperationException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.common.util.JsonUtils;
import com.fooddelivery.common.util.SlugUtils;
import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.courier.repository.CourierRepository;
import com.fooddelivery.order.dto.*;
import com.fooddelivery.order.entity.*;
import com.fooddelivery.order.event.OrderCreatedEvent;
import com.fooddelivery.order.event.OrderStatusChangedEvent;
import com.fooddelivery.order.mapper.OrderMapper;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.restaurant.entity.ItemOption;
import com.fooddelivery.restaurant.entity.ItemVariant;
import com.fooddelivery.restaurant.entity.MenuItem;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.repository.MenuItemRepository;
import com.fooddelivery.restaurant.service.RestaurantService;
import com.fooddelivery.analytics.financial.service.CommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Service for order operations with state machine logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantService restaurantService;
    private final UserService userService;
    private final OrderMapper orderMapper;
    private final EventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final CourierRepository courierRepository;
    private final DeliveryFeeCalculationService deliveryFeeCalculationService;
    private final CommissionService commissionService;
    private final PaymentService paymentService;

    @Value("${app.order.auto-cancel-unpaid-minutes:30}")
    private int autoCancelMinutes;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08"); // 8% tax

    /**
     * Create a new order.
     */
    @Transactional
    @Auditable(action = "CREATE_ORDER", entityType = "Order")
    public OrderDto createOrder(Long consumerId, CreateOrderRequest request, String idempotencyKey) {
        log.info("Creating order for consumer: {} at restaurant: {}", consumerId, request.getRestaurantId());

        // Idempotency: if this key already produced an order, return it instead of
        // creating a duplicate. The partial unique index (V33) is the concurrency
        // backstop for two requests that pass this check simultaneously.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Order> existing = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent replay for key {} -> existing order {}", idempotencyKey, existing.get().getId());
                return orderMapper.toDto(existing.get());
            }
        }

        // Validate restaurant
        Restaurant restaurant = restaurantService.getRestaurantEntityById(request.getRestaurantId());
        if (!restaurant.isCurrentlyOpen()) {
            throw new BusinessException("Restaurant is currently closed");
        }

        // Validate order type specific requirements
        validateOrderType(request);

        // Get consumer
        User consumer = userService.getUserEntityById(consumerId);

        // Calculate delivery fee dynamically for delivery orders
        BigDecimal deliveryFee = BigDecimal.ZERO;
        if (request.getOrderType() == OrderType.DELIVERY) {
            // Validate delivery is within restaurant's radius
            if (!deliveryFeeCalculationService.isWithinDeliveryRadius(
                    restaurant, request.getDeliveryLatitude(), request.getDeliveryLongitude())) {
                throw new BusinessException("Delivery address is outside restaurant's delivery radius");
            }
            // Calculate delivery fee based on distance
            deliveryFee = deliveryFeeCalculationService.calculateDeliveryFee(
                    restaurant, request.getDeliveryLatitude(), request.getDeliveryLongitude());
        }

        // Create order
        Order order = Order.builder()
                .externalOrderNo(SlugUtils.generateOrderNumber())
                .idempotencyKey(idempotencyKey)
                .consumer(consumer)
                .restaurant(restaurant)
                .orderType(request.getOrderType())
                .status(OrderStatus.CREATED)
                .paymentStatus(PaymentStatus.PENDING)
                .deliveryAddress(request.getDeliveryAddress())
                .deliveryLatitude(request.getDeliveryLatitude())
                .deliveryLongitude(request.getDeliveryLongitude())
                .deliveryInstructions(request.getDeliveryInstructions())
                .tableId(request.getTableId())
                .customerName(request.getCustomerName() != null ? request.getCustomerName() : consumer.getFullName())
                .customerPhone(request.getCustomerPhone() != null ? request.getCustomerPhone() : consumer.getPhone())
                .notes(request.getNotes())
                .tipAmount(request.getTipAmount() != null ? request.getTipAmount() : BigDecimal.ZERO)
                .deliveryFee(deliveryFee)
                .build();

        // Process order items
        List<OrderItem> orderItems = processOrderItems(order, request.getItems());
        order.setItems(orderItems);

        // Calculate totals
        order.calculateTotals();
        order.setTax(order.getSubtotal().multiply(TAX_RATE).setScale(2, java.math.RoundingMode.HALF_UP));
        order.calculateTotals(); // Recalculate with tax

        // Validate minimum order
        if (order.getSubtotal().compareTo(restaurant.getMinimumOrder()) < 0) {
            throw new BusinessException("Order subtotal must be at least " + restaurant.getMinimumOrder());
        }

        // Save order
        order = orderRepository.save(order);
        log.info("Order created: {} (ID: {})", order.getExternalOrderNo(), order.getId());

        // Publish event
        publishOrderCreatedEvent(order);

        // Notify restaurant via WebSocket
        notifyRestaurant(order);

        // Notify couriers via WebSocket about new delivery order
        if (order.getOrderType() == OrderType.DELIVERY) {
            notifyAvailableCouriers(order);
        }

        return orderMapper.toDto(order);
    }

    /**
     * Get order by ID.
     */
    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        return orderMapper.toDto(order);
    }

    /**
     * Get the order previously created under the given idempotency key. Used to
     * resolve the winner when a concurrent duplicate create loses the unique-index
     * race. Throws if no such order exists.
     */
    @Transactional(readOnly = true)
    public OrderDto getOrderByIdempotencyKey(String idempotencyKey) {
        Order order = orderRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "idempotencyKey", idempotencyKey));
        return orderMapper.toDto(order);
    }

    /**
     * Get all orders (admin/platform only).
     */
    @Transactional(readOnly = true)
    public PagedResponse<OrderDto> getAllOrders(Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(pageable);
        return PagedResponse.from(orders, orderMapper.toDtoList(orders.getContent()));
    }

    /**
     * Get problematic orders (cancelled, refunded, or with open disputes) for admin review.
     */
    @Transactional(readOnly = true)
    public PagedResponse<OrderDto> getProblematicOrders(Pageable pageable) {
        List<OrderStatus> problematicStatuses = List.of(OrderStatus.CANCELLED, OrderStatus.REFUNDED);
        Page<Order> orders = orderRepository.findProblematicOrders(problematicStatuses, pageable);
        return PagedResponse.from(orders, orderMapper.toDtoList(orders.getContent()));
    }

    /**
     * Get order by external order number.
     */
    @Transactional(readOnly = true)
    public OrderDto getOrderByExternalNo(String externalOrderNo) {
        Order order = orderRepository.findByExternalOrderNo(externalOrderNo)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "externalOrderNo", externalOrderNo));
        return orderMapper.toDto(order);
    }

    /**
     * Get orders by consumer.
     */
    @Transactional(readOnly = true)
    public PagedResponse<OrderDto> getOrdersByConsumer(Long consumerId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByConsumerId(consumerId, pageable);
        return PagedResponse.from(orders, orderMapper.toDtoList(orders.getContent()));
    }

    /**
     * Get orders by restaurant.
     */
    @Transactional(readOnly = true)
    public PagedResponse<OrderDto> getOrdersByRestaurant(Long restaurantId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByRestaurantId(restaurantId, pageable);
        return PagedResponse.from(orders, orderMapper.toDtoList(orders.getContent()));
    }

    /**
     * Get active orders for a restaurant.
     */
    @Transactional(readOnly = true)
    public List<OrderDto> getActiveOrdersForRestaurant(Long restaurantId) {
        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.CREATED, OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY,
                OrderStatus.COURIER_ASSIGNED, OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT, OrderStatus.DELIVERED
        );
        List<Order> orders = orderRepository.findActiveOrdersByRestaurant(restaurantId, activeStatuses);
        return orderMapper.toDtoList(orders);
    }

    /**
     * Update order status with state machine validation.
     */
    @Transactional
    @Auditable(action = "UPDATE_ORDER_STATUS", entityType = "Order")
    public OrderDto updateOrderStatus(Long orderId, UpdateOrderStatusRequest request,
                                      boolean isAdminOrPlatform, boolean isRestaurantRole,
                                      boolean isCourier) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        OrderStatus previousStatus = order.getStatus();
        OrderStatus newStatus = request.getStatus();

        validateActorMaySet(orderId, newStatus, isAdminOrPlatform, isRestaurantRole, isCourier);

        // Idempotent replay: already in the requested status -> return the current
        // order (200) with no side effects, so a client retry across a deploy drain
        // or network blip is safe instead of a 422 "cannot transition".
        if (previousStatus == newStatus) {
            log.debug("Order {} already in status {} — idempotent no-op",
                    order.getExternalOrderNo(), newStatus);
            return orderMapper.toDto(order);
        }

        // Validate transition
        if (!order.canTransitionTo(newStatus)) {
            throw new InvalidOperationException(
                    "updateStatus",
                    previousStatus.name(),
                    "Cannot transition from " + previousStatus + " to " + newStatus
            );
        }

        // Perform status update
        order.updateStatus(newStatus);

        // Handle status-specific logic
        handleStatusChange(order, previousStatus, newStatus, request);

        order = orderRepository.save(order);
        log.info("Order {} status changed: {} -> {}", order.getExternalOrderNo(), previousStatus, newStatus);

        // Publish event
        publishOrderStatusChangedEvent(order, previousStatus, request.getNotes());

        // Notify via WebSocket
        notifyOrderStatusChange(order);

        return orderMapper.toDto(order);
    }

    /**
     * Cancel an order.
     */
    @Transactional
    @Auditable(action = "CANCEL_ORDER", entityType = "Order")
    public OrderDto cancelOrder(Long orderId, CancelOrderRequest request, Long cancelledBy) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Idempotent replay: already cancelled -> return the current order (200),
        // so a retried cancel does not 422. (A refund only moves paymentStatus, not
        // the order status, so a cancelled order stays CANCELLED here.)
        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.debug("Order {} already cancelled — idempotent no-op", order.getExternalOrderNo());
            return orderMapper.toDto(order);
        }

        if (!order.isCancellable()) {
            throw new InvalidOperationException(
                    "cancel",
                    order.getStatus().name(),
                    "Order cannot be cancelled in current status"
            );
        }

        OrderStatus previousStatus = order.getStatus();
        order.updateStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(request.getReason());

        order = orderRepository.save(order);
        log.info("Order {} cancelled by user {}: {}", order.getExternalOrderNo(), cancelledBy, request.getReason());

        // If the customer already paid, issue a refund so money isn't stranded.
        refundIfPaid(order, request.getReason());

        // Publish event
        publishOrderStatusChangedEvent(order, previousStatus, request.getReason());

        // Notify via WebSocket
        notifyOrderStatusChange(order);

        return orderMapper.toDto(order);
    }

    /**
     * Refund a cancelled order if its payment was confirmed. Failures are logged
     * but never block the cancellation itself.
     */
    private void refundIfPaid(Order order, String reason) {
        if (order.getPaymentStatus() != PaymentStatus.CONFIRMED) {
            return;
        }
        try {
            paymentService.refundPayment(order.getId(), null,
                    reason != null ? reason : "Order cancelled");
            log.info("Refund issued for cancelled order {}", order.getExternalOrderNo());
        } catch (Exception e) {
            log.error("Failed to refund cancelled order {} (manual refund required): {}",
                    order.getExternalOrderNo(), e.getMessage());
        }
    }

    /**
     * Auto-cancel unpaid orders older than configured minutes.
     */
    @Transactional
    public int autoCancelUnpaidOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(autoCancelMinutes);
        List<Order> unpaidOrders = orderRepository.findUnpaidOrdersOlderThan(
                OrderStatus.CREATED, PaymentStatus.PENDING, cutoff
        );

        int cancelledCount = 0;
        for (Order order : unpaidOrders) {
            try {
                order.updateStatus(OrderStatus.CANCELLED);
                order.setCancellationReason("Auto-cancelled due to no payment after " + autoCancelMinutes + " minutes");
                orderRepository.save(order);
                cancelledCount++;
                log.info("Auto-cancelled unpaid order: {}", order.getExternalOrderNo());
            } catch (Exception e) {
                log.error("Failed to auto-cancel order {}: {}", order.getExternalOrderNo(), e.getMessage());
            }
        }

        return cancelledCount;
    }

    /**
     * Auto-complete orders that have been DELIVERED for longer than the grace period,
     * so they leave the restaurant's active list instead of accumulating forever.
     */
    @Transactional
    public int autoCompleteDeliveredOrders(int graceMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(graceMinutes);
        List<Order> delivered = orderRepository.findDeliveredBefore(cutoff);

        int completed = 0;
        for (Order order : delivered) {
            try {
                order.updateStatus(OrderStatus.COMPLETED);
                order.getRestaurant().incrementOrderCount();
                orderRepository.save(order);
                completed++;
            } catch (Exception e) {
                log.error("Failed to auto-complete delivered order {}: {}",
                        order.getExternalOrderNo(), e.getMessage());
            }
        }
        if (completed > 0) {
            log.info("Auto-completed {} delivered order(s) older than {} min", completed, graceMinutes);
        }
        return completed;
    }

    /**
     * Time out delivery orders that have been READY with no courier for too long:
     * cancel them and refund the customer so money isn't held for an order that
     * will never be delivered.
     */
    @Transactional
    public int cancelStuckNoCourierOrders(int timeoutMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Order> stuck = orderRepository.findReadyWithoutCourierBefore(cutoff);

        int cancelled = 0;
        for (Order order : stuck) {
            try {
                OrderStatus previousStatus = order.getStatus();
                order.updateStatus(OrderStatus.CANCELLED);
                String reason = "No courier available within " + timeoutMinutes + " minutes";
                order.setCancellationReason(reason);
                order = orderRepository.save(order);

                refundIfPaid(order, reason);
                publishOrderStatusChangedEvent(order, previousStatus, reason);
                notifyOrderStatusChange(order);
                cancelled++;
                log.warn("Cancelled stuck no-courier order {}: {}", order.getExternalOrderNo(), reason);
            } catch (Exception e) {
                log.error("Failed to cancel stuck order {}: {}", order.getExternalOrderNo(), e.getMessage());
            }
        }
        return cancelled;
    }

    private void validateOrderType(CreateOrderRequest request) {
        if (request.getOrderType() == OrderType.DELIVERY) {
            if (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank()) {
                throw new BusinessException("Delivery address is required for delivery orders");
            }
            if (request.getDeliveryLatitude() == null || request.getDeliveryLongitude() == null) {
                throw new BusinessException("Delivery coordinates (latitude and longitude) are required for delivery orders");
            }
        } else if (request.getOrderType() == OrderType.DINE_IN) {
            if (request.getTableId() == null || request.getTableId().isBlank()) {
                throw new BusinessException("Table ID is required for dine-in orders");
            }
        }
    }

    private List<OrderItem> processOrderItems(Order order, List<OrderItemRequest> itemRequests) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemReq : itemRequests) {
            MenuItem menuItem = menuItemRepository.findByIdWithVariantsAndOptions(itemReq.getMenuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", itemReq.getMenuItemId()));

            if (!menuItem.getInStock()) {
                throw new BusinessException("Item '" + menuItem.getName() + "' is out of stock");
            }

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .menuItem(menuItem)
                    .itemName(menuItem.getName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(menuItem.getEffectivePrice())
                    .specialInstructions(itemReq.getSpecialInstructions())
                    .build();

            // Process variant
            if (itemReq.getVariantId() != null) {
                ItemVariant variant = menuItem.getVariants().stream()
                        .filter(v -> v.getId().equals(itemReq.getVariantId()))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("ItemVariant", "id", itemReq.getVariantId()));

                orderItem.setVariantId(variant.getId());
                orderItem.setVariantName(variant.getName());
                orderItem.setVariantPriceDelta(variant.getPriceDelta());
            }

            // Process options/modifiers
            if (itemReq.getOptionIds() != null && !itemReq.getOptionIds().isEmpty()) {
                List<Map<String, Object>> modifiers = new ArrayList<>();
                BigDecimal modifiersTotal = BigDecimal.ZERO;

                for (Long optionId : itemReq.getOptionIds()) {
                    ItemOption option = menuItem.getOptions().stream()
                            .filter(o -> o.getId().equals(optionId))
                            .findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException("ItemOption", "id", optionId));

                    modifiers.add(Map.of(
                            "id", option.getId(),
                            "name", option.getName(),
                            "price", option.getPriceDelta()
                    ));
                    modifiersTotal = modifiersTotal.add(option.getPriceDelta());
                }

                orderItem.setModifiersJson(JsonUtils.toJson(modifiers));
                orderItem.setModifiersTotal(modifiersTotal);
            }

            orderItem.calculateTotalPrice();
            orderItems.add(orderItem);
        }

        return orderItems;
    }

    private void handleStatusChange(Order order, OrderStatus previousStatus, OrderStatus newStatus,
                                     UpdateOrderStatusRequest request) {
        switch (newStatus) {
            case ACCEPTED, PREPARING -> {
                // When restaurant accepts/starts preparing, set estimated times
                if (request.getEstimatedPrepTimeMinutes() != null) {
                    order.setEstimatedPrepTimeMinutes(request.getEstimatedPrepTimeMinutes());
                    order.setEstimatedDeliveryTime(
                            LocalDateTime.now().plusMinutes(request.getEstimatedPrepTimeMinutes() + 15)
                    );
                }
            }
            case DELIVERED, COMPLETED -> {
                if (newStatus == OrderStatus.COMPLETED) {
                    order.getRestaurant().incrementOrderCount();
                }
                // Record platform commission (idempotent — safe across DELIVERED then COMPLETED)
                commissionService.recordCommission(order);
            }
            default -> {}
        }
    }

    private void publishOrderCreatedEvent(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getExternalOrderNo(),
                order.getConsumer().getId(),
                order.getRestaurant().getId(),
                order.getOrderType(),
                order.getTotal(),
                order.getDeliveryAddress()
        );
        eventPublisher.publishAsync(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_KEY,
                event
        );
    }

    private void publishOrderStatusChangedEvent(Order order, OrderStatus previousStatus, String reason) {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                order.getId(),
                order.getExternalOrderNo(),
                order.getRestaurant().getId(),
                order.getConsumer().getId(),
                order.getCourier() != null ? order.getCourier().getId() : null,
                previousStatus,
                order.getStatus(),
                reason
        );
        eventPublisher.publishAsync(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_STATUS_CHANGED_KEY,
                event
        );
    }

    private void notifyRestaurant(Order order) {
        try {
            OrderDto dto = orderMapper.toDto(order);
            messagingTemplate.convertAndSend(
                    "/topic/restaurants/" + order.getRestaurant().getId() + "/orders",
                    dto
            );
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification: {}", e.getMessage());
        }
    }


    private void notifyOrderStatusChange(Order order) {
        try {
            OrderDto dto = orderMapper.toDto(order);
            // Notify order-specific channel
            messagingTemplate.convertAndSend("/topic/orders/" + order.getId(), dto);
            // Notify consumer
            messagingTemplate.convertAndSendToUser(
                    order.getConsumer().getEmail(),
                    "/queue/orders",
                    dto
            );

            // Notify restaurant about status change (cancellations, courier assigned, etc.)
            messagingTemplate.convertAndSend(
                    "/topic/restaurants/" + order.getRestaurant().getId() + "/orders",
                    dto
            );

            // If order is READY and is a delivery order, notify available couriers
            if (order.getStatus() == OrderStatus.READY &&
                order.getOrderType() == OrderType.DELIVERY &&
                order.getCourier() == null) {
                notifyAvailableCouriers(order);
            }
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification: {}", e.getMessage());
        }
    }

    /**
     * Notify available couriers about a new order ready for pickup.
     */
    private void notifyAvailableCouriers(Order order) {
        try {
            // Build simplified order notification for couriers
            Map<String, Object> orderNotification = new java.util.HashMap<>();
            orderNotification.put("orderId", order.getId());
            orderNotification.put("externalOrderNo", order.getExternalOrderNo());
            orderNotification.put("restaurantId", order.getRestaurant().getId());
            orderNotification.put("restaurantName", order.getRestaurant().getName());
            orderNotification.put("restaurantAddress", order.getRestaurant().getFullAddress());
            orderNotification.put("restaurantLat", order.getRestaurant().getLatitude());
            orderNotification.put("restaurantLng", order.getRestaurant().getLongitude());
            orderNotification.put("deliveryAddress", order.getDeliveryAddress() != null ? order.getDeliveryAddress() : "");
            orderNotification.put("deliveryLat", order.getDeliveryLatitude() != null ? order.getDeliveryLatitude() : BigDecimal.ZERO);
            orderNotification.put("deliveryLng", order.getDeliveryLongitude() != null ? order.getDeliveryLongitude() : BigDecimal.ZERO);
            orderNotification.put("deliveryFee", order.getDeliveryFee());
            orderNotification.put("itemCount", order.getItems() != null ? order.getItems().size() : 0);
            orderNotification.put("createdAt", order.getCreatedAt().toString());
            orderNotification.put("readyAt", order.getReadyAt() != null ? order.getReadyAt().toString() : "");

            // Broadcast to all couriers topic (couriers can filter by location on client side)
            messagingTemplate.convertAndSend("/topic/couriers/orders/available", orderNotification);

            // Also send to individual online couriers
            List<Courier> onlineCouriers = courierRepository.findOnlineCouriers();
            for (Courier courier : onlineCouriers) {
                messagingTemplate.convertAndSendToUser(
                        courier.getUser().getEmail(),
                        "/queue/orders/new",
                        orderNotification
                );
            }

            log.info("Notified {} online couriers about order {} ready for pickup",
                    onlineCouriers.size(), order.getExternalOrderNo());
        } catch (Exception e) {
            log.warn("Failed to notify couriers about ready order: {}", e.getMessage());
        }
    }


    // ============== Access Validation Methods ==============

    /**
     * Validate that user has access to the order.
     * Consumer can only access their own orders.
     * Restaurant owner/staff can access orders for their restaurants.
     * Courier can access orders assigned to them.
     * Admin/Platform can access all orders.
     */
    @Transactional(readOnly = true)
    /**
     * Which statuses each actor may set through the generic status endpoint.
     *
     * <p>{@code canTransitionTo} enforces the ORDER of the state machine but not
     * WHO may drive it, and access validation only proves the caller is party to
     * this order. Between them a restaurant could walk its own order
     * READY -> PICKED_UP -> IN_TRANSIT -> DELIVERED with no courier involved —
     * every step a legal transition — and on a cash platform that is what marks
     * the order settled. A courier could likewise drive the kitchen's states.
     *
     * <p>COURIER_ASSIGNED, COMPLETED and REFUNDED are intentionally in nobody's
     * set: they are set by dispatch, the auto-complete scheduler and the refund
     * path respectively, never by a party to the order.
     */
    private static final Set<OrderStatus> RESTAURANT_SETTABLE = EnumSet.of(
            OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.CANCELLED);

    private static final Set<OrderStatus> COURIER_SETTABLE = EnumSet.of(
            OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT, OrderStatus.DELIVERED);

    private void validateActorMaySet(Long orderId, OrderStatus newStatus, boolean isAdminOrPlatform,
                                     boolean isRestaurantRole, boolean isCourier) {
        if (isAdminOrPlatform) {
            return;
        }
        if (isRestaurantRole && RESTAURANT_SETTABLE.contains(newStatus)) {
            return;
        }
        if (isCourier && COURIER_SETTABLE.contains(newStatus)) {
            return;
        }
        log.warn("SECURITY: actor (restaurant={}, courier={}) attempted to set order {} to {}",
                isRestaurantRole, isCourier, orderId, newStatus);
        throw new BusinessException("You are not allowed to set this order status");
    }

    public void validateOrderAccess(Long orderId, Long userId, boolean isAdminOrPlatform,
                                    boolean isRestaurantRole, boolean isCourier) {
        if (isAdminOrPlatform) {
            return; // Admin and Platform can access any order
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        Long orderRestaurantId = order.getRestaurant().getId();
        Long orderConsumerId = order.getConsumer().getId();

        // Consumer can access their own orders
        if (orderConsumerId.equals(userId)) {
            return;
        }

        // Restaurant owner can ONLY access orders for THEIR OWN restaurant
        if (isRestaurantRole) {
            boolean ownsThisRestaurant = restaurantService.isRestaurantOwner(orderRestaurantId, userId);
            if (ownsThisRestaurant) {
                return;
            }
            // Restaurant trying to access another restaurant's order - log and deny
            log.warn("SECURITY: Restaurant user {} attempted to access order {} belonging to restaurant {}",
                    userId, orderId, orderRestaurantId);
            throw new BusinessException("You don't have permission to access orders from other restaurants");
        }

        // Courier can access orders assigned to them
        if (isCourier && order.getCourier() != null && order.getCourier().getUser().getId().equals(userId)) {
            return;
        }

        log.warn("SECURITY: User {} attempted to access order {} without permission (restaurant={}, consumer={})",
                userId, orderId, orderRestaurantId, orderConsumerId);
        throw new BusinessException("You don't have permission to access this order");
    }

    /**
     * Get order entity by ID (internal use with access check).
     */
    @Transactional(readOnly = true)
    public Order getOrderEntityById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
    }

    /**
     * Get order tracking information.
     */
    @Transactional(readOnly = true)
    public OrderTrackingDto getOrderTracking(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        OrderTrackingDto.OrderTrackingDtoBuilder builder = OrderTrackingDto.builder()
                .id(order.getId())
                .externalOrderNo(order.getExternalOrderNo())
                .status(order.getStatus())
                .restaurantId(order.getRestaurant().getId())
                .restaurantName(order.getRestaurant().getName())
                .restaurantAddress(order.getRestaurant().getFullAddress())
                .restaurantLat(order.getRestaurant().getLatitude())
                .restaurantLng(order.getRestaurant().getLongitude())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryLat(order.getDeliveryLatitude())
                .deliveryLng(order.getDeliveryLongitude())
                .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                .createdAt(order.getCreatedAt())
                .acceptedAt(order.getAcceptedAt())
                .readyAt(order.getReadyAt())
                .pickedUpAt(order.getPickedUpAt())
                .deliveredAt(order.getDeliveredAt());

        // Add courier info if assigned
        if (order.getCourier() != null) {
            Courier courier = order.getCourier();
            builder.courierId(courier.getId())
                    .courierName(courier.getUser().getFullName())
                    .courierPhone(courier.getUser().getPhone())
                    .courierLat(courier.getCurrentLat())
                    .courierLng(courier.getCurrentLng());
        }

        return builder.build();
    }

    /**
     * Create a new order based on a previous order (reorder).
     */
    @Transactional
    @Auditable(action = "REORDER", entityType = "Order")
    public OrderDto reorder(Long orderId, Long consumerId) {
        Order previousOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Validate consumer owns the order
        if (!previousOrder.getConsumer().getId().equals(consumerId)) {
            throw new BusinessException("You can only reorder your own orders");
        }

        // Validate restaurant is still active and open
        Restaurant restaurant = previousOrder.getRestaurant();
        if (!restaurant.isCurrentlyOpen()) {
            throw new BusinessException("Restaurant is currently closed");
        }

        // Build order items from previous order
        List<OrderItemRequest> itemRequests = new ArrayList<>();
        for (OrderItem item : previousOrder.getItems()) {
            // Check if menu item is still available
            MenuItem menuItem = menuItemRepository.findById(item.getMenuItem().getId()).orElse(null);
            if (menuItem == null || !menuItem.getInStock()) {
                log.warn("Skipping unavailable item {} in reorder", item.getItemName());
                continue;
            }

            OrderItemRequest itemReq = new OrderItemRequest();
            itemReq.setMenuItemId(item.getMenuItem().getId());
            itemReq.setQuantity(item.getQuantity());
            itemReq.setVariantId(item.getVariantId());
            itemReq.setSpecialInstructions(item.getSpecialInstructions());

            // Parse options from modifiersJson
            if (item.getModifiersJson() != null && !item.getModifiersJson().isBlank()) {
                List<Long> optionIds = extractOptionIdsFromModifiersJson(item.getModifiersJson());
                itemReq.setOptionIds(optionIds);
            }

            itemRequests.add(itemReq);
        }

        if (itemRequests.isEmpty()) {
            throw new BusinessException("No items from the previous order are currently available");
        }

        // Create new order request
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRestaurantId(restaurant.getId());
        request.setOrderType(previousOrder.getOrderType());
        request.setDeliveryAddress(previousOrder.getDeliveryAddress());
        request.setDeliveryLatitude(previousOrder.getDeliveryLatitude());
        request.setDeliveryLongitude(previousOrder.getDeliveryLongitude());
        request.setDeliveryInstructions(previousOrder.getDeliveryInstructions());
        request.setTableId(previousOrder.getTableId());
        request.setCustomerName(previousOrder.getCustomerName());
        request.setCustomerPhone(previousOrder.getCustomerPhone());
        request.setItems(itemRequests);

        log.info("Creating reorder from order {} for consumer {}", orderId, consumerId);
        return createOrder(consumerId, request, null);
    }

    /**
     * Extract option IDs from modifiersJson.
     * Format: [{"id": 1, "name": "Extra Cheese", "price": 1.50}, ...]
     */
    private List<Long> extractOptionIdsFromModifiersJson(String modifiersJson) {
        List<Long> optionIds = new ArrayList<>();
        try {
            JsonUtils.parseJson(modifiersJson).ifPresent(jsonNode -> {
                if (jsonNode.isArray()) {
                    for (var node : jsonNode) {
                        if (node.has("id")) {
                            optionIds.add(node.get("id").asLong());
                        }
                    }
                }
            });
        } catch (Exception e) {
            log.warn("Failed to parse modifiersJson: {}", e.getMessage());
        }
        return optionIds;
    }
}
