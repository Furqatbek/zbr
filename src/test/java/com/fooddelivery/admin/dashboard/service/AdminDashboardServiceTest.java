package com.fooddelivery.admin.dashboard.service;

import com.fooddelivery.admin.dashboard.collector.*;
import com.fooddelivery.admin.dashboard.dto.*;
import com.fooddelivery.admin.dashboard.model.DashboardRefreshLog;
import com.fooddelivery.admin.dashboard.repository.DashboardRefreshLogRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Dashboard Service Tests")
class AdminDashboardServiceTest {

    @Mock
    private OverviewCollector overviewCollector;

    @Mock
    private OrdersCollector ordersCollector;

    @Mock
    private RestaurantCollector restaurantCollector;

    @Mock
    private CourierCollector courierCollector;

    @Mock
    private FinanceCollector financeCollector;

    @Mock
    private SupportCollector supportCollector;

    @Mock
    private DashboardRefreshLogRepository refreshLogRepository;

    @InjectMocks
    private AdminDashboardServiceImpl dashboardService;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private DashboardFilterRequest filter;

    @BeforeEach
    void setUp() {
        startDate = LocalDate.now().atStartOfDay();
        endDate = LocalDateTime.now();
        filter = DashboardFilterRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .page(0)
                .pageSize(50)
                .build();
    }

    @Nested
    @DisplayName("Overview Tests")
    class OverviewTests {

        @Test
        @DisplayName("Should return dashboard overview with all metrics")
        void shouldReturnDashboardOverview() {
            // Arrange
            DashboardOverviewDto expectedOverview = createMockOverview();
            when(overviewCollector.collectOverview(any())).thenReturn(expectedOverview);
            when(refreshLogRepository.save(any())).thenReturn(new DashboardRefreshLog());

            // Act
            DashboardOverviewDto result = dashboardService.getOverview(startDate, endDate);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getOrdersToday()).isEqualTo(150L);
            assertThat(result.getRevenueToday()).isEqualByComparingTo(BigDecimal.valueOf(15000));
            assertThat(result.getActiveCouriers()).isEqualTo(25L);
            assertThat(result.getActiveRestaurants()).isEqualTo(50L);

            verify(overviewCollector).collectOverview(any());
            verify(refreshLogRepository).save(any());
        }

