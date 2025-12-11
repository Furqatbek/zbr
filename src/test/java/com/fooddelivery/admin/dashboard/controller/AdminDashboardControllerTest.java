package com.fooddelivery.admin.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.admin.dashboard.dto.*;
import com.fooddelivery.admin.dashboard.service.AdminDashboardService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminDashboardController.class)
@DisplayName("Admin Dashboard Controller Tests")
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminDashboardService dashboardService;

    private static final String BASE_PATH = "/api/v1/admin/dashboard";

    @Nested
    @DisplayName("Overview Endpoint Tests")
    class OverviewEndpointTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return overview for admin user")
        void shouldReturnOverviewForAdmin() throws Exception {
            // Arrange
            DashboardOverviewDto overview = createMockOverview();
            when(dashboardService.getOverview(any(), any())).thenReturn(overview);

            // Act & Assert
            mockMvc.perform(get(BASE_PATH + "/overview"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ordersToday").value(150))
                    .andExpect(jsonPath("$.revenueToday").value(15000))
                    .andExpect(jsonPath("$.activeCouriers").value(25))
                    .andExpect(jsonPath("$.systemStatus.status").value("HEALTHY"));
        }

        @Test
        @WithMockUser(roles = "OPERATIONS_MANAGER")
        @DisplayName("Should return overview for operations manager")
        void shouldReturnOverviewForOperationsManager() throws Exception {
            // Arrange
            when(dashboardService.getOverview(any(), any())).thenReturn(createMockOverview());

            // Act & Assert
            mockMvc.perform(get(BASE_PATH + "/overview"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "SUPPORT_AGENT")
        @DisplayName("Should deny access for unauthorized role")
        void shouldDenyAccessForUnauthorizedRole() throws Exception {
            mockMvc.perform(get(BASE_PATH + "/overview"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticated() throws Exception {
            mockMvc.perform(get(BASE_PATH + "/overview"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Active Orders Endpoint Tests")
    class ActiveOrdersEndpointTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return active orders via GET")
        void shouldReturnActiveOrdersViaGet() throws Exception {
            // Arrange
            ActiveOrdersDto activeOrders = createMockActiveOrders();
            when(dashboardService.getActiveOrders(any())).thenReturn(activeOrders);

            // Act & Assert
            mockMvc.perform(get(BASE_PATH + "/orders/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalActiveOrders").value(45))
                    .andExpect(jsonPath("$.orders").isArray());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return active orders via POST with filter")
        void shouldReturnActiveOrdersViaPost() throws Exception {
            // Arrange
            ActiveOrdersDto activeOrders = createMockActiveOrders();
            when(dashboardService.getActiveOrders(any())).thenReturn(activeOrders);

            DashboardFilterRequest filter = DashboardFilterRequest.builder()
                    .startDate(LocalDateTime.now().minusDays(1))
                    .endDate(LocalDateTime.now())
                    .page(0)
                    .pageSize(50)
                    .build();

            // Act & Assert
            mockMvc.perform(post(BASE_PATH + "/orders/active")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(filter)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalActiveOrders").value(45));
        }
    }

    @Nested
    @DisplayName("Stuck Orders Endpoint Tests")
    class StuckOrdersEndpointTests {

        @Test
        @WithMockUser(roles = "OPERATIONS_MANAGER")
        @DisplayName("Should return stuck orders")
        void shouldReturnStuckOrders() throws Exception {
            // Arrange
            StuckOrdersDto stuckOrders = createMockStuckOrders();
            when(dashboardService.getStuckOrders(any())).thenReturn(stuckOrders);

            // Act & Assert
            mockMvc.perform(get(BASE_PATH + "/orders/stuck"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalStuckOrders").value(5))
                    .andExpect(jsonPath("$.criticalCount").value(2));
        }
    }

    @Nested
    @DisplayName("Restaurant Metrics Endpoint Tests")
    class RestaurantMetricsEndpointTests {

        @Test
        @WithMockUser(roles = "RESTAURANT_MANAGER")
        @DisplayName("Should return restaurant metrics")
        void shouldReturnRestaurantMetrics() throws Exception {
            // Arrange
            RestaurantMetricsDto metrics = createMockRestaurantMetrics();
            when(dashboardService.getRestaurantMetrics(any())).thenReturn(metrics);

            // Act & Assert
            mockMvc.perform(get(BASE_PATH + "/restaurants"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalRestaurants").value(100))
                    .andExpect(jsonPath("$.onlineRestaurants").value(75))
                    .andExpect(jsonPath("$.onlinePercentage").value(75.0));
        }
    }

    @Nested
    @DisplayName("Courier Metrics Endpoint Tests")
    class CourierMetricsEndpointTests {

        @Test
        @WithMockUser(roles = "FLEET_MANAGER")
        @DisplayName("Should return courier metrics")
        void shouldReturnCourierMetrics() throws Exception {
            // Arrange
            CourierMetricsDto metrics = createMockCourierMetrics();
            when(dashboardService.getCourierMetrics(any())).thenReturn(metrics);

            // Act & Assert
            mockMvc.perform(get(BASE_PATH + "/couriers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCouriers").value(50))
                    .andExpect(jsonPath("$.activeCouriers").value(30));
        }
    }

    @Nested
    @DisplayName("Finance Metrics Endpoint Tests")
    class FinanceMetricsEndpointTests {

        @Test
        @WithMockUser(roles = "FINANCE_MANAGER")
        @DisplayName("Should return finance metrics")
        void shouldReturnFinanceMetrics() throws Exception {
            // Arrange
            FinanceMetricsDto metrics = createMockFinanceMetrics();
            when(dashboardService.getFinanceMetrics(any())).thenReturn(metrics);

            // Act & Assert
            mockMvc.perform(get(BASE_PATH + "/finance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.gmv").value(100000))
                    .andExpect(jsonPath("$.commissionRevenue").value(15000));
        }

        @Test
        @WithMockUser(roles = "SUPPORT_AGENT")
        @DisplayName("Should deny finance access for support agent")
        void shouldDenyFinanceAccessForSupportAgent() throws Exception {
            mockMvc.perform(get(BASE_PATH + "/finance"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Support Metrics Endpoint Tests")
    class SupportMetricsEndpointTests {

        @Test
        @WithMockUser(roles = "SUPPORT_AGENT")
        @DisplayName("Should return support metrics for support agent")
        void shouldReturnSupportMetrics() throws Exception {
            // Arrange
            SupportMetricsDto metrics = createMockSupportMetrics();
            when(dashboardService.getSupportMetrics(any())).thenReturn(metrics);

            // Act & Assert
            mockMvc.perform(get(BASE_PATH + "/support"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTickets").value(100))
                    .andExpect(jsonPath("$.openTickets").value(25));
        }
    }

    @Nested
    @DisplayName("Cache Refresh Endpoint Tests")
    class CacheRefreshEndpointTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should refresh all caches for admin")
        void shouldRefreshAllCachesForAdmin() throws Exception {
            mockMvc.perform(post(BASE_PATH + "/cache/refresh").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"));
        }

        @Test
        @WithMockUser(roles = "OPERATIONS_MANAGER")
        @DisplayName("Should deny cache refresh for non-admin")
        void shouldDenyCacheRefreshForNonAdmin() throws Exception {
            mockMvc.perform(post(BASE_PATH + "/cache/refresh").with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should refresh specific cache")
        void shouldRefreshSpecificCache() throws Exception {
            mockMvc.perform(post(BASE_PATH + "/cache/refresh/overview").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Cache 'overview' refreshed"));
        }
    }

    // Helper methods for creating mock data

    private DashboardOverviewDto createMockOverview() {
        return DashboardOverviewDto.builder()
                .ordersToday(150L)
                .revenueToday(BigDecimal.valueOf(15000))
                .activeCouriers(25L)
                .activeRestaurants(50L)
                .avgDeliveryTimeMinutes(32.5)
                .systemStatus(DashboardOverviewDto.SystemStatusDto.builder()
                        .status("HEALTHY")
                        .apiLatencyMs(150.0)
                        .dbLoadPercent(45.0)
                        .build())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private ActiveOrdersDto createMockActiveOrders() {
        return ActiveOrdersDto.builder()
                .totalActiveOrders(45L)
                .orders(List.of())
                .statusBreakdown(Map.of("PENDING", 15L, "PREPARING", 20L, "IN_TRANSIT", 10L))
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private StuckOrdersDto createMockStuckOrders() {
        return StuckOrdersDto.builder()
                .totalStuckOrders(5L)
                .criticalCount(2L)
                .highCount(2L)
                .mediumCount(1L)
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
                .tickets(List.of())
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
