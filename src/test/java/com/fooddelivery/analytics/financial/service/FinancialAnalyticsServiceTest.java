package com.fooddelivery.analytics.financial.service;

import com.fooddelivery.analytics.financial.dto.*;
import com.fooddelivery.analytics.financial.mapper.FinancialMetricsMapper;
import com.fooddelivery.analytics.financial.model.*;
import com.fooddelivery.analytics.financial.repository.*;
import com.fooddelivery.analytics.financial.service.impl.FinancialAnalyticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FinancialAnalyticsService.
 */
@ExtendWith(MockitoExtension.class)
class FinancialAnalyticsServiceTest {

    @Mock
    private RestaurantCommissionRepository commissionRepository;

    @Mock
    private CourierPaymentRepository courierPaymentRepository;

    @Mock
    private CourierBonusRepository courierBonusRepository;

    @Mock
    private PromotionUsageRepository promotionUsageRepository;

    @Mock
    private RestaurantPayoutRepository restaurantPayoutRepository;

    @Mock
    private PayoutDisputeRepository disputeRepository;

    @Mock
    private GiftCardUsageRepository giftCardUsageRepository;

    @Mock
    private ReferralRewardRepository referralRewardRepository;

    @Mock
    private FinancialMetricsMapper mapper;

    @InjectMocks
    private FinancialAnalyticsServiceImpl financialAnalyticsService;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private FinancialMetricsRequest request;

    @BeforeEach
    void setUp() {
        startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        endDate = LocalDateTime.of(2024, 1, 31, 23, 59);

        request = FinancialMetricsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .includeDailyTrend(false)
                .includeRestaurantBreakdown(false)
                .includeCourierBreakdown(false)
                .topN(10)
                .build();
    }

    @Nested
    @DisplayName("GMV Metrics Tests")
    class GmvMetricsTests {

