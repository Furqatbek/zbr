package com.fooddelivery.analytics.operations.service;

import com.fooddelivery.analytics.operations.dto.*;
import com.fooddelivery.analytics.operations.model.CourierOrderEventType;
import com.fooddelivery.analytics.operations.model.RestaurantOrderEventType;
import com.fooddelivery.analytics.operations.repository.*;
import com.fooddelivery.analytics.operations.service.impl.OperationsAnalyticsServiceImpl;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.restaurant.entity.Restaurant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OperationsAnalyticsService.
 */
@ExtendWith(MockitoExtension.class)
class OperationsAnalyticsServiceTest {

    @Mock
    private OperationsOrderRepository orderRepository;

    @Mock
    private CourierLocationEventRepository locationEventRepository;

    @Mock
    private CourierOrderEventRepository courierOrderEventRepository;

    @Mock
    private CourierAvailabilityEventRepository availabilityEventRepository;

    @Mock
    private RestaurantOrderEventRepository restaurantOrderEventRepository;

    @Mock
    private RestaurantOnlineStatusRepository onlineStatusRepository;

    @Mock
    private MenuUpdateHistoryRepository menuUpdateRepository;

    @Mock
    private EtaHistoryRepository etaHistoryRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private OperationsAnalyticsServiceImpl analyticsService;

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        startDate = LocalDate.now().minusDays(7);
        endDate = LocalDate.now();
        startTime = startDate.atStartOfDay();
        endTime = endDate.atTime(23, 59, 59);
    }

    @Nested
    @DisplayName("Order Fulfillment Metrics Tests")
    class OrderFulfillmentTests {

        @Test
        @DisplayName("Should calculate order fulfillment metrics correctly")
        void shouldCalculateOrderFulfillmentMetrics() {
            // Given
            Long orderId = 1L;
            LocalDateTime placedAt = LocalDateTime.now().minusMinutes(45);
            LocalDateTime acceptedAt = placedAt.plusMinutes(3);
            LocalDateTime readyAt = acceptedAt.plusMinutes(15);
            LocalDateTime pickedUpAt = readyAt.plusMinutes(8);
            LocalDateTime deliveredAt = pickedUpAt.plusMinutes(15);

            Order order = createMockOrder(orderId, placedAt, acceptedAt, readyAt, pickedUpAt, deliveredAt);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // When
            OrderFulfillmentMetricsDto result = analyticsService.getOrderFulfillmentMetrics(orderId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOrderId()).isEqualTo(orderId);
            assertThat(result.getTotalFulfillmentTimeMinutes()).isEqualTo(41L);
            assertThat(result.getAcceptanceTimeMinutes()).isEqualTo(3L);
            assertThat(result.getPreparationTimeMinutes()).isEqualTo(15L);
            assertThat(result.getPickupWaitTimeMinutes()).isEqualTo(8L);
            assertThat(result.getDeliveryTimeMinutes()).isEqualTo(15L);
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            // Given
            Long orderId = 999L;
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> analyticsService.getOrderFulfillmentMetrics(orderId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Order not found");
        }

        @Test
        @DisplayName("Should handle partially completed orders")
        void shouldHandlePartiallyCompletedOrders() {
            // Given
            Long orderId = 1L;
            LocalDateTime placedAt = LocalDateTime.now().minusMinutes(20);
            LocalDateTime acceptedAt = placedAt.plusMinutes(3);

            Order order = createMockOrder(orderId, placedAt, acceptedAt, null, null, null);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // When
            OrderFulfillmentMetricsDto result = analyticsService.getOrderFulfillmentMetrics(orderId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAcceptanceTimeMinutes()).isEqualTo(3L);
            assertThat(result.getPreparationTimeMinutes()).isNull();
            assertThat(result.getDeliveryTimeMinutes()).isNull();
            assertThat(result.getTotalFulfillmentTimeMinutes()).isNull();
        }

        @Test
        @DisplayName("Should handle cancelled orders")
        void shouldHandleCancelledOrders() {
            // Given
            Long orderId = 1L;
            LocalDateTime placedAt = LocalDateTime.now().minusMinutes(10);
            LocalDateTime cancelledAt = placedAt.plusMinutes(5);

            Order order = createMockOrder(orderId, placedAt, null, null, null, null);
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelledAt(cancelledAt);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // When
            OrderFulfillmentMetricsDto result = analyticsService.getOrderFulfillmentMetrics(orderId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOrderStatus()).isEqualTo("CANCELLED");
            assertThat(result.getCancelledAt()).isEqualTo(cancelledAt);
        }

        private Order createMockOrder(Long id, LocalDateTime placed, LocalDateTime accepted,
                                       LocalDateTime ready, LocalDateTime pickedUp, LocalDateTime delivered) {
            Restaurant restaurant = new Restaurant();
            restaurant.setId(1L);

            Order order = new Order();
            order.setId(id);
            order.setExternalOrderNo("ORD-" + id);
            order.setRestaurant(restaurant);
            order.setStatus(delivered != null ? OrderStatus.DELIVERED : OrderStatus.CREATED);
            order.setCreatedAt(placed);
            order.setAcceptedAt(accepted);
            order.setReadyAt(ready);
            order.setPickedUpAt(pickedUp);
            order.setDeliveredAt(delivered);
            return order;
        }
    }

    @Nested
    @DisplayName("Restaurant Performance Metrics Tests")
    class RestaurantPerformanceTests {

        @Test
        @DisplayName("Should calculate restaurant performance metrics correctly")
        void shouldCalculateRestaurantPerformanceMetrics() {
            // Given
            Long restaurantId = 1L;
            Restaurant restaurant = new Restaurant();
            restaurant.setId(restaurantId);
            restaurant.setName("Test Restaurant");

            when(entityManager.find(Restaurant.class, restaurantId)).thenReturn(restaurant);

            // Mock event counts
            when(restaurantOrderEventRepository.countByRestaurantIdAndEventType(
                    eq(restaurantId), eq(RestaurantOrderEventType.RECEIVED), any(), any())).thenReturn(100L);
            when(restaurantOrderEventRepository.countByRestaurantIdAndEventType(
                    eq(restaurantId), eq(RestaurantOrderEventType.ACCEPTED), any(), any())).thenReturn(95L);
            when(restaurantOrderEventRepository.countByRestaurantIdAndEventType(
                    eq(restaurantId), eq(RestaurantOrderEventType.REJECTED), any(), any())).thenReturn(3L);
            when(restaurantOrderEventRepository.countByRestaurantIdAndEventType(
                    eq(restaurantId), eq(RestaurantOrderEventType.READY), any(), any())).thenReturn(90L);
            when(restaurantOrderEventRepository.countByRestaurantIdAndEventType(
                    eq(restaurantId), eq(RestaurantOrderEventType.CANCELLED), any(), any())).thenReturn(2L);

            // Mock timing metrics
            when(orderRepository.getRestaurantAvgAcceptanceTime(eq(restaurantId), any(), any())).thenReturn(2.5);
            when(restaurantOrderEventRepository.getAveragePreparationTime(eq(restaurantId), any(), any())).thenReturn(18.0);
            when(restaurantOrderEventRepository.getMedianPreparationTime(eq(restaurantId), any(), any())).thenReturn(17.0);
            when(restaurantOrderEventRepository.getP90PreparationTime(eq(restaurantId), any(), any())).thenReturn(25.0);
            when(restaurantOrderEventRepository.countOrdersPreparedOnTime(eq(restaurantId), any(), any())).thenReturn(85L);

            // Mock online status
            when(onlineStatusRepository.getTotalOfflineTimeMinutes(eq(restaurantId), any(), any())).thenReturn(120L);
            when(onlineStatusRepository.countOfflineEvents(eq(restaurantId), any(), any())).thenReturn(3);

            // Mock menu updates
            when(menuUpdateRepository.countByRestaurantIdAndTimeRange(eq(restaurantId), any(), any())).thenReturn(5L);
            when(menuUpdateRepository.getMenuUpdateSummary(eq(restaurantId), any(), any()))
                    .thenReturn(new Object[]{5L, 3, 1, 2, 1});

            // Mock hourly and daily data
            when(restaurantOrderEventRepository.getOrdersByHour(eq(restaurantId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(restaurantOrderEventRepository.getDailyMetrics(eq(restaurantId), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            RestaurantPerformanceDto result = analyticsService.getRestaurantPerformanceMetrics(
                    restaurantId, startDate, endDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRestaurantId()).isEqualTo(restaurantId);
            assertThat(result.getRestaurantName()).isEqualTo("Test Restaurant");
            assertThat(result.getTotalOrdersReceived()).isEqualTo(100L);
            assertThat(result.getOrdersAccepted()).isEqualTo(95L);
            assertThat(result.getOrderAcceptanceRate()).isCloseTo(95.0, within(0.1));
            assertThat(result.getAverageAcceptanceTimeMinutes()).isEqualTo(2.5);
            assertThat(result.getAveragePreparationTimeMinutes()).isEqualTo(18.0);
            assertThat(result.getOfflineCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should throw exception when restaurant not found")
        void shouldThrowExceptionWhenRestaurantNotFound() {
            // Given
            Long restaurantId = 999L;
            when(entityManager.find(Restaurant.class, restaurantId)).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> analyticsService.getRestaurantPerformanceMetrics(
                    restaurantId, startDate, endDate))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Restaurant not found");
        }

        @Test
        @DisplayName("Should handle restaurant with no orders")
        void shouldHandleRestaurantWithNoOrders() {
            // Given
            Long restaurantId = 1L;
            Restaurant restaurant = new Restaurant();
            restaurant.setId(restaurantId);
            restaurant.setName("Empty Restaurant");

            when(entityManager.find(Restaurant.class, restaurantId)).thenReturn(restaurant);
            when(restaurantOrderEventRepository.countByRestaurantIdAndEventType(
                    eq(restaurantId), any(), any(), any())).thenReturn(0L);
            when(onlineStatusRepository.getTotalOfflineTimeMinutes(eq(restaurantId), any(), any())).thenReturn(0L);
            when(onlineStatusRepository.countOfflineEvents(eq(restaurantId), any(), any())).thenReturn(0);
            when(menuUpdateRepository.countByRestaurantIdAndTimeRange(eq(restaurantId), any(), any())).thenReturn(0L);
            when(menuUpdateRepository.getMenuUpdateSummary(eq(restaurantId), any(), any()))
                    .thenReturn(new Object[]{0L, 0, 0, 0, 0});
            when(restaurantOrderEventRepository.getOrdersByHour(eq(restaurantId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(restaurantOrderEventRepository.getDailyMetrics(eq(restaurantId), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            RestaurantPerformanceDto result = analyticsService.getRestaurantPerformanceMetrics(
                    restaurantId, startDate, endDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalOrdersReceived()).isEqualTo(0L);
            assertThat(result.getOrderAcceptanceRate()).isNull(); // No orders, so no rate
        }
    }

    @Nested
    @DisplayName("Courier Performance Metrics Tests")
    class CourierPerformanceTests {

        @Test
        @DisplayName("Should calculate courier performance metrics correctly")
        void shouldCalculateCourierPerformanceMetrics() {
            // Given
            Long courierId = 1L;
            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");

            Courier courier = new Courier();
            courier.setId(courierId);
            courier.setUser(user);
            courier.setTotalEarnings(BigDecimal.valueOf(500));
            courier.setAverageRating(BigDecimal.valueOf(4.8));
            courier.setTotalRatings(50);

            when(entityManager.find(Courier.class, courierId)).thenReturn(courier);

            // Mock event counts
            when(courierOrderEventRepository.countByCourierIdAndEventType(
                    eq(courierId), eq(CourierOrderEventType.OFFERED), any(), any())).thenReturn(100L);
            when(courierOrderEventRepository.countByCourierIdAndEventType(
                    eq(courierId), eq(CourierOrderEventType.ACCEPTED), any(), any())).thenReturn(85L);
            when(courierOrderEventRepository.countByCourierIdAndEventType(
                    eq(courierId), eq(CourierOrderEventType.REJECTED), any(), any())).thenReturn(10L);
            when(courierOrderEventRepository.countByCourierIdAndEventType(
                    eq(courierId), eq(CourierOrderEventType.TIMED_OUT), any(), any())).thenReturn(5L);
            when(courierOrderEventRepository.countByCourierIdAndEventType(
                    eq(courierId), eq(CourierOrderEventType.CANCELLED), any(), any())).thenReturn(2L);

            // Mock response time
            when(courierOrderEventRepository.getAverageResponseTime(eq(courierId), any(), any())).thenReturn(15.0);

            // Mock delivery stats
            when(orderRepository.countCourierDeliveries(eq(courierId), any(), any())).thenReturn(80L);
            when(orderRepository.getCourierDeliveryTimeStats(eq(courierId), any(), any()))
                    .thenReturn(new Object[]{12.5, 5L, 25L, 80L});

            // Mock availability
            when(availabilityEventRepository.getTotalActiveTimeMinutes(eq(courierId), any(), any())).thenReturn(2400L);
            when(availabilityEventRepository.getTotalAvailableTimeMinutes(eq(courierId), any(), any())).thenReturn(800L);

            // Mock location
            when(locationEventRepository.countByCourierIdAndTimeRange(eq(courierId), any(), any())).thenReturn(5000L);

            // Mock cancellation reasons
            when(courierOrderEventRepository.getCancellationReasons(eq(courierId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(availabilityEventRepository.getAvailabilityByHour(eq(courierId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(courierOrderEventRepository.getDailyEventCounts(eq(courierId), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            CourierPerformanceDto result = analyticsService.getCourierPerformanceMetrics(
                    courierId, startDate, endDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCourierId()).isEqualTo(courierId);
            assertThat(result.getCourierName()).isEqualTo("John Doe");
            assertThat(result.getTotalOrdersOffered()).isEqualTo(100L);
            assertThat(result.getOrdersAccepted()).isEqualTo(85L);
            assertThat(result.getAcceptanceRate()).isCloseTo(85.0, within(0.1));
            assertThat(result.getDeliveriesCompleted()).isEqualTo(80L);
            assertThat(result.getAverageDeliveryTimeMinutes()).isEqualTo(12.5);
            assertThat(result.getTotalLocationUpdates()).isEqualTo(5000L);
        }

        @Test
        @DisplayName("Should throw exception when courier not found")
        void shouldThrowExceptionWhenCourierNotFound() {
            // Given
            Long courierId = 999L;
            when(entityManager.find(Courier.class, courierId)).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> analyticsService.getCourierPerformanceMetrics(
                    courierId, startDate, endDate))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Courier not found");
        }

        @Test
        @DisplayName("Should handle courier with no activity")
        void shouldHandleCourierWithNoActivity() {
            // Given
            Long courierId = 1L;
            User user = new User();
            user.setFirstName("New");
            user.setLastName("Courier");

            Courier courier = new Courier();
            courier.setId(courierId);
            courier.setUser(user);
            courier.setTotalEarnings(BigDecimal.ZERO);
            courier.setAverageRating(BigDecimal.ZERO);
            courier.setTotalRatings(0);

            when(entityManager.find(Courier.class, courierId)).thenReturn(courier);
            when(courierOrderEventRepository.countByCourierIdAndEventType(eq(courierId), any(), any(), any())).thenReturn(0L);
            when(orderRepository.countCourierDeliveries(eq(courierId), any(), any())).thenReturn(0L);
            when(orderRepository.getCourierDeliveryTimeStats(eq(courierId), any(), any()))
                    .thenReturn(new Object[]{null, null, null, 0L});
            when(availabilityEventRepository.getTotalActiveTimeMinutes(eq(courierId), any(), any())).thenReturn(0L);
            when(availabilityEventRepository.getTotalAvailableTimeMinutes(eq(courierId), any(), any())).thenReturn(0L);
            when(locationEventRepository.countByCourierIdAndTimeRange(eq(courierId), any(), any())).thenReturn(0L);
            when(courierOrderEventRepository.getCancellationReasons(eq(courierId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(availabilityEventRepository.getAvailabilityByHour(eq(courierId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(courierOrderEventRepository.getDailyEventCounts(eq(courierId), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            CourierPerformanceDto result = analyticsService.getCourierPerformanceMetrics(
                    courierId, startDate, endDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalOrdersOffered()).isEqualTo(0L);
            assertThat(result.getDeliveriesCompleted()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Delivery Success Rate Tests")
    class DeliverySuccessRateTests {

        @Test
        @DisplayName("Should calculate delivery success rate correctly")
        void shouldCalculateDeliverySuccessRate() {
            // Given
            when(orderRepository.countTotalOrders(any(), any())).thenReturn(1000L);
            when(orderRepository.countSuccessfulDeliveries(any(), any())).thenReturn(940L);
            when(orderRepository.countOrdersWithMajorDelays(any(), any())).thenReturn(30L);
            when(orderRepository.countOnTimeDeliveries(any(), any())).thenReturn(850L);
            when(orderRepository.countCancelledByReasonPattern(eq("%customer%"), any(), any())).thenReturn(25L);
            when(orderRepository.countCancelledByReasonPattern(eq("%restaurant%"), any(), any())).thenReturn(20L);
            when(orderRepository.countCancelledByReasonPattern(eq("%courier%"), any(), any())).thenReturn(15L);
            when(orderRepository.getCancellationReasons(any(), any())).thenReturn(Collections.emptyList());
            when(orderRepository.getSuccessRateByHour(any(), any())).thenReturn(Collections.emptyList());
            when(orderRepository.getSuccessRateByDayOfWeek(any(), any())).thenReturn(Collections.emptyList());
            when(orderRepository.getSuccessRateByRestaurant(any(), any())).thenReturn(Collections.emptyList());
            when(orderRepository.getDailySuccessMetrics(any(), any())).thenReturn(Collections.emptyList());

            // When
            DeliverySuccessRateDto result = analyticsService.getDeliverySuccessRate(startDate, endDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalOrders()).isEqualTo(1000L);
            assertThat(result.getSuccessfulDeliveries()).isEqualTo(940L);
            assertThat(result.getSuccessRate()).isCloseTo(94.0, within(0.1));
            assertThat(result.getOnTimeDeliveries()).isEqualTo(850L);
            assertThat(result.getOnTimeRate()).isCloseTo(85.0, within(0.1));
        }

        @Test
        @DisplayName("Should handle zero orders")
        void shouldHandleZeroOrders() {
            // Given
            when(orderRepository.countTotalOrders(any(), any())).thenReturn(0L);
            when(orderRepository.countSuccessfulDeliveries(any(), any())).thenReturn(0L);
            when(orderRepository.countOrdersWithMajorDelays(any(), any())).thenReturn(0L);
            when(orderRepository.countOnTimeDeliveries(any(), any())).thenReturn(0L);
            when(orderRepository.countCancelledByReasonPattern(any(), any(), any())).thenReturn(0L);
            when(orderRepository.getCancellationReasons(any(), any())).thenReturn(Collections.emptyList());
            when(orderRepository.getSuccessRateByHour(any(), any())).thenReturn(Collections.emptyList());
            when(orderRepository.getSuccessRateByDayOfWeek(any(), any())).thenReturn(Collections.emptyList());
            when(orderRepository.getSuccessRateByRestaurant(any(), any())).thenReturn(Collections.emptyList());
            when(orderRepository.getDailySuccessMetrics(any(), any())).thenReturn(Collections.emptyList());

            // When
            DeliverySuccessRateDto result = analyticsService.getDeliverySuccessRate(startDate, endDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalOrders()).isEqualTo(0L);
            assertThat(result.getSuccessRate()).isNull(); // Cannot calculate rate with 0 orders
        }
    }

    @Nested
    @DisplayName("ETA Accuracy Metrics Tests")
    class EtaAccuracyTests {

        @Test
        @DisplayName("Should calculate ETA accuracy metrics correctly")
        void shouldCalculateEtaAccuracyMetrics() {
            // Given
            when(etaHistoryRepository.getOverallEtaStats(any(), any()))
                    .thenReturn(new Object[]{2.5, 5.3, 1000L});
            when(etaHistoryRepository.getMedianAbsoluteError(any(), any())).thenReturn(4.5);
            when(etaHistoryRepository.getP90AbsoluteError(any(), any())).thenReturn(12.0);
            when(etaHistoryRepository.countWithinMinutes(eq(5), any(), any())).thenReturn(720L);
            when(etaHistoryRepository.countWithinMinutes(eq(10), any(), any())).thenReturn(850L);
            when(etaHistoryRepository.countWithinMinutes(eq(15), any(), any())).thenReturn(920L);
            when(etaHistoryRepository.getOverUnderEstimationStats(any(), any()))
                    .thenReturn(new Object[]{350L, 650L, 3.2, 4.1});
            when(etaHistoryRepository.getErrorByHour(any(), any())).thenReturn(Collections.emptyList());
            when(etaHistoryRepository.getErrorByDayOfWeek(any(), any())).thenReturn(Collections.emptyList());
            when(etaHistoryRepository.getPhaseAccuracy(any(), any()))
                    .thenReturn(new Object[]{15.0, 16.0, 5.0, 6.0, 10.0, 11.0});
            when(etaHistoryRepository.getAccuracyByRestaurant(any(), any())).thenReturn(Collections.emptyList());
            when(etaHistoryRepository.getDailyMetrics(any(), any())).thenReturn(Collections.emptyList());

            // When
            EtaAccuracyDto result = analyticsService.getEtaAccuracyMetrics(startDate, endDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalDeliveries()).isEqualTo(1000L);
            assertThat(result.getAverageEtaErrorMinutes()).isEqualTo(2.5);
            assertThat(result.getAverageAbsoluteErrorMinutes()).isEqualTo(5.3);
            assertThat(result.getWithin5Minutes()).isEqualTo(720L);
            assertThat(result.getAccuracyRate5Min()).isCloseTo(72.0, within(0.1));
            assertThat(result.getOverEstimationCount()).isEqualTo(350L);
            assertThat(result.getUnderEstimationCount()).isEqualTo(650L);
        }

        @Test
        @DisplayName("Should handle no ETA data")
        void shouldHandleNoEtaData() {
            // Given
            when(etaHistoryRepository.getOverallEtaStats(any(), any()))
                    .thenReturn(new Object[]{null, null, null});
            when(etaHistoryRepository.getMedianAbsoluteError(any(), any())).thenReturn(null);
            when(etaHistoryRepository.getP90AbsoluteError(any(), any())).thenReturn(null);
            when(etaHistoryRepository.countWithinMinutes(anyInt(), any(), any())).thenReturn(0L);
            when(etaHistoryRepository.getOverUnderEstimationStats(any(), any()))
                    .thenReturn(new Object[]{0L, 0L, null, null});
            when(etaHistoryRepository.getErrorByHour(any(), any())).thenReturn(Collections.emptyList());
            when(etaHistoryRepository.getErrorByDayOfWeek(any(), any())).thenReturn(Collections.emptyList());
            when(etaHistoryRepository.getPhaseAccuracy(any(), any()))
                    .thenReturn(new Object[]{null, null, null, null, null, null});
            when(etaHistoryRepository.getAccuracyByRestaurant(any(), any())).thenReturn(Collections.emptyList());
            when(etaHistoryRepository.getDailyMetrics(any(), any())).thenReturn(Collections.emptyList());

            // When
            EtaAccuracyDto result = analyticsService.getEtaAccuracyMetrics(startDate, endDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getWithin5Minutes()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Operations Summary Tests")
    class OperationsSummaryTests {

        @Test
        @DisplayName("Should calculate operations summary correctly")
        void shouldCalculateOperationsSummary() {
            // Given
            when(orderRepository.getAverageAcceptanceTimeMinutes(any(), any())).thenReturn(2.5);
            when(orderRepository.getAveragePreparationTimeMinutes(any(), any())).thenReturn(18.0);
            when(orderRepository.getAverageDeliveryTimeMinutes(any(), any())).thenReturn(12.0);
            when(orderRepository.getAverageTotalFulfillmentTimeMinutes(any(), any())).thenReturn(35.0);
            when(orderRepository.countTotalOrders(any(), any())).thenReturn(1000L);
            when(orderRepository.countSuccessfulDeliveries(any(), any())).thenReturn(940L);
            when(orderRepository.countOnTimeDeliveries(any(), any())).thenReturn(850L);
            when(etaHistoryRepository.getOverallEtaStats(any(), any()))
                    .thenReturn(new Object[]{2.5, 5.3, 940L});
            when(etaHistoryRepository.countWithinMinutes(eq(5), any(), any())).thenReturn(680L);
            when(availabilityEventRepository.countActiveCouriers(any(), any())).thenReturn(50L);
            when(availabilityEventRepository.getAverageActiveHoursPerDay(any(), any())).thenReturn(8.5);

            // When
            OperationsSummaryDto result = analyticsService.getOperationsSummary(startDate, endDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAvgAcceptanceTimeMinutes()).isEqualTo(2.5);
            assertThat(result.getAvgPreparationTimeMinutes()).isEqualTo(18.0);
            assertThat(result.getAvgDeliveryTimeMinutes()).isEqualTo(12.0);
            assertThat(result.getAvgFulfillmentTimeMinutes()).isEqualTo(35.0);
            assertThat(result.getTotalOrders()).isEqualTo(1000L);
            assertThat(result.getTotalDeliveriesCompleted()).isEqualTo(940L);
            assertThat(result.getDeliverySuccessRate()).isCloseTo(94.0, within(0.1));
            assertThat(result.getActiveCouriers()).isEqualTo(50L);
        }
    }

    @Nested
    @DisplayName("Cache Management Tests")
    class CacheManagementTests {

        @Test
        @DisplayName("Should evict all caches")
        void shouldEvictAllCaches() {
            // When
            analyticsService.evictAllCaches();

            // Then - no exception thrown, method completes
            // Cache eviction is handled by Spring annotation
        }

        @Test
        @DisplayName("Should evict specific cache")
        void shouldEvictSpecificCache() {
            // Given
            org.springframework.cache.Cache mockCache = mock(org.springframework.cache.Cache.class);
            when(cacheManager.getCache("ops:test")).thenReturn(mockCache);

            // When
            analyticsService.evictCache("ops:test");

            // Then
            verify(mockCache).clear();
        }

        @Test
        @DisplayName("Should handle non-existent cache gracefully")
        void shouldHandleNonExistentCache() {
            // Given
            when(cacheManager.getCache("nonexistent")).thenReturn(null);

            // When/Then - no exception thrown
            analyticsService.evictCache("nonexistent");
        }
    }
}
