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
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.event.CourierAssignedEvent;
import com.fooddelivery.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final UserService userService;
    private final EventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;

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
        Courier courier = courierRepository.findById(id)
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
        Courier courier = courierRepository.findById(courierId)
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
     */
    @Transactional
    public void updateLocation(Long courierId, BigDecimal lat, BigDecimal lng) {
        Courier courier = courierRepository.findById(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "id", courierId));

        courier.updateLocation(lat, lng);
        courierRepository.save(courier);

        // Broadcast location to interested parties
        broadcastLocation(courier);
    }

    /**
     * Find available couriers near a location.
     */
    @Transactional(readOnly = true)
    public List<CourierDto> findAvailableCouriers(BigDecimal lat, BigDecimal lng, double radiusKm) {
        List<Courier> couriers = courierRepository.findNearbyCouriers(lat, lng, radiusKm);
        return couriers.stream().map(this::toDto).toList();
    }

    /**
     * Accept an order assignment.
     */
    @Transactional
    @Auditable(action = "ACCEPT_ORDER", entityType = "Courier")
    public CourierDto acceptOrder(Long courierId, Long orderId) {
        Courier courier = courierRepository.findById(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "id", courierId));

        if (!courier.isAvailable()) {
            throw new BusinessException("Courier is not available to accept orders");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getCourier() != null) {
            throw new BusinessException("Order already has a courier assigned");
        }

        if (order.getStatus() != OrderStatus.READY) {
            throw new BusinessException("Order must be ready before courier assignment");
        }

        // Assign courier to order
        order.setCourier(courier);
        order.updateStatus(OrderStatus.PICKED_UP);
        orderRepository.save(order);

        // Update courier
        courier.assignOrder();
        courier = courierRepository.save(courier);

        log.info("Courier {} accepted order {}", courierId, orderId);

        // Publish event
        publishCourierAssignedEvent(order, courier);

        return toDto(courier);
    }

    /**
     * Complete a delivery.
     */
    @Transactional
    @Auditable(action = "COMPLETE_DELIVERY", entityType = "Courier")
    public CourierDto completeDelivery(Long courierId, Long orderId) {
        Courier courier = courierRepository.findById(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "id", courierId));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getCourier().getId().equals(courierId)) {
            throw new BusinessException("This order is not assigned to you");
        }

        order.updateStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

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
        Courier courier = courierRepository.findById(courierId)
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
        Page<Courier> couriers = courierRepository.findAll(pageable);
        return PagedResponse.from(couriers, couriers.getContent().stream()
                .map(this::toDto)
                .toList());
    }

    /**
     * Get pending couriers awaiting approval.
     */
    @Transactional(readOnly = true)
    public PagedResponse<CourierDto> getPendingCouriers(Pageable pageable) {
        Page<Courier> couriers = courierRepository.findPendingApproval(pageable);
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
        Courier courier = courierRepository.findById(courierId)
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
        Courier courier = courierRepository.findById(courierId)
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
        Courier courier = courierRepository.findById(courierId)
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
        Courier courier = courierRepository.findById(courierId)
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
