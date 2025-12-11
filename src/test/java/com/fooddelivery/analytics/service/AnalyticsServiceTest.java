package com.fooddelivery.analytics.service;

import com.fooddelivery.analytics.dto.*;
import com.fooddelivery.analytics.model.ActivityEventType;
import com.fooddelivery.analytics.repository.*;
import com.fooddelivery.analytics.service.impl.AnalyticsServiceImpl;
import com.fooddelivery.restaurant.entity.RestaurantStatus;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnalyticsServiceImpl.
 * Tests all metric calculation methods with various scenarios.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private AnalyticsOrderRepository orderRepository;

    @Mock
    private AnalyticsUserRepository userRepository;

    @Mock
    private AnalyticsRestaurantRepository restaurantRepository;

    @Mock
    private AnalyticsCourierRepository courierRepository;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @Nested
    @DisplayName("DAU/WAU/MAU Tests")
    class DauWauMauTests {

        @Test
        @DisplayName("Should return correct DAU")
        void getDailyActiveUsers_ReturnsCorrectCount() {
            // Given
            when(activityLogRepository.countDistinctActiveUsersSince(any(LocalDateTime.class)))
                    .thenReturn(150L);

            // When
            Long dau = analyticsService.getDailyActiveUsers();

            // Then
            assertThat(dau).isEqualTo(150L);
            verify(activityLogRepository).countDistinctActiveUsersSince(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should return zero DAU when no data")
        void getDailyActiveUsers_ReturnsZeroWhenNoData() {
            // Given
            when(activityLogRepository.countDistinctActiveUsersSince(any(LocalDateTime.class)))
                    .thenReturn(null);

            // When
            Long dau = analyticsService.getDailyActiveUsers();

            // Then
            assertThat(dau).isNull();
        }

        @Test
        @DisplayName("Should return correct WAU")
        void getWeeklyActiveUsers_ReturnsCorrectCount() {
            // Given
            when(activityLogRepository.countDistinctActiveUsersSince(any(LocalDateTime.class)))
                    .thenReturn(1000L);

            // When
            Long wau = analyticsService.getWeeklyActiveUsers();

            // Then
            assertThat(wau).isEqualTo(1000L);
        }

        @Test
        @DisplayName("Should return correct MAU")
        void getMonthlyActiveUsers_ReturnsCorrectCount() {
            // Given
            when(activityLogRepository.countDistinctActiveUsersSince(any(LocalDateTime.class)))
                    .thenReturn(5000L);

            // When
            Long mau = analyticsService.getMonthlyActiveUsers();

            // Then
            assertThat(mau).isEqualTo(5000L);
        }

        @Test
        @DisplayName("Should calculate correct DAU/MAU ratio")
        void getUserActivityMetrics_CalculatesCorrectRatios() {
            // Given
            when(activityLogRepository.countDistinctActiveUsersSince(any(LocalDateTime.class)))
                    .thenReturn(100L)  // DAU
                    .thenReturn(500L)  // WAU
                    .thenReturn(1000L); // MAU
            when(userRepository.countNewUsersSince(any(LocalDateTime.class)))
                    .thenReturn(10L)
                    .thenReturn(50L)
                    .thenReturn(200L);

            // When
            DauWauMauDto metrics = analyticsService.getUserActivityMetrics();

            // Then
            assertThat(metrics.getDailyActiveUsers()).isEqualTo(100L);
            assertThat(metrics.getWeeklyActiveUsers()).isEqualTo(500L);
            assertThat(metrics.getMonthlyActiveUsers()).isEqualTo(1000L);
            assertThat(metrics.getDauMauRatio()).isEqualTo(0.1); // 100/1000
            assertThat(metrics.getDauWauRatio()).isEqualTo(0.2); // 100/500
            assertThat(metrics.getWauMauRatio()).isEqualTo(0.5); // 500/1000
        }

        @Test
        @DisplayName("Should handle zero MAU for ratio calculation")
        void getUserActivityMetrics_HandlesZeroMauForRatios() {
            // Given
            when(activityLogRepository.countDistinctActiveUsersSince(any(LocalDateTime.class)))
                    .thenReturn(0L);
            when(userRepository.countNewUsersSince(any(LocalDateTime.class)))
                    .thenReturn(0L);

            // When
            DauWauMauDto metrics = analyticsService.getUserActivityMetrics();

            // Then
            assertThat(metrics.getDauMauRatio()).isNull(); // Division by zero protection
        }
    }

    @Nested
    @DisplayName("Order Volume Tests")
    class OrderVolumeTests {

        @Test
        @DisplayName("Should return correct orders per day")
        void getOrdersPerDay_ReturnsCorrectCount() {
            // Given
            when(orderRepository.countOrdersSince(any(LocalDateTime.class)))
                    .thenReturn(250L);

            // When
            Long ordersPerDay = analyticsService.getOrdersPerDay();

            // Then
            assertThat(ordersPerDay).isEqualTo(250L);
        }

        @Test
        @DisplayName("Should calculate order volume metrics with trends")
        void getOrderVolumeMetrics_ReturnsCompleteMetrics() {
            // Given
            when(orderRepository.countOrdersSince(any(LocalDateTime.class)))
                    .thenReturn(100L)  // today
                    .thenReturn(700L)  // week
                    .thenReturn(3000L); // month
            when(orderRepository.countCompletedOrdersSince(any(LocalDateTime.class)))
                    .thenReturn(90L);
            when(orderRepository.countCancelledOrdersSince(any(LocalDateTime.class)))
                    .thenReturn(5L);
            when(orderRepository.countFirstTimeOrdersSince(any(LocalDateTime.class)))
                    .thenReturn(30L);
            when(orderRepository.countRepeatOrdersSince(any(LocalDateTime.class)))
                    .thenReturn(70L);
            when(orderRepository.getOrdersPerHourBetween(any(), any()))
                    .thenReturn(new ArrayList<>());
            when(orderRepository.getDailyOrderCountsBetween(any(), any()))
                    .thenReturn(new ArrayList<>());

            // When
            OrderVolumeDto metrics = analyticsService.getOrderVolumeMetrics();

            // Then
            assertThat(metrics.getTotalOrdersToday()).isEqualTo(100L);
            assertThat(metrics.getTotalOrdersWeek()).isEqualTo(700L);
            assertThat(metrics.getTotalOrdersMonth()).isEqualTo(3000L);
            assertThat(metrics.getCompletedOrdersToday()).isEqualTo(90L);
            assertThat(metrics.getCancelledOrdersToday()).isEqualTo(5L);
            assertThat(metrics.getFirstTimeOrdersToday()).isEqualTo(30L);
            assertThat(metrics.getRepeatOrdersToday()).isEqualTo(70L);
            assertThat(metrics.getSuccessRate()).isEqualTo(90.0); // 90/100
        }

        @Test
        @DisplayName("Should handle null values gracefully")
        void getOrderVolumeMetrics_HandlesNullValues() {
            // Given
            when(orderRepository.countOrdersSince(any(LocalDateTime.class)))
                    .thenReturn(null);
            when(orderRepository.countCompletedOrdersSince(any(LocalDateTime.class)))
                    .thenReturn(null);
            when(orderRepository.countCancelledOrdersSince(any(LocalDateTime.class)))
                    .thenReturn(null);
            when(orderRepository.countFirstTimeOrdersSince(any(LocalDateTime.class)))
                    .thenReturn(null);
            when(orderRepository.countRepeatOrdersSince(any(LocalDateTime.class)))
                    .thenReturn(null);
            when(orderRepository.getOrdersPerHourBetween(any(), any()))
                    .thenReturn(new ArrayList<>());
            when(orderRepository.getDailyOrderCountsBetween(any(), any()))
                    .thenReturn(new ArrayList<>());

            // When
            OrderVolumeDto metrics = analyticsService.getOrderVolumeMetrics();

            // Then
            assertThat(metrics.getTotalOrdersToday()).isZero();
            assertThat(metrics.getCompletedOrdersToday()).isZero();
        }
    }

    @Nested
    @DisplayName("Conversion Funnel Tests")
    class ConversionFunnelTests {

        @Test
        @DisplayName("Should calculate correct conversion rate")
        void getConversionRate_ReturnsCorrectPercentage() {
            // Given
            List<Object[]> funnelData = List.of(
                    new Object[]{ActivityEventType.RESTAURANT_VIEW, 1000L},
                    new Object[]{ActivityEventType.ADD_TO_CART, 400L},
                    new Object[]{ActivityEventType.CHECKOUT_START, 200L},
                    new Object[]{ActivityEventType.PAYMENT_COMPLETED, 100L}
            );
            when(activityLogRepository.countDistinctUsersByEventTypesSince(any(), any()))
                    .thenReturn(funnelData);

            // When
            Double conversionRate = analyticsService.getConversionRate();

            // Then
            assertThat(conversionRate).isEqualTo(10.0); // 100/1000 * 100
        }

        @Test
        @DisplayName("Should return complete funnel metrics with all steps")
        void getConversionMetrics_ReturnsAllFunnelSteps() {
            // Given
            List<Object[]> funnelData = List.of(
                    new Object[]{ActivityEventType.RESTAURANT_VIEW, 1000L},
                    new Object[]{ActivityEventType.ADD_TO_CART, 400L},
                    new Object[]{ActivityEventType.CHECKOUT_START, 200L},
                    new Object[]{ActivityEventType.PAYMENT_COMPLETED, 100L}
            );
            when(activityLogRepository.countDistinctUsersByEventTypesSince(any(), any()))
                    .thenReturn(funnelData);

            // When
            ConversionRateDto metrics = analyticsService.getConversionMetrics(1);

            // Then
            assertThat(metrics.getRestaurantViews()).isEqualTo(1000L);
            assertThat(metrics.getAddToCartEvents()).isEqualTo(400L);
            assertThat(metrics.getCheckoutStartEvents()).isEqualTo(200L);
            assertThat(metrics.getPaymentCompletedEvents()).isEqualTo(100L);
            assertThat(metrics.getOverallConversionRate()).isEqualTo(10.0);
            assertThat(metrics.getViewToCartRate()).isEqualTo(40.0);
            assertThat(metrics.getCartToCheckoutRate()).isEqualTo(50.0);
            assertThat(metrics.getCheckoutToPaymentRate()).isEqualTo(50.0);
            assertThat(metrics.getFunnelSteps()).hasSize(4);
        }

        @Test
        @DisplayName("Should handle empty funnel data")
        void getConversionMetrics_HandlesEmptyData() {
            // Given
            when(activityLogRepository.countDistinctUsersByEventTypesSince(any(), any()))
                    .thenReturn(new ArrayList<>());

            // When
            ConversionRateDto metrics = analyticsService.getConversionMetrics(1);

            // Then
            assertThat(metrics.getRestaurantViews()).isZero();
            assertThat(metrics.getOverallConversionRate()).isNull();
        }
    }

    @Nested
    @DisplayName("AOV Tests")
    class AovTests {

        @Test
        @DisplayName("Should return correct AOV")
        void getAverageOrderValue_ReturnsCorrectValue() {
            // Given
            when(orderRepository.getAverageOrderValueSince(any(LocalDateTime.class)))
                    .thenReturn(new BigDecimal("45000.50"));

            // When
            BigDecimal aov = analyticsService.getAverageOrderValue();

            // Then
            assertThat(aov).isEqualByComparingTo(new BigDecimal("45000.50"));
        }

        @Test
        @DisplayName("Should return zero when no completed orders")
        void getAverageOrderValue_ReturnsZeroWhenNoOrders() {
            // Given
            when(orderRepository.getAverageOrderValueSince(any(LocalDateTime.class)))
                    .thenReturn(null);

            // When
            BigDecimal aov = analyticsService.getAverageOrderValue();

            // Then
            assertThat(aov).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return complete AOV metrics")
        void getAovMetrics_ReturnsCompleteMetrics() {
            // Given
            when(orderRepository.getAverageOrderValueSince(any(LocalDateTime.class)))
                    .thenReturn(new BigDecimal("50000"))
                    .thenReturn(new BigDecimal("48000"))
                    .thenReturn(new BigDecimal("47000"));
            when(orderRepository.getTotalRevenueSince(any(LocalDateTime.class)))
                    .thenReturn(new BigDecimal("5000000"))
                    .thenReturn(new BigDecimal("35000000"))
                    .thenReturn(new BigDecimal("140000000"));
            when(orderRepository.countCompletedOrdersSince(any(LocalDateTime.class)))
                    .thenReturn(100L)
                    .thenReturn(700L)
                    .thenReturn(3000L);
            when(orderRepository.getOrderValueStatsSince(any(LocalDateTime.class)))
                    .thenReturn(new Object[]{
                            new BigDecimal("15000"),
                            new BigDecimal("150000"),
                            new BigDecimal("50000"),
                            new BigDecimal("5000000"),
                            100L
                    });
            when(orderRepository.getMedianOrderValueSince(any(LocalDateTime.class)))
                    .thenReturn(new BigDecimal("45000"));
            when(orderRepository.getAverageItemsPerOrderSince(any(LocalDateTime.class)))
                    .thenReturn(2.5);
            when(orderRepository.getAverageDeliveryFeeSince(any(LocalDateTime.class)))
                    .thenReturn(new BigDecimal("5000"));
            when(orderRepository.getAverageTipAmountSince(any(LocalDateTime.class)))
                    .thenReturn(new BigDecimal("2000"));
            when(orderRepository.getDailyAovTrendBetween(any(), any()))
                    .thenReturn(new ArrayList<>());

            // When
            AovDto metrics = analyticsService.getAovMetrics();

            // Then
            assertThat(metrics.getAovToday()).isEqualByComparingTo(new BigDecimal("50000.00"));
            assertThat(metrics.getTotalRevenueToday()).isEqualByComparingTo(new BigDecimal("5000000"));
            assertThat(metrics.getCompletedOrdersToday()).isEqualTo(100L);
            assertThat(metrics.getAverageItemsPerOrder()).isEqualTo(2.5);
        }
    }

    @Nested
    @DisplayName("Churn Tests")
    class ChurnTests {

        @Test
        @DisplayName("Should calculate correct user churn rate")
        void getUserChurnRate_ReturnsCorrectPercentage() {
            // Given
            setupChurnMocks();

            // When
            Double churnRate = analyticsService.getUserChurnRate();

            // Then
            assertThat(churnRate).isEqualTo(20.0); // 200 churned / 1000 total
        }

        @Test
        @DisplayName("Should calculate correct restaurant churn rate")
        void getRestaurantChurnRate_ReturnsCorrectPercentage() {
            // Given
            setupChurnMocks();

            // When
            Double churnRate = analyticsService.getRestaurantChurnRate();

            // Then
            assertThat(churnRate).isEqualTo(10.0); // 10 churned / 100 total
        }

        @Test
        @DisplayName("Should calculate correct courier churn rate")
        void getCourierChurnRate_ReturnsCorrectPercentage() {
            // Given
            setupChurnMocks();

            // When
            Double churnRate = analyticsService.getCourierChurnRate();

            // Then
            assertThat(churnRate).isEqualTo(25.0); // 25 inactive / 100 total
        }

        @Test
        @DisplayName("Should return complete churn metrics")
        void getChurnMetrics_ReturnsCompleteMetrics() {
            // Given
            setupChurnMocks();

            // When
            ChurnDto metrics = analyticsService.getChurnMetrics();

            // Then
            assertThat(metrics.getTotalActiveUsers()).isEqualTo(1000L);
            assertThat(metrics.getUsersOrderedLast30Days()).isEqualTo(800L);
            assertThat(metrics.getUsersChurnedLast30Days()).isEqualTo(200L);
            assertThat(metrics.getUserChurnRate()).isEqualTo(20.0);
            assertThat(metrics.getTotalActiveRestaurants()).isEqualTo(100L);
            assertThat(metrics.getRestaurantChurnRate()).isEqualTo(10.0);
            assertThat(metrics.getCourierChurnRate()).isEqualTo(25.0);
        }

        @Test
        @DisplayName("Should handle zero total users for churn calculation")
        void getChurnMetrics_HandlesZeroTotalUsers() {
            // Given
            when(orderRepository.countTotalUsersWithOrders()).thenReturn(0L);
            when(orderRepository.countUsersWithOrdersSince(any(LocalDateTime.class))).thenReturn(0L);
            when(orderRepository.countUsersAtRiskOfChurn(any(), any())).thenReturn(0L);
            when(restaurantRepository.countByStatus(RestaurantStatus.ACTIVE)).thenReturn(0L);
            when(restaurantRepository.countRestaurantsWithOrdersSince(any())).thenReturn(0L);
            when(restaurantRepository.countRestaurantsWithNoOrdersSince(any())).thenReturn(0L);
            when(restaurantRepository.countRestaurantsAtRisk(any(), anyInt())).thenReturn(0L);
            when(courierRepository.countVerifiedCouriers()).thenReturn(0L);
            when(courierRepository.countActiveCouriersSince(any())).thenReturn(0L);
            when(courierRepository.countInactiveCouriersSince(any())).thenReturn(0L);
            when(courierRepository.countCouriersAtRisk(any(), anyInt(), anyInt())).thenReturn(0L);
            when(userRepository.getUserRetentionCohorts(any())).thenReturn(new ArrayList<>());

            // When
            ChurnDto metrics = analyticsService.getChurnMetrics();

            // Then
            assertThat(metrics.getUserChurnRate()).isNull(); // Division by zero protection
        }

        private void setupChurnMocks() {
            // User churn
            when(orderRepository.countTotalUsersWithOrders()).thenReturn(1000L);
            when(orderRepository.countUsersWithOrdersSince(any(LocalDateTime.class))).thenReturn(800L);
            when(orderRepository.countUsersAtRiskOfChurn(any(), any())).thenReturn(50L);

            // Restaurant churn
            when(restaurantRepository.countByStatus(RestaurantStatus.ACTIVE)).thenReturn(100L);
            when(restaurantRepository.countRestaurantsWithOrdersSince(any())).thenReturn(90L);
            when(restaurantRepository.countRestaurantsWithNoOrdersSince(any())).thenReturn(10L);
            when(restaurantRepository.countRestaurantsAtRisk(any(), anyInt())).thenReturn(5L);

            // Courier churn
            when(courierRepository.countVerifiedCouriers()).thenReturn(100L);
            when(courierRepository.countActiveCouriersSince(any())).thenReturn(75L);
            when(courierRepository.countInactiveCouriersSince(any())).thenReturn(25L);
            when(courierRepository.countCouriersAtRisk(any(), anyInt(), anyInt())).thenReturn(10L);

            // Retention cohorts
            when(userRepository.getUserRetentionCohorts(any())).thenReturn(new ArrayList<>());
        }
    }

    @Nested
    @DisplayName("Activation Metrics Tests")
    class ActivationMetricsTests {

        @Test
        @DisplayName("Should return correct first delivery count")
        void getFirstDeliveryCount_ReturnsCorrectCount() {
            // Given
            when(orderRepository.countFirstDeliveryCompletedSince(any(LocalDateTime.class)))
                    .thenReturn(50L);

            // When
            Long count = analyticsService.getFirstDeliveryCount();

            // Then
            assertThat(count).isEqualTo(50L);
        }

        @Test
        @DisplayName("Should return correct activation time")
        void getActivationTime_ReturnsCorrectHours() {
            // Given
            Object[] stats = new Object[]{24.5, 0.5, 168.0};
            when(orderRepository.getActivationTimeStatsSince(any(LocalDateTime.class)))
                    .thenReturn(stats);

            // When
            Double activationTime = analyticsService.getActivationTime();

            // Then
            assertThat(activationTime).isEqualTo(24.5);
        }

        @Test
        @DisplayName("Should return correct referral usage rate")
        void getReferralUsageRate_ReturnsCorrectPercentage() {
            // Given
            when(userRepository.countReferralSignupsSince(any(LocalDateTime.class)))
                    .thenReturn(50L);
            when(userRepository.countNewUsersSince(any(LocalDateTime.class)))
                    .thenReturn(200L);

            // When
            Double rate = analyticsService.getReferralUsageRate();

            // Then
            assertThat(rate).isEqualTo(25.0); // 50/200 * 100
        }

        @Test
        @DisplayName("Should handle zero new users for referral rate")
        void getReferralUsageRate_HandlesZeroNewUsers() {
            // Given
            when(userRepository.countReferralSignupsSince(any(LocalDateTime.class)))
                    .thenReturn(0L);
            when(userRepository.countNewUsersSince(any(LocalDateTime.class)))
                    .thenReturn(0L);

            // When
            Double rate = analyticsService.getReferralUsageRate();

            // Then
            assertThat(rate).isZero();
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null values from all repositories")
        void shouldHandleNullValuesGracefully() {
            // Given
            when(activityLogRepository.countDistinctActiveUsersSince(any()))
                    .thenReturn(null);
            when(orderRepository.countOrdersSince(any())).thenReturn(null);

            // When/Then - should not throw
            Long dau = analyticsService.getDailyActiveUsers();
            Long orders = analyticsService.getOrdersPerDay();

            assertThat(dau).isNull();
            assertThat(orders).isNull();
        }

        @Test
        @DisplayName("Should handle empty result lists")
        void shouldHandleEmptyLists() {
            // Given
            when(activityLogRepository.countDistinctUsersByEventTypesSince(any(), any()))
                    .thenReturn(new ArrayList<>());

            // When
            ConversionRateDto metrics = analyticsService.getConversionMetrics(1);

            // Then
            assertThat(metrics).isNotNull();
            assertThat(metrics.getRestaurantViews()).isZero();
        }

        @Test
        @DisplayName("Should prevent division by zero in ratio calculations")
        void shouldPreventDivisionByZero() {
            // Given
            when(activityLogRepository.countDistinctActiveUsersSince(any()))
                    .thenReturn(0L);
            when(userRepository.countNewUsersSince(any())).thenReturn(0L);

            // When
            DauWauMauDto metrics = analyticsService.getUserActivityMetrics();

            // Then - ratios should be null or zero, not throw exception
            assertThat(metrics.getDauMauRatio()).isNull();
        }
    }
}