        @Test
        @DisplayName("Should include system status in overview")
        void shouldIncludeSystemStatus() {
            // Arrange
            DashboardOverviewDto overview = createMockOverview();
            when(overviewCollector.collectOverview(any())).thenReturn(overview);
            when(refreshLogRepository.save(any())).thenReturn(new DashboardRefreshLog());

            // Act
            DashboardOverviewDto result = dashboardService.getOverview(startDate, endDate);

            // Assert
            assertThat(result.getSystemStatus()).isNotNull();
            assertThat(result.getSystemStatus().getOverallStatus()).isEqualTo("HEALTHY");
        }
    }

    @Nested
    @DisplayName("Active Orders Tests")
    class ActiveOrdersTests {

        @Test
        @DisplayName("Should return active orders list")
        void shouldReturnActiveOrders() {
            // Arrange
            ActiveOrdersDto expectedOrders = createMockActiveOrders();
            when(ordersCollector.collectActiveOrders(any())).thenReturn(expectedOrders);
            when(refreshLogRepository.save(any())).thenReturn(new DashboardRefreshLog());

            // Act
            ActiveOrdersDto result = dashboardService.getActiveOrders(filter);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalActiveOrders()).isEqualTo(45L);
            assertThat(result.getOrders()).hasSize(3);

            verify(ordersCollector).collectActiveOrders(filter);
        }

        @Test
        @DisplayName("Should include status breakdown")
        void shouldIncludeStatusBreakdown() {
            // Arrange
            ActiveOrdersDto activeOrders = createMockActiveOrders();
            when(ordersCollector.collectActiveOrders(any())).thenReturn(activeOrders);
            when(refreshLogRepository.save(any())).thenReturn(new DashboardRefreshLog());

            // Act
            ActiveOrdersDto result = dashboardService.getActiveOrders(filter);

            // Assert
            assertThat(result.getPendingOrders()).isNotNull();
            assertThat(result.getPreparingOrders()).isNotNull();
            assertThat(result.getInTransitOrders()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Stuck Orders Tests")
    class StuckOrdersTests {

        @Test
        @DisplayName("Should return stuck orders prioritized by urgency")
        void shouldReturnStuckOrdersByPriority() {
            // Arrange
            StuckOrdersDto expectedStuck = createMockStuckOrders();
            when(ordersCollector.collectStuckOrders(any())).thenReturn(expectedStuck);
            when(refreshLogRepository.save(any())).thenReturn(new DashboardRefreshLog());

            // Act
            StuckOrdersDto result = dashboardService.getStuckOrders(filter);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalStuckOrders()).isEqualTo(5L);
            assertThat(result.getStuckPending()).isNotNull();

            verify(ordersCollector).collectStuckOrders(filter);
        }
    }

    @Nested
    @DisplayName("Restaurant Metrics Tests")
    class RestaurantMetricsTests {

        @Test
        @DisplayName("Should return restaurant metrics with performance data")
        void shouldReturnRestaurantMetrics() {
            // Arrange
            RestaurantMetricsDto expectedMetrics = createMockRestaurantMetrics();
            when(restaurantCollector.collectRestaurantMetrics(any())).thenReturn(expectedMetrics);
            when(refreshLogRepository.save(any())).thenReturn(new DashboardRefreshLog());

            // Act
            RestaurantMetricsDto result = dashboardService.getRestaurantMetrics(filter);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalRestaurants()).isEqualTo(100L);
            assertThat(result.getOnlineRestaurants()).isEqualTo(75L);
            assertThat(result.getOnlinePercentage()).isEqualTo(75.0);

            verify(restaurantCollector).collectRestaurantMetrics(filter);
        }
    }

    @Nested
    @DisplayName("Courier Metrics Tests")
    class CourierMetricsTests {

        @Test
        @DisplayName("Should return courier metrics with locations")
        void shouldReturnCourierMetrics() {
            // Arrange
            CourierMetricsDto expectedMetrics = createMockCourierMetrics();
            when(courierCollector.collectCourierMetrics(any())).thenReturn(expectedMetrics);
            when(refreshLogRepository.save(any())).thenReturn(new DashboardRefreshLog());

            // Act
            CourierMetricsDto result = dashboardService.getCourierMetrics(filter);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalCouriers()).isEqualTo(50L);
            assertThat(result.getActiveCouriers()).isEqualTo(30L);
            assertThat(result.getUtilizationRate()).isGreaterThan(0);

            verify(courierCollector).collectCourierMetrics(filter);
        }
    }

    @Nested
    @DisplayName("Finance Metrics Tests")
    class FinanceMetricsTests {

        @Test
        @DisplayName("Should return finance metrics with GMV and revenue")
        void shouldReturnFinanceMetrics() {
            // Arrange
            FinanceMetricsDto expectedMetrics = createMockFinanceMetrics();
            when(financeCollector.collectFinanceMetrics(any())).thenReturn(expectedMetrics);
            when(refreshLogRepository.save(any())).thenReturn(new DashboardRefreshLog());

            // Act
            FinanceMetricsDto result = dashboardService.getFinanceMetrics(filter);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getGmv()).isEqualByComparingTo(BigDecimal.valueOf(100000));
            assertThat(result.getCommissionRevenue()).isEqualByComparingTo(BigDecimal.valueOf(15000));
            assertThat(result.getNetRevenue()).isGreaterThan(BigDecimal.ZERO);

            verify(financeCollector).collectFinanceMetrics(filter);
        }
    }

    @Nested
    @DisplayName("Support Metrics Tests")
    class SupportMetricsTests {

        @Test
        @DisplayName("Should return support metrics with ticket data")
        void shouldReturnSupportMetrics() {
            // Arrange
            SupportMetricsDto expectedMetrics = createMockSupportMetrics();
            when(supportCollector.collectSupportMetrics(any())).thenReturn(expectedMetrics);
            when(refreshLogRepository.save(any())).thenReturn(new DashboardRefreshLog());

            // Act
            SupportMetricsDto result = dashboardService.getSupportMetrics(filter);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalTickets()).isEqualTo(100L);
            assertThat(result.getOpenTickets()).isEqualTo(25L);
            assertThat(result.getResolutionRate()).isGreaterThan(0);

            verify(supportCollector).collectSupportMetrics(filter);
        }
    }

    @Nested
    @DisplayName("Cache Refresh Tests")
    class CacheRefreshTests {

        @Test
        @DisplayName("Should refresh all caches")
        void shouldRefreshAllCaches() {
            // Arrange
            when(refreshLogRepository.save(any())).thenReturn(new DashboardRefreshLog());

            // Act
            dashboardService.refreshCache();

            // Assert
            verify(refreshLogRepository).save(any());
        }

        @Test
        @DisplayName("Should refresh specific cache")
        void shouldRefreshSpecificCache() {
            // Arrange
            when(refreshLogRepository.save(any())).thenReturn(new DashboardRefreshLog());

            // Act
            dashboardService.refreshCache("overview");

            // Assert
            verify(refreshLogRepository).save(any());
        }
    }

    // Helper methods for creating mock data

    private DashboardOverviewDto createMockOverview() {
        return DashboardOverviewDto.builder()
                .ordersToday(150L)
                .revenueToday(BigDecimal.valueOf(15000))
                .activeCouriers(25L)
                .activeRestaurants(50L)
                .avgDeliveryTimeToday(32.5)
                .systemStatus(DashboardOverviewDto.SystemStatusDto.builder()
                        .overallStatus("HEALTHY")
                        .apiLatencyP50(150)
                        .dbLoad(45)
                        .build())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private ActiveOrdersDto createMockActiveOrders() {
        return ActiveOrdersDto.builder()
                .totalActiveOrders(45L)
                .orders(List.of(
                        ActiveOrdersDto.ActiveOrderItemDto.builder()
                                .orderId(1L)
                                .externalOrderNo("ORD-001")
                                .status("PENDING")
                                .build(),
                        ActiveOrdersDto.ActiveOrderItemDto.builder()
                                .orderId(2L)
                                .externalOrderNo("ORD-002")
                                .status("PREPARING")
                                .build(),
                        ActiveOrdersDto.ActiveOrderItemDto.builder()
                                .orderId(3L)
                                .externalOrderNo("ORD-003")
                                .status("IN_TRANSIT")
                                .build()
                ))
                .pendingOrders(15L)
                .preparingOrders(20L)
                .inTransitOrders(10L)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private StuckOrdersDto createMockStuckOrders() {
        return StuckOrdersDto.builder()
                .totalStuckOrders(5L)
                .stuckPending(2L)
                .stuckAccepted(1L)
                .stuckPreparing(1L)
                .stuckReady(1L)
                .orders(List.of())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private RestaurantMetricsDto createMockRestaurantMetrics() {
        return RestaurantMetricsDto.builder()
                .totalRestaurants(100L)
                .onlineRestaurants(75L)
                .offlineRestaurants(25L)
                .onlinePercentage(75.0)
                .restaurants(List.of())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private CourierMetricsDto createMockCourierMetrics() {
        return CourierMetricsDto.builder()
                .totalCouriers(50L)
                .activeCouriers(30L)
                .onDeliveryCouriers(20L)
                .availableCouriers(10L)
                .utilizationRate(66.67)
                .couriers(List.of())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private FinanceMetricsDto createMockFinanceMetrics() {
        return FinanceMetricsDto.builder()
                .gmv(BigDecimal.valueOf(100000))
                .commissionRevenue(BigDecimal.valueOf(15000))
                .deliveryFeeRevenue(BigDecimal.valueOf(5000))
                .totalRevenue(BigDecimal.valueOf(20000))
                .netRevenue(BigDecimal.valueOf(18000))
                .restaurantPayouts(FinanceMetricsDto.PayoutSummaryDto.builder()
                        .totalRestaurantPayouts(BigDecimal.valueOf(85000))
                        .pendingPayouts(BigDecimal.valueOf(5000))
                        .build())
                .courierPayouts(FinanceMetricsDto.PayoutSummaryDto.builder()
                        .totalCourierPayouts(BigDecimal.valueOf(10000))
                        .pendingPayouts(BigDecimal.valueOf(1000))
                        .build())
                .discountsUsed(BigDecimal.valueOf(1500))
                .refundsPaid(BigDecimal.valueOf(500))
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private SupportMetricsDto createMockSupportMetrics() {
        return SupportMetricsDto.builder()
                .totalTickets(100L)
                .openTickets(25L)
                .inProgressTickets(30L)
                .resolvedTickets(35L)
                .closedTickets(10L)
                .resolutionRate(45.0)
                .avgResolutionTimeMinutes(45.5)
                .tickets(List.of())
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
