package com.fooddelivery.courier.service;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.service.UserService;
import com.fooddelivery.common.annotation.Auditable;
import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.common.event.EventPublisher;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.DuplicateResourceException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.courier.dto.*;
import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.courier.entity.CourierStatus;
import com.fooddelivery.courier.entity.VehicleType;
import com.fooddelivery.courier.repository.CourierRepository;
import com.fooddelivery.order.entity.DeliveryIssue;
import com.fooddelivery.order.entity.DeliveryIssueType;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.entity.PaymentStatus;
import com.fooddelivery.order.event.CourierAssignedEvent;
import com.fooddelivery.order.event.OrderStatusChangedEvent;
import com.fooddelivery.order.repository.DeliveryIssueRepository;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.order.repository.PaymentRepository;
import com.fooddelivery.notification.service.PersistentNotificationService;
import com.fooddelivery.analytics.financial.service.CommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for courier operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourierService {

    private final CourierRepository courierRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final DeliveryIssueRepository deliveryIssueRepository;
    private final UserService userService;
    private final EventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final PersistentNotificationService notificationService;
    private final CommissionService commissionService;

    /**
     * Register as a courier.
     */
    @Transactional
    @Auditable(action = "REGISTER_COURIER", entityType = "Courier")
    public CourierDto registerCourier(Long userId, CreateCourierRequest request) {
        if (courierRepository.existsByUserId(userId)) {
            throw new DuplicateResourceException("Courier", "userId", userId);
        }

        User user = userService.getUserEntityById(userId);
        user.addRole(Role.COURIER);

        Courier courier = Courier.builder()
                .user(user)
                .status(CourierStatus.PENDING_APPROVAL)
                .vehicleType(request.getVehicleType())
                .vehicleNumber(request.getVehicleNumber())
                .licenseNumber(request.getLicenseNumber())
                .preferredRadiusKm(request.getPreferredRadiusKm() != null ? request.getPreferredRadiusKm() : 5)
                .build();

        courier = courierRepository.save(courier);
        log.info("Courier registered: {} (user: {})", courier.getId(), userId);

        return toDto(courier);
    }

    /**
     * Get courier by ID.
     */
    @Transactional(readOnly = true)
    public CourierDto getCourierById(Long id) {
        Courier courier = courierRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "id", id));
        return toDto(courier);
    }

    /**
     * Get courier by user ID.
     */
    @Transactional(readOnly = true)
    public CourierDto getCourierByUserId(Long userId) {
        Courier courier = courierRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "userId", userId));
        return toDto(courier);
    }

    /**
     * Update courier status (go online/offline).
     */
    @Transactional
    public CourierDto updateStatus(Long courierId, CourierStatus status) {
        Courier courier = courierRepository.findByIdWithUser(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "id", courierId));

        if (!courier.getVerified() && status == CourierStatus.AVAILABLE) {
            throw new BusinessException("Courier must be verified before going online");
        }

        courier.setStatus(status);
        courier = courierRepository.save(courier);
        log.info("Courier {} status updated to: {}", courierId, status);

        return toDto(courier);
    }

    /**
     * Update courier location.
     * Uses direct UPDATE query to avoid optimistic locking conflicts with concurrent operations.
     */
    @Transactional
    public void updateLocation(Long courierId, BigDecimal lat, BigDecimal lng) {
        int updated = courierRepository.updateLocation(courierId, lat, lng);
        if (updated == 0) {
            throw new ResourceNotFoundException("Courier", "id", courierId);
        }

        // Broadcast location to interested parties
        broadcastLocationUpdate(courierId, lat, lng);
    }

    /**
     * Broadcast location update without loading the full entity.
     */
    private void broadcastLocationUpdate(Long courierId, BigDecimal lat, BigDecimal lng) {
        try {
            CourierLocationDto location = CourierLocationDto.builder()
                    .courierId(courierId)
                    .lat(lat)
                    .lng(lng)
                    .timestamp(LocalDateTime.now())
                    .build();

            messagingTemplate.convertAndSend(
                    "/topic/couriers/" + courierId + "/location",
                    location
            );
        } catch (Exception e) {
            log.warn("Failed to broadcast courier location: {}", e.getMessage());
        }
    }

    /**
     * Find available couriers near a location.
     */
    @Transactional(readOnly = true)
    public List<CourierDto> findAvailableCouriers(BigDecimal lat, BigDecimal lng, double radiusKm) {
        List<Long> courierIds = courierRepository.findNearbyCourierIds(lat, lng, radiusKm);
        if (courierIds.isEmpty()) {
            return List.of();
        }
        List<Courier> couriers = courierRepository.findByIdsWithUser(courierIds);
        return couriers.stream().map(this::toDto).toList();
    }

    /**
     * Accept an order assignment.
     * Uses pessimistic locking to prevent race condition where multiple couriers accept the same order.
     */
    @Transactional
    @Auditable(action = "ACCEPT_ORDER", entityType = "Courier")
    public CourierDto acceptOrder(Long courierId, Long orderId) {
        Courier courier = courierRepository.findByIdWithUser(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "id", courierId));

        if (!courier.isAvailable()) {
            throw new BusinessException("Courier is not available to accept orders");
        }

        // Use pessimistic lock to prevent race condition
        Order order = orderRepository.findByIdForCourierAssignment(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getCourier() != null) {
            throw new BusinessException("Order already has a courier assigned");
        }

        if (order.getStatus() != OrderStatus.ACCEPTED && order.getStatus() != OrderStatus.PREPARING
                && order.getStatus() != OrderStatus.READY) {
            throw new BusinessException("Order must be accepted by restaurant before courier assignment");
        }

        // Assign courier to order
        order.setCourier(courier);
        order.updateStatus(OrderStatus.COURIER_ASSIGNED);
        orderRepository.save(order);

        // Update courier
        courier.assignOrder();
        courier = courierRepository.save(courier);

        log.info("Courier {} accepted order {}", courierId, orderId);

        // Publish event
        publishCourierAssignedEvent(order, courier);

        // Broadcast to other couriers that this order is no longer available
        broadcastOrderTaken(order, courier);

        return toDto(courier);
    }

    /**
     * Complete a delivery.
     */
    @Transactional
    @Auditable(action = "COMPLETE_DELIVERY", entityType = "Courier")
    public CourierDto completeDelivery(Long courierId, Long orderId) {
        Courier courier = courierRepository.findByIdWithUser(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "id", courierId));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getCourier().getId().equals(courierId)) {
            throw new BusinessException("This order is not assigned to you");
        }

        order.updateStatus(OrderStatus.DELIVERED);

        // For cash payments, mark payment as confirmed when order is delivered
        if (order.getPaymentStatus() == PaymentStatus.PENDING) {
            final Order orderRef = order;
            paymentRepository.findByOrderId(orderId).ifPresentOrElse(
                    payment -> {
                        if ("cash".equalsIgnoreCase(payment.getPaymentMethod())) {
                            orderRef.setPaymentStatus(PaymentStatus.CONFIRMED);
                            log.info("Cash payment confirmed for order {}", orderRef.getExternalOrderNo());
                        }
                    },
                    () -> {
                        orderRef.setPaymentStatus(PaymentStatus.CONFIRMED);
                        log.info("Payment confirmed for order {} (no payment record - assumed cash)", orderRef.getExternalOrderNo());
                    }
            );
        }

        order = orderRepository.save(order);

        // Record platform commission for the delivered order
        commissionService.recordCommission(order);

        courier.completeOrder();
        // Add delivery earnings (simplified)
        courier.addEarnings(new BigDecimal("5.00"));
        courier = courierRepository.save(courier);

        log.info("Courier {} completed delivery for order {}", courierId, orderId);

        return toDto(courier);
    }

    /**
     * Verify a courier (admin).
     */
    @Transactional
    @Auditable(action = "VERIFY_COURIER", entityType = "Courier")
    public CourierDto verifyCourier(Long courierId) {
        Courier courier = courierRepository.findByIdWithUser(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "id", courierId));

        courier.setVerified(true);
        courier.setVerifiedAt(java.time.LocalDateTime.now());
        courier.setStatus(CourierStatus.OFFLINE);
        courier = courierRepository.save(courier);

        log.info("Courier {} verified", courierId);
        return toDto(courier);
    }

    /**
     * Get all couriers with pagination.
     */
    @Transactional(readOnly = true)
    public PagedResponse<CourierDto> getAllCouriers(Pageable pageable) {
        Page<Courier> couriers = courierRepository.findAllWithUser(pageable);
        return PagedResponse.from(couriers, couriers.getContent().stream()
                .map(this::toDto)
                .toList());
    }

    /**
     * Get pending couriers awaiting approval.
     */
    @Transactional(readOnly = true)
    public PagedResponse<CourierDto> getPendingCouriers(Pageable pageable) {
        Page<Courier> couriers = courierRepository.findPendingVerification(pageable);
        return PagedResponse.from(couriers, couriers.getContent().stream()
                .map(this::toDto)
                .toList());
    }

    /**
     * Get couriers by status.
     */
    @Transactional(readOnly = true)
    public PagedResponse<CourierDto> getCouriersByStatus(CourierStatus status, Pageable pageable) {
        Page<Courier> couriers = courierRepository.findByStatus(status, pageable);
        return PagedResponse.from(couriers, couriers.getContent().stream()
                .map(this::toDto)
                .toList());
    }

    /**
     * Get all online couriers with location data (for map display).
     */
    @Transactional(readOnly = true)
    public List<CourierDto> getOnlineCouriers() {
        List<Courier> couriers = courierRepository.findOnlineCouriersWithLocation();
        return couriers.stream().map(this::toDto).toList();
    }

    /**
     * Reject a pending courier application.
     */
    @Transactional
    @Auditable(action = "REJECT_COURIER", entityType = "Courier")
    public CourierDto rejectCourier(Long courierId, String reason) {
        Courier courier = courierRepository.findByIdWithUser(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "id", courierId));

        if (courier.getStatus() != CourierStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only pending couriers can be rejected");
        }

        courier.setStatus(CourierStatus.SUSPENDED);
        courier.setVerified(false);
        courier = courierRepository.save(courier);

        log.info("Courier {} rejected. Reason: {}", courierId, reason);
        return toDto(courier);
    }

    /**
     * Suspend a courier.
     */
    @Transactional
    @Auditable(action = "SUSPEND_COURIER", entityType = "Courier")
    public CourierDto suspendCourier(Long courierId, String reason) {
        Courier courier = courierRepository.findByIdWithUser(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "id", courierId));

        if (courier.getStatus() == CourierStatus.SUSPENDED) {
            throw new BusinessException("Courier is already suspended");
        }

        courier.setStatus(CourierStatus.SUSPENDED);
        courier = courierRepository.save(courier);

        log.info("Courier {} suspended. Reason: {}", courierId, reason);
        return toDto(courier);
    }

    /**
     * Activate/reactivate a courier.
     */
    @Transactional
    @Auditable(action = "ACTIVATE_COURIER", entityType = "Courier")
    public CourierDto activateCourier(Long courierId) {
        Courier courier = courierRepository.findByIdWithUser(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "id", courierId));

        if (courier.getStatus() != CourierStatus.SUSPENDED &&
            courier.getStatus() != CourierStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only suspended or pending couriers can be activated");
        }

        courier.setStatus(CourierStatus.OFFLINE);
        courier.setVerified(true);
        courier.setVerifiedAt(java.time.LocalDateTime.now());
        courier = courierRepository.save(courier);

        log.info("Courier {} activated", courierId);
        return toDto(courier);
    }

    /**
     * Update courier profile (admin).
     */
    @Transactional
    @Auditable(action = "UPDATE_COURIER", entityType = "Courier")
    public CourierDto updateCourierProfile(Long courierId, UpdateCourierRequest request) {
        Courier courier = courierRepository.findByIdWithUser(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "id", courierId));

        if (request.getVehicleType() != null) {
            courier.setVehicleType(request.getVehicleType());
        }
        if (request.getVehicleNumber() != null) {
            courier.setVehicleNumber(request.getVehicleNumber());
        }
        if (request.getLicenseNumber() != null) {
            courier.setLicenseNumber(request.getLicenseNumber());
        }
        if (request.getPreferredRadiusKm() != null) {
            courier.setPreferredRadiusKm(request.getPreferredRadiusKm());
        }
        if (request.getMaxConcurrentOrders() != null) {
            courier.setMaxConcurrentOrders(request.getMaxConcurrentOrders());
        }

        courier = courierRepository.save(courier);
        log.info("Courier {} profile updated", courierId);
        return toDto(courier);
    }

    /**
     * Get courier statistics.
     */
    @Transactional(readOnly = true)
    public CourierStatisticsDto getCourierStatistics() {
        long totalCouriers = courierRepository.count();
        long pendingCouriers = courierRepository.countByStatus(CourierStatus.PENDING_APPROVAL);
        long onlineCouriers = courierRepository.countByStatus(CourierStatus.AVAILABLE) +
                              courierRepository.countByStatus(CourierStatus.BUSY);
        long offlineCouriers = courierRepository.countByStatus(CourierStatus.OFFLINE);
        long suspendedCouriers = courierRepository.countByStatus(CourierStatus.SUSPENDED);
        long verifiedCouriers = courierRepository.countByVerified(true);

        return CourierStatisticsDto.builder()
                .totalCouriers(totalCouriers)
                .pendingApproval(pendingCouriers)
                .online(onlineCouriers)
                .offline(offlineCouriers)
                .suspended(suspendedCouriers)
                .verified(verifiedCouriers)
                .available(courierRepository.countByStatus(CourierStatus.AVAILABLE))
                .busy(courierRepository.countByStatus(CourierStatus.BUSY))
                .onBreak(courierRepository.countByStatus(CourierStatus.ON_BREAK))
                .build();
    }

    // ===== Courier App Endpoints =====

    /**
     * Get available orders for a courier to accept.
     */
    @Transactional(readOnly = true)
    public List<CourierOrderDto> getAvailableOrders(Long courierId) {
        // Verify courier exists and is verified
        Courier courier = courierRepository.findByIdWithUser(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "id", courierId));

        if (!courier.getVerified()) {
            throw new BusinessException("Courier must be verified to see available orders");
        }

        List<Order> orders = orderRepository.findAvailableOrdersForCourier();
        return orders.stream().map(this::toOrderDto).toList();
    }

    /**
     * Get active orders for a courier (in progress).
     */
    @Transactional(readOnly = true)
    public List<CourierOrderDto> getActiveOrders(Long courierId) {
        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.READY,
                OrderStatus.COURIER_ASSIGNED,
                OrderStatus.PICKED_UP,
                OrderStatus.IN_TRANSIT
        );
        List<Order> orders = orderRepository.findActiveOrdersByCourier(courierId, activeStatuses);
        log.info("Active orders query for courierId={}: found {} orders (statuses={})",
                courierId, orders.size(), activeStatuses);
        if (orders.isEmpty()) {
            // Debug: check if courier has ANY orders at all
            long totalOrders = orderRepository.countByCourierId(courierId);
            log.warn("Courier {} has 0 active orders but {} total orders in DB", courierId, totalOrders);
        }
        return orders.stream().map(this::toOrderDto).toList();
    }

    /**
     * Get order history for a courier.
     */
    @Transactional(readOnly = true)
    public PagedResponse<CourierOrderDto> getOrderHistory(Long courierId, Pageable pageable) {
        Page<Order> orders = orderRepository.findOrderHistoryByCourier(courierId, pageable);
        return PagedResponse.from(orders, orders.getContent().stream()
                .map(this::toOrderDto)
                .toList());
    }

    /**
     * Get earnings for a courier.
     */
    @Transactional(readOnly = true)
    public CourierEarningsDto getEarnings(Long courierId) {
        Courier courier = courierRepository.findByIdWithUser(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "id", courierId));

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        java.time.LocalDateTime startOfWeek = now.toLocalDate().minusDays(now.getDayOfWeek().getValue() - 1).atStartOfDay();
        java.time.LocalDateTime startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay();

        BigDecimal todayEarnings = orderRepository.sumCourierEarningsSince(courierId, startOfToday);
        BigDecimal weekEarnings = orderRepository.sumCourierEarningsSince(courierId, startOfWeek);
        BigDecimal monthEarnings = orderRepository.sumCourierEarningsSince(courierId, startOfMonth);

        long todayDeliveries = orderRepository.countDeliveriesByCourierSince(courierId, startOfToday);
        long weekDeliveries = orderRepository.countDeliveriesByCourierSince(courierId, startOfWeek);
        long monthDeliveries = orderRepository.countDeliveriesByCourierSince(courierId, startOfMonth);

        BigDecimal avgPerDelivery = courier.getTotalDeliveries() > 0
                ? courier.getTotalEarnings().divide(BigDecimal.valueOf(courier.getTotalDeliveries()), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Split earnings by payment method
        BigDecimal cashEarnings = orderRepository.sumCourierEarningsByPaymentMethodSince(
                courierId, startOfMonth, "cash");
        BigDecimal cardEarnings = orderRepository.sumCourierEarningsByPaymentMethodSince(
                courierId, startOfMonth, "card");

        return CourierEarningsDto.builder()
                .todayEarnings(todayEarnings != null ? todayEarnings : BigDecimal.ZERO)
                .weekEarnings(weekEarnings != null ? weekEarnings : BigDecimal.ZERO)
                .monthEarnings(monthEarnings != null ? monthEarnings : BigDecimal.ZERO)
                .totalEarnings(courier.getTotalEarnings())
                .todayDeliveries((int) todayDeliveries)
                .weekDeliveries((int) weekDeliveries)
                .monthDeliveries((int) monthDeliveries)
                .totalDeliveries(courier.getTotalDeliveries())
                .averagePerDelivery(avgPerDelivery)
                .cashEarnings(cashEarnings != null ? cashEarnings : BigDecimal.ZERO)
                .cardEarnings(cardEarnings != null ? cardEarnings : BigDecimal.ZERO)
                .withdrawableBalance(cardEarnings != null ? cardEarnings : BigDecimal.ZERO)
                .pendingPayout(BigDecimal.ZERO)
                .build();
    }

    // ===== Order Management Methods =====

    /**
     * Accept an order and return order details.
     * Uses pessimistic locking to prevent race condition where multiple couriers accept the same order.
     */
    @Transactional
    @Auditable(action = "ACCEPT_ORDER", entityType = "Courier")
    public CourierOrderDto acceptOrderAndGetDetails(Long courierId, Long orderId) {
        Courier courier = courierRepository.findByIdWithUser(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "id", courierId));

        if (!courier.isAvailable()) {
            throw new BusinessException("Courier is not available to accept orders");
        }

        // Use pessimistic lock to prevent race condition
        Order order = orderRepository.findByIdForCourierAssignment(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getCourier() != null) {
            throw new BusinessException("Order already has a courier assigned");
        }

        if (order.getStatus() != OrderStatus.ACCEPTED && order.getStatus() != OrderStatus.PREPARING
                && order.getStatus() != OrderStatus.READY) {
            throw new BusinessException("Order must be accepted by restaurant before courier assignment");
        }

        order.setCourier(courier);
        order.updateStatus(OrderStatus.COURIER_ASSIGNED);
        order = orderRepository.save(order);

        courier.assignOrder();
        courierRepository.save(courier);

        log.info("Courier {} accepted order {}", courierId, orderId);
        publishCourierAssignedEvent(order, courier);

        // Broadcast to other couriers that this order is no longer available
        broadcastOrderTaken(order, courier);

        return toOrderDto(order);
    }

    /**
     * Get order details for a courier.
     */
    @Transactional(readOnly = true)
    public CourierOrderDto getOrderDetails(Long courierId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getCourier() == null || !order.getCourier().getId().equals(courierId)) {
            throw new BusinessException("This order is not assigned to you");
        }

        return toOrderDto(order);
    }

    /**
     * Confirm order pickup from restaurant.
     */
    @Transactional
    @Auditable(action = "PICKUP_ORDER", entityType = "Courier")
    public CourierOrderDto confirmPickup(Long courierId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getCourier() == null || !order.getCourier().getId().equals(courierId)) {
            throw new BusinessException("This order is not assigned to you");
        }

        if (order.getStatus() != OrderStatus.COURIER_ASSIGNED && order.getStatus() != OrderStatus.PICKED_UP) {
            throw new BusinessException("Order cannot be picked up in current status: " + order.getStatus());
        }

        // Don't allow pickup if restaurant hasn't marked the order as ready
        if (order.getReadyAt() == null && order.getStatus() == OrderStatus.COURIER_ASSIGNED) {
            throw new BusinessException("Order is not ready for pickup yet. Wait for the restaurant to prepare it.");
        }

        OrderStatus previousStatus = order.getStatus();
        order.updateStatus(OrderStatus.PICKED_UP);
        order = orderRepository.save(order);

        log.info("Courier {} picked up order {}", courierId, orderId);

        // Publish event to trigger notifications
        publishOrderStatusChangedEvent(order, previousStatus, null);

        return toOrderDto(order);
    }

    /**
     * Start transit to customer.
     */
    @Transactional
    @Auditable(action = "START_TRANSIT", entityType = "Courier")
    public CourierOrderDto startTransit(Long courierId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getCourier() == null || !order.getCourier().getId().equals(courierId)) {
            throw new BusinessException("This order is not assigned to you");
        }

        if (order.getStatus() != OrderStatus.PICKED_UP) {
            throw new BusinessException("Order must be picked up before starting transit");
        }

        OrderStatus previousStatus = order.getStatus();
        order.updateStatus(OrderStatus.IN_TRANSIT);
        order = orderRepository.save(order);

        log.info("Courier {} started transit for order {}", courierId, orderId);

        // Publish event to trigger notifications
        publishOrderStatusChangedEvent(order, previousStatus, null);

        return toOrderDto(order);
    }

    /**
     * Complete delivery and return order details.
     */
    @Transactional
    @Auditable(action = "COMPLETE_DELIVERY", entityType = "Courier")
    public CourierOrderDto completeDeliveryAndGetDetails(Long courierId, Long orderId) {
        Courier courier = courierRepository.findByIdWithUser(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "id", courierId));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getCourier() == null || !order.getCourier().getId().equals(courierId)) {
            throw new BusinessException("This order is not assigned to you");
        }

        OrderStatus previousStatus = order.getStatus();
        order.updateStatus(OrderStatus.DELIVERED);

        // For cash payments, mark payment as confirmed when order is delivered
        if (order.getPaymentStatus() == PaymentStatus.PENDING) {
            final Order orderToUpdate = order;
            paymentRepository.findByOrderId(orderId).ifPresentOrElse(
                    payment -> {
                        if ("cash".equalsIgnoreCase(payment.getPaymentMethod())) {
                            orderToUpdate.setPaymentStatus(PaymentStatus.CONFIRMED);
                            log.info("Cash payment confirmed for order {}", orderToUpdate.getExternalOrderNo());
                        }
                    },
                    () -> {
                        // No payment record exists - assume cash payment
                        orderToUpdate.setPaymentStatus(PaymentStatus.CONFIRMED);
                        log.info("Payment confirmed for order {} (no payment record - assumed cash)", orderToUpdate.getExternalOrderNo());
                    }
            );
        }

        order = orderRepository.save(order);

        // Record platform commission for the delivered order
        commissionService.recordCommission(order);

        courier.completeOrder();
        courier.addEarnings(order.getDeliveryFee().add(order.getTipAmount() != null ? order.getTipAmount() : BigDecimal.ZERO));
        courierRepository.save(courier);

        log.info("Courier {} completed delivery for order {}", courierId, orderId);

        // Publish event to trigger notifications
        publishOrderStatusChangedEvent(order, previousStatus, null);

        return toOrderDto(order);
    }

    /**
     * Report an issue with an order.
     */
    @Transactional
    @Auditable(action = "REPORT_ISSUE", entityType = "Courier")
    public void reportOrderIssue(Long courierId, Long orderId, String issueType, String description) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        Courier courier = order.getCourier();
        if (courier == null || !courier.getId().equals(courierId)) {
            throw new BusinessException("This order is not assigned to you");
        }

        // Parse issue type
        DeliveryIssueType deliveryIssueType;
        try {
            deliveryIssueType = DeliveryIssueType.valueOf(issueType.toUpperCase());
        } catch (IllegalArgumentException e) {
            deliveryIssueType = DeliveryIssueType.OTHER;
        }

        // Create delivery issue entity
        DeliveryIssue deliveryIssue = DeliveryIssue.builder()
                .order(order)
                .courier(courier)
                .issueType(deliveryIssueType)
                .description(description)
                .resolved(false)
                .build();
        deliveryIssueRepository.save(deliveryIssue);

        log.warn("Courier {} reported issue for order {}: {} - {}", courierId, orderId, issueType, description);

        // Send notifications to consumer, restaurant owner, and admin
        String courierName = courier.getUser().getFullName();
        notificationService.notifyCourierIssueReported(
                orderId,
                order.getConsumer().getId(),
                order.getRestaurant().getId(),
                order.getExternalOrderNo(),
                issueType,
                description,
                courierName
        );
    }

    private CourierOrderDto toOrderDto(Order order) {
        return CourierOrderDto.builder()
                .orderId(order.getId())
                .externalOrderNo(order.getExternalOrderNo())
                .restaurantId(order.getRestaurant().getId())
                .restaurantName(order.getRestaurant().getName())
                .restaurantAddress(order.getRestaurant().getFullAddress())
                .restaurantLat(order.getRestaurant().getLatitude())
                .restaurantLng(order.getRestaurant().getLongitude())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryLat(order.getDeliveryLatitude())
                .deliveryLng(order.getDeliveryLongitude())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .deliveryInstructions(order.getDeliveryInstructions())
                .status(order.getStatus())
                .deliveryFee(order.getDeliveryFee())
                .tipAmount(order.getTipAmount())
                .total(order.getTotal())
                .itemCount(order.getItems() != null ? order.getItems().size() : 0)
                .createdAt(order.getCreatedAt())
                .readyAt(order.getReadyAt())
                .pickedUpAt(order.getPickedUpAt())
                .deliveredAt(order.getDeliveredAt())
                .build();
    }

    private void broadcastLocation(Courier courier) {
        try {
            CourierLocationDto location = CourierLocationDto.builder()
                    .courierId(courier.getId())
                    .lat(courier.getCurrentLat())
                    .lng(courier.getCurrentLng())
                    .timestamp(courier.getLocationUpdatedAt())
                    .build();

            messagingTemplate.convertAndSend(
                    "/topic/couriers/" + courier.getId() + "/location",
                    location
            );
        } catch (Exception e) {
            log.warn("Failed to broadcast courier location: {}", e.getMessage());
        }
    }

    /**
     * Broadcast to all couriers that an order has been taken.
     * This immediately removes the order from other couriers' available orders list.
     */
    private void broadcastOrderTaken(Order order, Courier courier) {
        try {
            java.util.Map<String, Object> notification = new java.util.HashMap<>();
            notification.put("type", "ORDER_TAKEN");
            notification.put("orderId", order.getId());
            notification.put("externalOrderNo", order.getExternalOrderNo());
            notification.put("courierId", courier.getId());
            notification.put("courierName", courier.getUser().getFullName());
            notification.put("timestamp", LocalDateTime.now().toString());

            // Broadcast to all couriers listening for available orders
            messagingTemplate.convertAndSend("/topic/couriers/orders/available", notification);

            // Also send to the specific order topic so any courier with it open gets notified
            messagingTemplate.convertAndSend("/topic/orders/" + order.getId() + "/taken", notification);

            log.info("Broadcasted order {} taken by courier {}", order.getExternalOrderNo(), courier.getId());
        } catch (Exception e) {
            log.warn("Failed to broadcast order taken notification: {}", e.getMessage());
        }
    }

    private void publishCourierAssignedEvent(Order order, Courier courier) {
        CourierAssignedEvent event = new CourierAssignedEvent(
                order.getId(),
                order.getExternalOrderNo(),
                courier.getId(),
                order.getRestaurant().getId(),
                order.getRestaurant().getFullAddress(),
                order.getDeliveryAddress(),
                order.getRestaurant().getLatitude(),
                order.getRestaurant().getLongitude(),
                order.getDeliveryLatitude(),
                order.getDeliveryLongitude()
        );
        eventPublisher.publishAsync(
                RabbitMQConfig.COURIER_EXCHANGE,
                RabbitMQConfig.COURIER_ASSIGNED_KEY,
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

    private CourierDto toDto(Courier courier) {
        return CourierDto.builder()
                .id(courier.getId())
                .userId(courier.getUser().getId())
                .userName(courier.getUser().getFullName())
                .email(courier.getUser().getEmail())
                .phone(courier.getUser().getPhone())
                .status(courier.getStatus())
                .vehicleType(courier.getVehicleType())
                .vehicleNumber(courier.getVehicleNumber())
                .currentLat(courier.getCurrentLat())
                .currentLng(courier.getCurrentLng())
                .totalDeliveries(courier.getTotalDeliveries())
                .averageRating(courier.getAverageRating())
                .verified(courier.getVerified())
                .currentOrderCount(courier.getCurrentOrderCount())
                .build();
    }
}