        @Test
        @DisplayName("Should calculate GMV metrics successfully")
        void shouldCalculateGmvMetrics() {
            // Given
            BigDecimal totalGmv = new BigDecimal("100000.00");
            Long orderCount = 500L;

            when(commissionRepository.getTotalOrderSubtotal(any(), any())).thenReturn(totalGmv);
            when(commissionRepository.getOrderCount(any(), any())).thenReturn(orderCount);

            Object[] deliveryMetrics = new Object[]{
                    new BigDecimal("5000.00"),  // total payments
                    new BigDecimal("4000.00"),  // base payments
                    new BigDecimal("500.00"),   // distance bonus
                    new BigDecimal("1000.00"),  // tips
                    new BigDecimal("300.00"),   // peak bonus
                    500L,                        // delivery count
                    50L                          // courier count
            };
            when(courierPaymentRepository.getCourierPaymentMetrics(any(), any())).thenReturn(List.of(deliveryMetrics));

            // When
            GmvMetricsDto result = financialAnalyticsService.calculateGmvMetrics(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalGmv()).isEqualByComparingTo(totalGmv);
            assertThat(result.getTotalOrders()).isEqualTo(orderCount);
            assertThat(result.getAverageOrderValue()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(result.getPeriodStart()).isEqualTo(startDate);
            assertThat(result.getPeriodEnd()).isEqualTo(endDate);

            verify(commissionRepository).getTotalOrderSubtotal(startDate, endDate);
            verify(commissionRepository).getOrderCount(startDate, endDate);
        }

        @Test
        @DisplayName("Should handle zero orders gracefully")
        void shouldHandleZeroOrders() {
            // Given
            when(commissionRepository.getTotalOrderSubtotal(any(), any())).thenReturn(BigDecimal.ZERO);
            when(commissionRepository.getOrderCount(any(), any())).thenReturn(0L);

            Object[] emptyDeliveryMetrics = new Object[]{
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, 0L, 0L
            };
            when(courierPaymentRepository.getCourierPaymentMetrics(any(), any())).thenReturn(List.of(emptyDeliveryMetrics));

            // When
            GmvMetricsDto result = financialAnalyticsService.calculateGmvMetrics(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalGmv()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getTotalOrders()).isZero();
            assertThat(result.getAverageOrderValue()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Commission Metrics Tests")
    class CommissionMetricsTests {

        @Test
        @DisplayName("Should calculate commission metrics successfully")
        void shouldCalculateCommissionMetrics() {
            // Given
            Object[] commissionMetrics = new Object[]{
                    new BigDecimal("15000.00"),  // total commission
                    500L,                         // order count
                    new BigDecimal("30.00"),      // avg commission per order
                    new BigDecimal("0.15")        // effective rate (15%)
            };

            Object[] typeBreakdown = new Object[]{
                    new BigDecimal("12000.00"),  // percentage based
                    new BigDecimal("2500.00"),   // fixed
                    new BigDecimal("500.00")     // hybrid
            };

            when(commissionRepository.getCommissionMetrics(any(), any())).thenReturn(commissionMetrics);
            when(commissionRepository.getCommissionByCalculationType(any(), any())).thenReturn(typeBreakdown);
            when(commissionRepository.getTotalCommission(any(), any())).thenReturn(new BigDecimal("15000.00"));
            when(commissionRepository.getCommissionByType(any(), any())).thenReturn(Collections.emptyList());

            // When
            CommissionMetricsDto result = financialAnalyticsService.calculateCommissionMetrics(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalCommission()).isEqualByComparingTo(new BigDecimal("15000.00"));
            assertThat(result.getOrderCount()).isEqualTo(500L);
            assertThat(result.getAverageCommissionPerOrder()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(result.getEffectiveCommissionRate()).isEqualByComparingTo(new BigDecimal("0.15"));
            assertThat(result.getPercentageBasedCommission()).isEqualByComparingTo(new BigDecimal("12000.00"));
            assertThat(result.getFixedCommission()).isEqualByComparingTo(new BigDecimal("2500.00"));

            verify(commissionRepository).getCommissionMetrics(startDate, endDate);
        }

        @Test
        @DisplayName("Should calculate commission growth rate")
        void shouldCalculateCommissionGrowthRate() {
            // Given
            Object[] commissionMetrics = new Object[]{
                    new BigDecimal("15000.00"), 500L, new BigDecimal("30.00"), new BigDecimal("0.15")
            };
            Object[] typeBreakdown = new Object[]{
                    new BigDecimal("12000.00"), new BigDecimal("2500.00"), new BigDecimal("500.00")
            };

            when(commissionRepository.getCommissionMetrics(any(), any())).thenReturn(commissionMetrics);
            when(commissionRepository.getCommissionByCalculationType(any(), any())).thenReturn(typeBreakdown);
            when(commissionRepository.getTotalCommission(startDate, endDate)).thenReturn(new BigDecimal("15000.00"));
            when(commissionRepository.getTotalCommission(any(LocalDateTime.class), eq(startDate)))
                    .thenReturn(new BigDecimal("10000.00"));
            when(commissionRepository.getCommissionByType(any(), any())).thenReturn(Collections.emptyList());

            when(mapper.calculateGrowthRate(any(), any())).thenReturn(new BigDecimal("50.00"));

            // When
            CommissionMetricsDto result = financialAnalyticsService.calculateCommissionMetrics(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getGrowthRate()).isEqualByComparingTo(new BigDecimal("50.00"));
        }
    }

    @Nested
    @DisplayName("Delivery Fee Metrics Tests")
    class DeliveryFeeMetricsTests {

        @Test
        @DisplayName("Should calculate delivery fee metrics successfully")
        void shouldCalculateDeliveryFeeMetrics() {
            // Given
            Object[] deliveryMetrics = new Object[]{
                    new BigDecimal("25000.00"),  // total payments
                    new BigDecimal("20000.00"),  // base payments
                    new BigDecimal("2000.00"),   // distance bonus
                    new BigDecimal("5000.00"),   // tips
                    new BigDecimal("1500.00"),   // peak bonus
                    1000L,                        // delivery count
                    100L                          // courier count
            };

            when(courierPaymentRepository.getCourierPaymentMetrics(any(), any())).thenReturn(List.of(deliveryMetrics));
            when(courierPaymentRepository.getAverageDeliveryDistance(any(), any()))
                    .thenReturn(new BigDecimal("3.5"));

            // When
            DeliveryFeeMetricsDto result = financialAnalyticsService.calculateDeliveryFeeMetrics(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalDeliveryFeesPaid()).isEqualByComparingTo(new BigDecimal("25000.00"));
            assertThat(result.getDeliveryCount()).isEqualTo(1000L);
            assertThat(result.getTotalDistanceBonus()).isEqualByComparingTo(new BigDecimal("2000.00"));
            assertThat(result.getTotalPeakSurcharge()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(result.getAverageDistance()).isEqualByComparingTo(new BigDecimal("3.5"));

            verify(courierPaymentRepository).getCourierPaymentMetrics(startDate, endDate);
        }

        @Test
        @DisplayName("Should calculate net delivery fee revenue")
        void shouldCalculateNetDeliveryFeeRevenue() {
            // Given
            Object[] deliveryMetrics = new Object[]{
                    new BigDecimal("8500.00"),  // total fees paid
                    new BigDecimal("7000.00"), new BigDecimal("500.00"),
                    new BigDecimal("500.00"), new BigDecimal("300.00"),
                    500L, 50L
            };

            when(courierPaymentRepository.getCourierPaymentMetrics(any(), any())).thenReturn(List.of(deliveryMetrics));
            when(courierPaymentRepository.getAverageDeliveryDistance(any(), any())).thenReturn(new BigDecimal("4.0"));

            // When
            DeliveryFeeMetricsDto result = financialAnalyticsService.calculateDeliveryFeeMetrics(request);

            // Then
            assertThat(result).isNotNull();
            // Collected = Paid / (1 - 0.15) = 8500 / 0.85 = 10000
            assertThat(result.getTotalDeliveryFeesCollected()).isEqualByComparingTo(new BigDecimal("10000.00"));
            // Net = Collected - Paid = 10000 - 8500 = 1500
            assertThat(result.getNetDeliveryFeeRevenue()).isEqualByComparingTo(new BigDecimal("1500.00"));
        }
    }

    @Nested
    @DisplayName("Promotion Metrics Tests")
    class PromotionMetricsTests {

        @Test
        @DisplayName("Should calculate promotion metrics successfully")
        void shouldCalculatePromotionMetrics() {
            // Given
            Object[] promotionMetrics = new Object[]{
                    new BigDecimal("8000.00"),   // total discount
                    new BigDecimal("5000.00"),   // platform cost
                    new BigDecimal("3000.00"),   // restaurant cost
                    400L,                         // promoted order count
                    new BigDecimal("20.00")       // avg discount
            };

            when(promotionUsageRepository.getPromotionMetrics(any(), any())).thenReturn(promotionMetrics);
            when(giftCardUsageRepository.getTotalRedemptions(any(), any())).thenReturn(new BigDecimal("2000.00"));
            when(referralRewardRepository.getTotalRewardsPaid(any(), any())).thenReturn(new BigDecimal("1000.00"));
            when(commissionRepository.getOrderCount(any(), any())).thenReturn(500L);
            when(promotionUsageRepository.getPromotionByType(any(), any())).thenReturn(Collections.emptyList());
            when(promotionUsageRepository.getPromotionByFunder(any(), any())).thenReturn(Collections.emptyList());
            when(promotionUsageRepository.getTopPromotionsByUsage(any(), any())).thenReturn(Collections.emptyList());

            // When
            PromotionMetricsDto result = financialAnalyticsService.calculatePromotionMetrics(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalDiscountValue()).isEqualByComparingTo(new BigDecimal("8000.00"));
            assertThat(result.getPlatformCost()).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(result.getRestaurantCost()).isEqualByComparingTo(new BigDecimal("3000.00"));
            assertThat(result.getPromotedOrderCount()).isEqualTo(400L);
            assertThat(result.getTotalOrderCount()).isEqualTo(500L);
            assertThat(result.getGiftCardRedemptions()).isEqualByComparingTo(new BigDecimal("2000.00"));
            assertThat(result.getReferralRewardsPaid()).isEqualByComparingTo(new BigDecimal("1000.00"));
        }

        @Test
        @DisplayName("Should calculate promotion usage rate")
        void shouldCalculatePromotionUsageRate() {
            // Given
            Object[] promotionMetrics = new Object[]{
                    new BigDecimal("8000.00"), new BigDecimal("5000.00"),
                    new BigDecimal("3000.00"), 400L, new BigDecimal("20.00")
            };

            when(promotionUsageRepository.getPromotionMetrics(any(), any())).thenReturn(promotionMetrics);
            when(giftCardUsageRepository.getTotalRedemptions(any(), any())).thenReturn(BigDecimal.ZERO);
            when(referralRewardRepository.getTotalRewardsPaid(any(), any())).thenReturn(BigDecimal.ZERO);
            when(commissionRepository.getOrderCount(any(), any())).thenReturn(500L);
            when(promotionUsageRepository.getPromotionByType(any(), any())).thenReturn(Collections.emptyList());
            when(promotionUsageRepository.getPromotionByFunder(any(), any())).thenReturn(Collections.emptyList());
            when(promotionUsageRepository.getTopPromotionsByUsage(any(), any())).thenReturn(Collections.emptyList());

            // When
            PromotionMetricsDto result = financialAnalyticsService.calculatePromotionMetrics(request);

            // Then
            // Usage rate = 400 / 500 * 100 = 80%
            assertThat(result.getPromotionUsageRate()).isEqualByComparingTo(new BigDecimal("80.00"));
        }
    }

    @Nested
    @DisplayName("Contribution Margin Tests")
    class ContributionMarginTests {

        @Test
        @DisplayName("Should calculate contribution margin successfully")
        void shouldCalculateContributionMargin() {
            // Given
            when(commissionRepository.getTotalOrderSubtotal(any(), any())).thenReturn(new BigDecimal("100000.00"));
            when(commissionRepository.getOrderCount(any(), any())).thenReturn(500L);
            when(commissionRepository.getTotalCommission(any(), any())).thenReturn(new BigDecimal("15000.00"));

            Object[] commissionMetrics = new Object[]{
                    new BigDecimal("15000.00"), 500L, new BigDecimal("30.00"), new BigDecimal("0.15")
            };
            when(commissionRepository.getCommissionMetrics(any(), any())).thenReturn(commissionMetrics);

            Object[] deliveryMetrics = new Object[]{
                    new BigDecimal("8500.00"),  // total
                    new BigDecimal("7000.00"),  // base
                    new BigDecimal("500.00"),   // distance
                    new BigDecimal("500.00"),   // tips
                    new BigDecimal("300.00"),   // peak
                    500L, 50L
            };
            when(courierPaymentRepository.getCourierPaymentMetrics(any(), any())).thenReturn(List.of(deliveryMetrics));

            when(courierBonusRepository.getTotalBonuses(any(), any())).thenReturn(new BigDecimal("1000.00"));
            when(promotionUsageRepository.getTotalPlatformCost(any(), any())).thenReturn(new BigDecimal("5000.00"));
            when(giftCardUsageRepository.getTotalRedemptions(any(), any())).thenReturn(new BigDecimal("2000.00"));
            when(referralRewardRepository.getTotalRewardsPaid(any(), any())).thenReturn(new BigDecimal("1000.00"));

            when(mapper.calculateGrowthRate(any(), any())).thenReturn(BigDecimal.ZERO);

            // When
            ContributionMarginDto result = financialAnalyticsService.calculateContributionMargin(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getGmv()).isEqualByComparingTo(new BigDecimal("100000.00"));
            assertThat(result.getCommissionRevenue()).isEqualByComparingTo(new BigDecimal("15000.00"));
            assertThat(result.getOrderCount()).isEqualTo(500L);

            // Total revenue and costs should be calculated
            assertThat(result.getTotalRevenue()).isNotNull();
            assertThat(result.getTotalVariableCosts()).isNotNull();
            assertThat(result.getContributionMargin()).isNotNull();

            verify(commissionRepository).getTotalCommission(startDate, endDate);
            verify(courierPaymentRepository).getCourierPaymentMetrics(startDate, endDate);
            verify(promotionUsageRepository).getTotalPlatformCost(startDate, endDate);
        }

        @Test
        @DisplayName("Should calculate unit economics")
        void shouldCalculateUnitEconomics() {
            // Given
            when(commissionRepository.getTotalOrderSubtotal(any(), any())).thenReturn(new BigDecimal("100000.00"));
            when(commissionRepository.getOrderCount(any(), any())).thenReturn(500L);
            when(commissionRepository.getTotalCommission(any(), any())).thenReturn(new BigDecimal("15000.00"));

            Object[] commissionMetrics = new Object[]{
                    new BigDecimal("15000.00"), 500L, new BigDecimal("30.00"), new BigDecimal("0.15")
            };
            when(commissionRepository.getCommissionMetrics(any(), any())).thenReturn(commissionMetrics);

            Object[] deliveryMetrics = new Object[]{
                    new BigDecimal("8500.00"), new BigDecimal("7000.00"), new BigDecimal("500.00"),
                    new BigDecimal("500.00"), new BigDecimal("300.00"), 500L, 50L
            };
            when(courierPaymentRepository.getCourierPaymentMetrics(any(), any())).thenReturn(List.of(deliveryMetrics));

            when(courierBonusRepository.getTotalBonuses(any(), any())).thenReturn(new BigDecimal("1000.00"));
            when(promotionUsageRepository.getTotalPlatformCost(any(), any())).thenReturn(new BigDecimal("5000.00"));
            when(giftCardUsageRepository.getTotalRedemptions(any(), any())).thenReturn(BigDecimal.ZERO);
            when(referralRewardRepository.getTotalRewardsPaid(any(), any())).thenReturn(BigDecimal.ZERO);
            when(mapper.calculateGrowthRate(any(), any())).thenReturn(BigDecimal.ZERO);

            // When
            ContributionMarginDto result = financialAnalyticsService.calculateContributionMargin(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUnitEconomics()).isNotNull();
            assertThat(result.getRevenuePerOrder()).isNotNull();
            assertThat(result.getVariableCostPerOrder()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Restaurant Payout Metrics Tests")
    class RestaurantPayoutMetricsTests {

        @Test
        @DisplayName("Should calculate restaurant payout metrics successfully")
        void shouldCalculateRestaurantPayoutMetrics() {
            // Given
            Object[] payoutMetrics = new Object[]{
                    new BigDecimal("100000.00"),  // gross
                    new BigDecimal("15000.00"),   // commission
                    new BigDecimal("1000.00"),    // delivery subsidies
                    new BigDecimal("3000.00"),    // promotion costs
                    new BigDecimal("500.00"),     // adjustments
                    new BigDecimal("200.00"),     // fees
                    new BigDecimal("81300.00"),   // net payout
                    50L,                           // restaurant count
                    50L                            // payout count
            };

            when(restaurantPayoutRepository.getPayoutMetrics(any(), any())).thenReturn(payoutMetrics);
            when(restaurantPayoutRepository.getTotalByStatus(PaymentStatus.PENDING)).thenReturn(new BigDecimal("20000.00"));
            when(restaurantPayoutRepository.getTotalByStatus(PaymentStatus.COMPLETED)).thenReturn(new BigDecimal("61300.00"));
            when(restaurantPayoutRepository.getTotalByStatus(PaymentStatus.FAILED)).thenReturn(BigDecimal.ZERO);
            when(restaurantPayoutRepository.getAverageProcessingTimeHours(any(), any())).thenReturn(new BigDecimal("24.5"));
            when(disputeRepository.getDisputeCount(any(), any())).thenReturn(5L);
            when(disputeRepository.getTotalPendingDisputeAmount()).thenReturn(new BigDecimal("2500.00"));
            when(restaurantPayoutRepository.getPayoutsByStatus(any(), any())).thenReturn(Collections.emptyList());
            when(disputeRepository.getDisputeSummaryByStatus(any(), any())).thenReturn(Collections.emptyList());

            // When
            RestaurantPayoutMetricsDto result = financialAnalyticsService.calculateRestaurantPayoutMetrics(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalGrossSales()).isEqualByComparingTo(new BigDecimal("100000.00"));
            assertThat(result.getTotalCommissionsDeducted()).isEqualByComparingTo(new BigDecimal("15000.00"));
            assertThat(result.getNetPayoutAmount()).isEqualByComparingTo(new BigDecimal("81300.00"));
            assertThat(result.getRestaurantCount()).isEqualTo(50L);
            assertThat(result.getPendingPayouts()).isEqualByComparingTo(new BigDecimal("20000.00"));
            assertThat(result.getDisputeCount()).isEqualTo(5L);
            assertThat(result.getDisputeAmountPending()).isEqualByComparingTo(new BigDecimal("2500.00"));
        }
    }

    @Nested
    @DisplayName("Courier Payout Metrics Tests")
    class CourierPayoutMetricsTests {

        @Test
        @DisplayName("Should calculate courier payout metrics successfully")
        void shouldCalculateCourierPayoutMetrics() {
            // Given
            Object[] paymentMetrics = new Object[]{
                    new BigDecimal("50000.00"),   // total
                    new BigDecimal("35000.00"),   // base
                    new BigDecimal("5000.00"),    // distance
                    new BigDecimal("8000.00"),    // tips
                    new BigDecimal("2000.00"),    // peak
                    1000L,                         // deliveries
                    100L                           // couriers
            };

            Object[] bonusMetrics = new Object[]{
                    new BigDecimal("5000.00"),    // total bonuses
                    200L,                          // count
                    80L,                           // unique couriers
                    new BigDecimal("25.00")        // avg bonus
            };

            Object[] tipStats = new Object[]{
                    1000L,                         // total deliveries
                    700L,                          // deliveries with tips
                    new BigDecimal("11.43"),       // avg tip
                    new BigDecimal("8000.00")      // total tips
            };

            when(courierPaymentRepository.getCourierPaymentMetrics(any(), any())).thenReturn(List.of(paymentMetrics));
            when(courierBonusRepository.getBonusMetrics(any(), any())).thenReturn(bonusMetrics);
            when(courierPaymentRepository.getTipStatistics(any(), any())).thenReturn(tipStats);
            when(courierPaymentRepository.getTotalByStatus(eq(PaymentStatus.PENDING), any(), any())).thenReturn(new BigDecimal("10000.00"));
            when(courierPaymentRepository.getTotalByStatus(eq(PaymentStatus.COMPLETED), any(), any())).thenReturn(new BigDecimal("40000.00"));
            when(courierPaymentRepository.getPaymentsByType(any(), any())).thenReturn(Collections.emptyList());
            when(courierBonusRepository.getBonusesByType(any(), any())).thenReturn(Collections.emptyList());
            when(courierPaymentRepository.getPaymentsByStatus(any(), any())).thenReturn(Collections.emptyList());

            // When
            CourierPayoutMetricsDto result = financialAnalyticsService.calculateCourierPayoutMetrics(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalBasePayments()).isEqualByComparingTo(new BigDecimal("35000.00"));
            assertThat(result.getTotalDistanceBonuses()).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(result.getTotalTips()).isEqualByComparingTo(new BigDecimal("8000.00"));
            assertThat(result.getTotalPeakBonuses()).isEqualByComparingTo(new BigDecimal("2000.00"));
            assertThat(result.getTotalIncentives()).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(result.getActiveCourierCount()).isEqualTo(100L);
            assertThat(result.getTotalDeliveries()).isEqualTo(1000L);
            assertThat(result.getTipRate()).isEqualByComparingTo(new BigDecimal("70.00"));
        }
    }

    @Nested
    @DisplayName("Financial Summary Tests")
    class FinancialSummaryTests {

        @Test
        @DisplayName("Should get financial summary successfully")
        void shouldGetFinancialSummary() {
            // Given - Set up all necessary mocks for the summary
            when(commissionRepository.getTotalOrderSubtotal(any(), any())).thenReturn(new BigDecimal("100000.00"));
            when(commissionRepository.getOrderCount(any(), any())).thenReturn(500L);
            when(commissionRepository.getTotalCommission(any(), any())).thenReturn(new BigDecimal("15000.00"));

            Object[] commissionMetrics = new Object[]{
                    new BigDecimal("15000.00"), 500L, new BigDecimal("30.00"), new BigDecimal("0.15")
            };
            when(commissionRepository.getCommissionMetrics(any(), any())).thenReturn(commissionMetrics);
            when(commissionRepository.getCommissionByCalculationType(any(), any())).thenReturn(
                    new Object[]{new BigDecimal("12000.00"), new BigDecimal("3000.00"), BigDecimal.ZERO}
            );
            when(commissionRepository.getCommissionByType(any(), any())).thenReturn(Collections.emptyList());

            Object[] deliveryMetrics = new Object[]{
                    new BigDecimal("8500.00"), new BigDecimal("7000.00"), new BigDecimal("500.00"),
                    new BigDecimal("500.00"), new BigDecimal("300.00"), 500L, 50L
            };
            when(courierPaymentRepository.getCourierPaymentMetrics(any(), any())).thenReturn(List.of(deliveryMetrics));
            when(courierPaymentRepository.getAverageDeliveryDistance(any(), any())).thenReturn(new BigDecimal("3.5"));
            when(courierPaymentRepository.getTotalByStatus(any(), any(), any())).thenReturn(new BigDecimal("2000.00"));
            when(courierPaymentRepository.getPaymentsByStatus(any(), any())).thenReturn(Collections.emptyList());
            when(courierPaymentRepository.getPaymentsByType(any(), any())).thenReturn(Collections.emptyList());
            when(courierPaymentRepository.getTipStatistics(any(), any())).thenReturn(new Object[]{500L, 400L, new BigDecimal("1.25"), new BigDecimal("500.00")});

            Object[] bonusMetrics = new Object[]{new BigDecimal("1000.00"), 100L, 50L, new BigDecimal("10.00")};
            when(courierBonusRepository.getBonusMetrics(any(), any())).thenReturn(bonusMetrics);
            when(courierBonusRepository.getTotalBonuses(any(), any())).thenReturn(new BigDecimal("1000.00"));
            when(courierBonusRepository.getBonusesByType(any(), any())).thenReturn(Collections.emptyList());

            Object[] promotionMetrics = new Object[]{
                    new BigDecimal("5000.00"), new BigDecimal("3000.00"), new BigDecimal("2000.00"),
                    200L, new BigDecimal("25.00")
            };
            when(promotionUsageRepository.getPromotionMetrics(any(), any())).thenReturn(promotionMetrics);
            when(promotionUsageRepository.getTotalPlatformCost(any(), any())).thenReturn(new BigDecimal("3000.00"));
            when(promotionUsageRepository.getPromotionByType(any(), any())).thenReturn(Collections.emptyList());
            when(promotionUsageRepository.getPromotionByFunder(any(), any())).thenReturn(Collections.emptyList());
            when(promotionUsageRepository.getTopPromotionsByUsage(any(), any())).thenReturn(Collections.emptyList());

            when(giftCardUsageRepository.getTotalRedemptions(any(), any())).thenReturn(BigDecimal.ZERO);
            when(referralRewardRepository.getTotalRewardsPaid(any(), any())).thenReturn(BigDecimal.ZERO);

            when(restaurantPayoutRepository.getTotalByStatus(PaymentStatus.PENDING)).thenReturn(new BigDecimal("10000.00"));

            when(mapper.calculateGrowthRate(any(), any())).thenReturn(BigDecimal.ZERO);

            // When
            FinancialSummaryDto result = financialAnalyticsService.getFinancialSummary(startDate, endDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getGmv()).isNotNull();
            assertThat(result.getCommissionRevenue()).isNotNull();
            assertThat(result.getTotalOrders()).isEqualTo(500L);
        }
    }
}
