package com.fooddelivery.admin.dashboard.collector;

import com.fooddelivery.common.time.BusinessTime;
import com.fooddelivery.admin.dashboard.dto.*;
import com.fooddelivery.admin.dashboard.repository.DashboardOrderRepository;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Collector for order-related dashboard metrics.
 * Handles active, stuck, rejected, and cancelled orders.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrdersCollector {

    private final DashboardOrderRepository orderRepository;

    private static final List<OrderStatus> ACTIVE_STATUSES = Arrays.asList(
            OrderStatus.CREATED, OrderStatus.ACCEPTED, OrderStatus.PREPARING,
            OrderStatus.READY, OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT
    );

    // Default stuck thresholds in minutes
    private static final int DEFAULT_PENDING_THRESHOLD = 10;
    private static final int DEFAULT_ACCEPTED_THRESHOLD = 15;
    private static final int DEFAULT_PREPARING_THRESHOLD = 30;
    private static final int DEFAULT_READY_THRESHOLD = 15;
    private static final int DEFAULT_IN_TRANSIT_THRESHOLD = 45;

    /**
     * Collect active orders.
     */
    public ActiveOrdersDto collectActiveOrders(DashboardFilterRequest filter) {
        log.debug("Collecting active orders with filter: {}", filter);

        PageRequest pageRequest = PageRequest.of(
                filter.getPageNumber(),
                filter.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Order> ordersPage = filter.getRestaurantId() != null || filter.getCourierId() != null
                ? orderRepository.findActiveOrdersWithFilters(ACTIVE_STATUSES, filter.getRestaurantId(), filter.getCourierId(), pageRequest)
                : orderRepository.findActiveOrders(ACTIVE_STATUSES, pageRequest);

        // Count by status
        Long pendingOrders = orderRepository.countByStatusIn(Collections.singletonList(OrderStatus.CREATED));
        Long acceptedOrders = orderRepository.countByStatusIn(Collections.singletonList(OrderStatus.ACCEPTED));
        Long preparingOrders = orderRepository.countByStatusIn(Collections.singletonList(OrderStatus.PREPARING));
        Long readyOrders = orderRepository.countByStatusIn(Collections.singletonList(OrderStatus.READY));
        Long pickedUpOrders = orderRepository.countByStatusIn(Collections.singletonList(OrderStatus.PICKED_UP));
        Long inTransitOrders = orderRepository.countByStatusIn(Collections.singletonList(OrderStatus.IN_TRANSIT));

        Long totalActive = pendingOrders + acceptedOrders + preparingOrders + readyOrders + pickedUpOrders + inTransitOrders;

        // Calculate total value and average wait time
        BigDecimal totalValue = BigDecimal.ZERO;
        double totalWaitMinutes = 0;
        List<ActiveOrdersDto.ActiveOrderItemDto> orderItems = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();
        for (Order order : ordersPage.getContent()) {
            totalValue = totalValue.add(order.getTotal());
            long waitMinutes = ChronoUnit.MINUTES.between(order.getCreatedAt(), now);
            totalWaitMinutes += waitMinutes;
            orderItems.add(mapToActiveOrderItem(order, waitMinutes));
        }

        double avgWaitTime = orderItems.isEmpty() ? 0 : totalWaitMinutes / orderItems.size();

        return ActiveOrdersDto.builder()
                .totalActiveOrders(totalActive)
                .pendingOrders(pendingOrders)
                .acceptedOrders(acceptedOrders)
                .preparingOrders(preparingOrders)
                .readyOrders(readyOrders)
                .pickedUpOrders(pickedUpOrders)
                .inTransitOrders(inTransitOrders)
                .orders(orderItems)
                .currentPage(ordersPage.getNumber())
                .pageSize(ordersPage.getSize())
                .totalElements(ordersPage.getTotalElements())
                .totalPages(ordersPage.getTotalPages())
                .totalActiveOrdersValue(totalValue)
                .avgWaitTimeMinutes(avgWaitTime)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Collect stuck orders.
     */
    public StuckOrdersDto collectStuckOrders(DashboardFilterRequest filter) {
        log.debug("Collecting stuck orders with filter: {}", filter);

        LocalDateTime now = LocalDateTime.now();

        int pendingThreshold = filter.getPendingThresholdMinutes() != null
                ? filter.getPendingThresholdMinutes() : DEFAULT_PENDING_THRESHOLD;
        int acceptedThreshold = filter.getAcceptedThresholdMinutes() != null
                ? filter.getAcceptedThresholdMinutes() : DEFAULT_ACCEPTED_THRESHOLD;
        int preparingThreshold = filter.getPreparingThresholdMinutes() != null
                ? filter.getPreparingThresholdMinutes() : DEFAULT_PREPARING_THRESHOLD;
        int readyThreshold = filter.getReadyThresholdMinutes() != null
                ? filter.getReadyThresholdMinutes() : DEFAULT_READY_THRESHOLD;
        int inTransitThreshold = filter.getInTransitThresholdMinutes() != null
                ? filter.getInTransitThresholdMinutes() : DEFAULT_IN_TRANSIT_THRESHOLD;

        List<Order> stuckPending = orderRepository.findStuckPendingOrders(now, pendingThreshold);
        List<Order> stuckAccepted = orderRepository.findStuckAcceptedOrders(now, acceptedThreshold);
        List<Order> stuckPreparing = orderRepository.findStuckPreparingOrders(now, preparingThreshold);
        List<Order> stuckReady = orderRepository.findStuckReadyOrders(now, readyThreshold);
        List<Order> stuckInTransit = orderRepository.findStuckInTransitOrders(now, inTransitThreshold);

        List<StuckOrdersDto.StuckOrderItemDto> stuckOrders = new ArrayList<>();
        BigDecimal totalValueAtRisk = BigDecimal.ZERO;
        Set<Long> affectedCustomers = new HashSet<>();

        // Process each stuck category
        stuckOrders.addAll(mapStuckOrders(stuckPending, "PENDING", pendingThreshold, now));
        stuckOrders.addAll(mapStuckOrders(stuckAccepted, "ACCEPTED", acceptedThreshold, now));
        stuckOrders.addAll(mapStuckOrders(stuckPreparing, "PREPARING", preparingThreshold, now));
        stuckOrders.addAll(mapStuckOrders(stuckReady, "READY", readyThreshold, now));
        stuckOrders.addAll(mapStuckOrders(stuckInTransit, "IN_TRANSIT", inTransitThreshold, now));

        for (StuckOrdersDto.StuckOrderItemDto item : stuckOrders) {
            totalValueAtRisk = totalValueAtRisk.add(item.getTotal());
            affectedCustomers.add(item.getCustomerId());
        }

        // Sort by priority and minutes stuck
        stuckOrders.sort((a, b) -> {
            int priorityCompare = getPriorityOrder(b.getPriority()) - getPriorityOrder(a.getPriority());
            if (priorityCompare != 0) return priorityCompare;
            return Long.compare(b.getMinutesStuck(), a.getMinutesStuck());
        });

        return StuckOrdersDto.builder()
                .totalStuckOrders((long) stuckOrders.size())
                .stuckPending((long) stuckPending.size())
                .stuckAccepted((long) stuckAccepted.size())
                .stuckPreparing((long) stuckPreparing.size())
                .stuckReady((long) stuckReady.size())
                .stuckInTransit((long) stuckInTransit.size())
                .orders(stuckOrders)
                .thresholds(StuckOrdersDto.StuckThresholdsDto.builder()
                        .pendingThresholdMinutes(pendingThreshold)
                        .acceptedThresholdMinutes(acceptedThreshold)
                        .preparingThresholdMinutes(preparingThreshold)
                        .readyThresholdMinutes(readyThreshold)
                        .inTransitThresholdMinutes(inTransitThreshold)
                        .build())
                .totalValueAtRisk(totalValueAtRisk)
                .customersAffected((long) affectedCustomers.size())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Collect cancelled orders.
     */
    public CanceledOrdersDto collectCancelledOrders(DashboardFilterRequest filter) {
        log.debug("Collecting cancelled orders from {} to {}", filter.getEffectiveStartDate(), filter.getEffectiveEndDate());

        LocalDateTime startDate = filter.getEffectiveStartDate();
        LocalDateTime endDate = filter.getEffectiveEndDate();

        PageRequest pageRequest = PageRequest.of(
                filter.getPageNumber(),
                filter.getPageSize(),
                Sort.by(Sort.Direction.DESC, "cancelledAt")
        );

        Page<Order> cancelledPage = orderRepository.findCancelledOrders(startDate, endDate, pageRequest);

        // Count totals
        Long totalCancelled = orderRepository.countByStatusAndDateRange(OrderStatus.CANCELLED, startDate, endDate);
        BigDecimal totalValueCancelled = orderRepository.sumCancelledOrderValue(startDate, endDate);

        // Get cancellation reasons breakdown
        List<Object[]> reasonsData = orderRepository.countCancelledByReason(startDate, endDate);
        Map<String, Long> reasonsBreakdown = new HashMap<>();
        for (Object[] row : reasonsData) {
            String reason = row[0] != null ? row[0].toString() : "Unknown";
            Long count = ((Number) row[1]).longValue();
            reasonsBreakdown.put(reason, count);
        }

        // Get hourly distribution
        List<Object[]> hourlyData = orderRepository.cancelledOrdersByHour(startDate, endDate);
        Map<Integer, Long> cancelsByHour = new HashMap<>();
        for (Object[] row : hourlyData) {
            Integer hour = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            cancelsByHour.put(hour, count);
        }

        // Map orders to DTOs
        List<CanceledOrdersDto.CanceledOrderItemDto> orderItems = cancelledPage.getContent().stream()
                .map(this::mapToCancelledOrderItem)
                .collect(Collectors.toList());

        // Calculate totals for different periods
        LocalDateTime todayStart = BusinessTime.startOfToday();
        LocalDateTime weekStart = todayStart.minusDays(7);
        LocalDateTime monthStart = todayStart.minusDays(30);

        Long cancelledToday = orderRepository.countByStatusAndDateRange(OrderStatus.CANCELLED, todayStart, LocalDateTime.now());
        Long cancelledThisWeek = orderRepository.countByStatusAndDateRange(OrderStatus.CANCELLED, weekStart, LocalDateTime.now());
        Long cancelledThisMonth = orderRepository.countByStatusAndDateRange(OrderStatus.CANCELLED, monthStart, LocalDateTime.now());

        // Calculate cancellation rate
        Long totalOrdersInPeriod = orderRepository.countByDateRange(startDate, endDate);
        Double cancellationRate = totalOrdersInPeriod > 0
                ? (totalCancelled.doubleValue() / totalOrdersInPeriod.doubleValue()) * 100
                : 0.0;

        return CanceledOrdersDto.builder()
                .totalCanceledOrders(totalCancelled)
                .canceledToday(cancelledToday)
                .canceledThisWeek(cancelledThisWeek)
                .canceledThisMonth(cancelledThisMonth)
                .totalValueCanceled(totalValueCancelled)
                .cancellationRate(cancellationRate)
                .cancellationReasonBreakdown(reasonsBreakdown)
                .cancelsByHour(cancelsByHour)
                .orders(orderItems)
                .currentPage(cancelledPage.getNumber())
                .pageSize(cancelledPage.getSize())
                .totalElements(cancelledPage.getTotalElements())
                .totalPages(cancelledPage.getTotalPages())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== Helper Methods ====================

    private ActiveOrdersDto.ActiveOrderItemDto mapToActiveOrderItem(Order order, long waitMinutes) {
        LocalDateTime now = LocalDateTime.now();
        boolean isDelayed = isOrderDelayed(order, waitMinutes);
        boolean isStuck = isOrderStuck(order, now);

        return ActiveOrdersDto.ActiveOrderItemDto.builder()
                .orderId(order.getId())
                .externalOrderNo(order.getExternalOrderNo())
                .status(order.getStatus().name())
                .orderType(order.getOrderType() != null ? order.getOrderType().name() : null)
                .customerId(order.getConsumer() != null ? order.getConsumer().getId() : null)
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .deliveryAddress(order.getDeliveryAddress())
                .restaurantId(order.getRestaurant() != null ? order.getRestaurant().getId() : null)
                .restaurantName(order.getRestaurant() != null ? order.getRestaurant().getName() : null)
                .courierId(order.getCourier() != null ? order.getCourier().getId() : null)
                .courierName(order.getCourier() != null && order.getCourier().getUser() != null
                        ? order.getCourier().getUser().getFullName()
                        : null)
                .createdAt(order.getCreatedAt())
                .acceptedAt(order.getAcceptedAt())
                .preparingAt(order.getPreparingAt())
                .readyAt(order.getReadyAt())
                .pickedUpAt(order.getPickedUpAt())
                .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                .waitTimeMinutes(waitMinutes)
                .subtotal(order.getSubtotal())
                .deliveryFee(order.getDeliveryFee())
                .discount(order.getDiscount())
                .total(order.getTotal())
                .isDelayed(isDelayed)
                .isStuck(isStuck)
                .requiresAttention(isDelayed || isStuck)
                .attentionReason(getAttentionReason(order, isDelayed, isStuck))
                .build();
    }

    private List<StuckOrdersDto.StuckOrderItemDto> mapStuckOrders(
            List<Order> orders, String stage, int threshold, LocalDateTime now) {

        return orders.stream()
                .map(order -> {
                    LocalDateTime stageTime = getStageTime(order, stage);
                    long minutesStuck = stageTime != null
                            ? ChronoUnit.MINUTES.between(stageTime, now)
                            : ChronoUnit.MINUTES.between(order.getCreatedAt(), now);

                    String priority = determinePriority(minutesStuck, threshold);
                    String suggestedAction = getSuggestedAction(stage, minutesStuck);

                    return StuckOrdersDto.StuckOrderItemDto.builder()
                            .orderId(order.getId())
                            .externalOrderNo(order.getExternalOrderNo())
                            .status(order.getStatus().name())
                            .stuckStage(stage)
                            .restaurantId(order.getRestaurant() != null ? order.getRestaurant().getId() : null)
                            .restaurantName(order.getRestaurant() != null ? order.getRestaurant().getName() : null)
                            .courierId(order.getCourier() != null ? order.getCourier().getId() : null)
                            .courierName(order.getCourier() != null && order.getCourier().getUser() != null
                                    ? order.getCourier().getUser().getFullName() : null)
                            .customerId(order.getConsumer() != null ? order.getConsumer().getId() : null)
                            .customerName(order.getCustomerName())
                            .customerPhone(order.getCustomerPhone())
                            .deliveryAddress(order.getDeliveryAddress())
                            .createdAt(order.getCreatedAt())
                            .lastStatusChange(stageTime)
                            .minutesStuck(minutesStuck)
                            .expectedTimeMinutes((long) threshold)
                            .total(order.getTotal())
                            .priority(priority)
                            .suggestedAction(suggestedAction)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private CanceledOrdersDto.CanceledOrderItemDto mapToCancelledOrderItem(Order order) {
        long orderAgeMinutes = order.getCancelledAt() != null
                ? ChronoUnit.MINUTES.between(order.getCreatedAt(), order.getCancelledAt())
                : 0;

        return CanceledOrdersDto.CanceledOrderItemDto.builder()
                .orderId(order.getId())
                .externalOrderNo(order.getExternalOrderNo())
                .statusWhenCanceled(order.getStatus().name())
                .restaurantId(order.getRestaurant() != null ? order.getRestaurant().getId() : null)
                .restaurantName(order.getRestaurant() != null ? order.getRestaurant().getName() : null)
                .customerId(order.getConsumer() != null ? order.getConsumer().getId() : null)
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .courierId(order.getCourier() != null ? order.getCourier().getId() : null)
                .cancellationReason(order.getCancellationReason())
                .canceledAt(order.getCancelledAt())
                .orderAgeMinutes(orderAgeMinutes)
                .subtotal(order.getSubtotal())
                .total(order.getTotal())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private LocalDateTime getStageTime(Order order, String stage) {
        return switch (stage) {
            case "PENDING" -> order.getCreatedAt();
            case "ACCEPTED" -> order.getAcceptedAt();
            case "PREPARING" -> order.getPreparingAt();
            case "READY" -> order.getReadyAt();
            case "IN_TRANSIT" -> order.getPickedUpAt();
            default -> order.getCreatedAt();
        };
    }

    private boolean isOrderDelayed(Order order, long waitMinutes) {
        if (order.getEstimatedDeliveryTime() != null) {
            return LocalDateTime.now().isAfter(order.getEstimatedDeliveryTime());
        }
        return waitMinutes > 60; // Default 60 minutes threshold
    }

    private boolean isOrderStuck(Order order, LocalDateTime now) {
        long minutesSinceCreation = ChronoUnit.MINUTES.between(order.getCreatedAt(), now);

        return switch (order.getStatus()) {
            case CREATED -> minutesSinceCreation > DEFAULT_PENDING_THRESHOLD;
            case ACCEPTED -> order.getAcceptedAt() != null &&
                    ChronoUnit.MINUTES.between(order.getAcceptedAt(), now) > DEFAULT_ACCEPTED_THRESHOLD;
            case PREPARING -> order.getPreparingAt() != null &&
                    ChronoUnit.MINUTES.between(order.getPreparingAt(), now) > DEFAULT_PREPARING_THRESHOLD;
            case READY -> order.getReadyAt() != null &&
                    ChronoUnit.MINUTES.between(order.getReadyAt(), now) > DEFAULT_READY_THRESHOLD;
            case PICKED_UP, IN_TRANSIT -> order.getPickedUpAt() != null &&
                    ChronoUnit.MINUTES.between(order.getPickedUpAt(), now) > DEFAULT_IN_TRANSIT_THRESHOLD;
            default -> false;
        };
    }

    private String getAttentionReason(Order order, boolean isDelayed, boolean isStuck) {
        if (isStuck) return "Order stuck at " + order.getStatus().name() + " stage";
        if (isDelayed) return "Order delivery is delayed";
        return null;
    }

    private String determinePriority(long minutesStuck, int threshold) {
        double ratio = (double) minutesStuck / threshold;
        if (ratio >= 3) return "CRITICAL";
        if (ratio >= 2) return "HIGH";
        if (ratio >= 1.5) return "MEDIUM";
        return "LOW";
    }

    private int getPriorityOrder(String priority) {
        return switch (priority) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    private String getSuggestedAction(String stage, long minutesStuck) {
        return switch (stage) {
            case "PENDING" -> "Contact restaurant to accept order";
            case "ACCEPTED" -> "Check if restaurant has started preparation";
            case "PREPARING" -> "Verify preparation status with restaurant";
            case "READY" -> "Assign courier immediately";
            case "IN_TRANSIT" -> "Check courier location and ETA";
            default -> "Review order status";
        };
    }

    /**
     * Collect rejected orders.
     */
    public RejectedOrdersDto collectRejectedOrders(DashboardFilterRequest filter) {
        log.debug("Collecting rejected orders from {} to {}", filter.getEffectiveStartDate(), filter.getEffectiveEndDate());

        LocalDateTime startDate = filter.getEffectiveStartDate();
        LocalDateTime endDate = filter.getEffectiveEndDate();

        PageRequest pageRequest = PageRequest.of(
                filter.getPageNumber(),
                filter.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Order> rejectedPage = orderRepository.findRejectedOrders(startDate, endDate, pageRequest);

        // Count totals
        Long totalRejected = orderRepository.countRejectedOrders(startDate, endDate);
        BigDecimal totalValueLost = orderRepository.sumRejectedOrderValue(startDate, endDate);

        // Get restaurant breakdown
        List<Object[]> restaurantData = orderRepository.countRejectedByRestaurant(startDate, endDate);
        List<RejectedOrdersDto.RejectorSummaryDto> topRejectingRestaurants = restaurantData.stream()
                .limit(10)
                .map(row -> RejectedOrdersDto.RejectorSummaryDto.builder()
                        .entityId(((Number) row[0]).longValue())
                        .entityName((String) row[1])
                        .entityType("RESTAURANT")
                        .rejectionCount(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        // Calculate time-based breakdowns
        LocalDateTime todayStart = BusinessTime.startOfToday();
        LocalDateTime weekStart = todayStart.minusDays(7);
        LocalDateTime monthStart = todayStart.minusDays(30);

        Long rejectedToday = orderRepository.countRejectedOrders(todayStart, LocalDateTime.now());
        Long rejectedThisWeek = orderRepository.countRejectedOrders(weekStart, LocalDateTime.now());
        Long rejectedThisMonth = orderRepository.countRejectedOrders(monthStart, LocalDateTime.now());

        // Map orders to DTOs
        List<RejectedOrdersDto.RejectedOrderItemDto> orderItems = rejectedPage.getContent().stream()
                .map(this::mapToRejectedOrderItem)
                .collect(Collectors.toList());

        return RejectedOrdersDto.builder()
                .totalRejectedOrders(totalRejected)
                .rejectedToday(rejectedToday)
                .rejectedThisWeek(rejectedThisWeek)
                .rejectedThisMonth(rejectedThisMonth)
                .totalValueLost(totalValueLost)
                .topRejectingRestaurants(topRejectingRestaurants)
                .orders(orderItems)
                .currentPage(rejectedPage.getNumber())
                .pageSize(rejectedPage.getSize())
                .totalElements(rejectedPage.getTotalElements())
                .totalPages(rejectedPage.getTotalPages())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private RejectedOrdersDto.RejectedOrderItemDto mapToRejectedOrderItem(Order order) {
        long timeToRejection = order.getRejectedAt() != null
                ? ChronoUnit.MINUTES.between(order.getCreatedAt(), order.getRejectedAt())
                : 0;

        return RejectedOrdersDto.RejectedOrderItemDto.builder()
                .orderId(order.getId())
                .externalOrderNo(order.getExternalOrderNo())
                .restaurantId(order.getRestaurant() != null ? order.getRestaurant().getId() : null)
                .restaurantName(order.getRestaurant() != null ? order.getRestaurant().getName() : null)
                .customerId(order.getConsumer() != null ? order.getConsumer().getId() : null)
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .rejectionReason(order.getRejectionReason())
                .rejectedAt(order.getRejectedAt())
                .timeToRejectionMinutes(timeToRejection)
                .total(order.getTotal())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
