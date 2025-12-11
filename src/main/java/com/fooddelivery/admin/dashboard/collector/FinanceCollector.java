package com.fooddelivery.admin.dashboard.collector;

import com.fooddelivery.admin.dashboard.dto.DashboardFilterRequest;
import com.fooddelivery.admin.dashboard.dto.FinanceMetricsDto;
import com.fooddelivery.admin.dashboard.dto.FinanceMetricsDto.*;
import com.fooddelivery.admin.dashboard.repository.DashboardOrderRepository;
import com.fooddelivery.admin.dashboard.repository.DashboardRestaurantRepository;
import com.fooddelivery.admin.dashboard.repository.DashboardSupportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Collector for financial metrics and analytics data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinanceCollector {

    private final DashboardOrderRepository orderRepository;
    private final DashboardRestaurantRepository restaurantRepository;
    private final DashboardSupportRepository supportRepository;

    // Default commission rate (can be made configurable)
    private static final BigDecimal DEFAULT_COMMISSION_RATE = BigDecimal.valueOf(0.15); // 15%

    /**
     * Collect comprehensive financial metrics.
     */
    public FinanceMetricsDto collectFinanceMetrics(DashboardFilterRequest filter) {
        log.debug("Collecting finance metrics with filter: {}", filter);

        LocalDateTime startDate = filter.getStartDate();
        LocalDateTime endDate = filter.getEndDate();

        // Calculate GMV and revenue
        BigDecimal gmv = orderRepository.calculateGMV(startDate, endDate);
        gmv = gmv != null ? gmv : BigDecimal.ZERO;

        BigDecimal commissionRevenue = calculateCommissionRevenue(startDate, endDate);
        BigDecimal deliveryFeeRevenue = orderRepository.calculateDeliveryFeeRevenue(startDate, endDate);
        deliveryFeeRevenue = deliveryFeeRevenue != null ? deliveryFeeRevenue : BigDecimal.ZERO;

        BigDecimal totalRevenue = commissionRevenue.add(deliveryFeeRevenue);

        // Calculate costs
        BigDecimal restaurantPayouts = calculateRestaurantPayouts(startDate, endDate);
        BigDecimal courierPayouts = orderRepository.calculateCourierPayouts(startDate, endDate);
        courierPayouts = courierPayouts != null ? courierPayouts : BigDecimal.ZERO;

        BigDecimal discountsUsed = orderRepository.calculateDiscountsUsed(startDate, endDate);
        discountsUsed = discountsUsed != null ? discountsUsed : BigDecimal.ZERO;

        BigDecimal refundsPaid = supportRepository.calculateTotalRefunds(startDate, endDate);
        refundsPaid = refundsPaid != null ? refundsPaid : BigDecimal.ZERO;

        // Net revenue
        BigDecimal netRevenue = totalRevenue.subtract(discountsUsed).subtract(refundsPaid);

        // Unsettled payouts
        BigDecimal unsettledRestaurantPayouts = orderRepository.calculateUnsettledRestaurantPayouts(startDate, endDate);
        unsettledRestaurantPayouts = unsettledRestaurantPayouts != null ? unsettledRestaurantPayouts : BigDecimal.ZERO;

        BigDecimal unsettledCourierPayouts = orderRepository.calculateUnsettledCourierPayouts(startDate, endDate);
        unsettledCourierPayouts = unsettledCourierPayouts != null ? unsettledCourierPayouts : BigDecimal.ZERO;

        // Order statistics
        Long totalOrders = orderRepository.countOrdersToday(startDate, endDate);
        BigDecimal avgOrderValue = totalOrders > 0 ? gmv.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // Get detailed summaries
        PayoutSummaryDto payoutSummary = collectPayoutSummary(startDate, endDate, restaurantPayouts, courierPayouts,
                unsettledRestaurantPayouts, unsettledCourierPayouts);
        DiscountSummaryDto discountSummary = collectDiscountSummary(startDate, endDate);
        RefundSummaryDto refundSummary = collectRefundSummary(startDate, endDate);

        // Get daily revenue breakdown
        List<DailyRevenueDto> dailyRevenue = collectDailyRevenue(startDate, endDate);

        // Payment method breakdown
        Map<String, BigDecimal> paymentMethodBreakdown = collectPaymentMethodBreakdown(startDate, endDate);

        // Comparison with previous period
        Map<String, Object> periodComparison = collectPeriodComparison(startDate, endDate);

        return FinanceMetricsDto.builder()
                .gmv(gmv)
                .commissionRevenue(commissionRevenue)
                .deliveryFeeRevenue(deliveryFeeRevenue)
                .totalRevenue(totalRevenue)
                .netRevenue(netRevenue)
                .restaurantPayouts(restaurantPayouts)
                .courierPayouts(courierPayouts)
                .discountsUsed(discountsUsed)
                .refundsPaid(refundsPaid)
                .unsettledRestaurantPayouts(unsettledRestaurantPayouts)
                .unsettledCourierPayouts(unsettledCourierPayouts)
                .totalOrders(totalOrders)
                .avgOrderValue(avgOrderValue)
                .commissionRate(DEFAULT_COMMISSION_RATE.multiply(BigDecimal.valueOf(100)))
                .payoutSummary(payoutSummary)
                .discountSummary(discountSummary)
                .refundSummary(refundSummary)
                .dailyRevenue(dailyRevenue)
                .paymentMethodBreakdown(paymentMethodBreakdown)
                .periodComparison(periodComparison)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Calculate commission revenue from order totals.
     */
    private BigDecimal calculateCommissionRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal gmv = orderRepository.calculateGMV(startDate, endDate);
        if (gmv == null) {
            return BigDecimal.ZERO;
        }
        return gmv.multiply(DEFAULT_COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate restaurant payouts (GMV minus commission).
     */
    private BigDecimal calculateRestaurantPayouts(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal gmv = orderRepository.calculateGMV(startDate, endDate);
        if (gmv == null) {
            return BigDecimal.ZERO;
        }
        return gmv.subtract(gmv.multiply(DEFAULT_COMMISSION_RATE)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Collect payout summary details.
     */
    private PayoutSummaryDto collectPayoutSummary(LocalDateTime startDate, LocalDateTime endDate,
                                                   BigDecimal restaurantPayouts, BigDecimal courierPayouts,
                                                   BigDecimal unsettledRestaurant, BigDecimal unsettledCourier) {
        Long pendingRestaurantPayouts = orderRepository.countPendingRestaurantPayouts(startDate, endDate);
        Long pendingCourierPayouts = orderRepository.countPendingCourierPayouts(startDate, endDate);
        Long completedPayouts = orderRepository.countCompletedPayouts(startDate, endDate);

        return PayoutSummaryDto.builder()
                .totalRestaurantPayouts(restaurantPayouts)
                .totalCourierPayouts(courierPayouts)
                .pendingRestaurantPayouts(pendingRestaurantPayouts)
                .pendingCourierPayouts(pendingCourierPayouts)
                .unsettledRestaurantAmount(unsettledRestaurant)
                .unsettledCourierAmount(unsettledCourier)
                .completedPayoutsCount(completedPayouts)
                .avgSettlementTimeHours(calculateAvgSettlementTime(startDate, endDate))
                .build();
    }

    /**
     * Calculate average settlement time in hours.
     */
    private Double calculateAvgSettlementTime(LocalDateTime startDate, LocalDateTime endDate) {
        Double avgMinutes = orderRepository.avgPayoutSettlementTimeMinutes(startDate, endDate);
        if (avgMinutes == null) {
            return 0.0;
        }
        return BigDecimal.valueOf(avgMinutes / 60)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Collect discount summary details.
     */
    private DiscountSummaryDto collectDiscountSummary(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal totalDiscounts = orderRepository.calculateDiscountsUsed(startDate, endDate);
        totalDiscounts = totalDiscounts != null ? totalDiscounts : BigDecimal.ZERO;

        Long ordersWithDiscount = orderRepository.countOrdersWithDiscount(startDate, endDate);
        Long totalOrders = orderRepository.countOrdersToday(startDate, endDate);

        Double discountUsageRate = totalOrders > 0
                ? (ordersWithDiscount.doubleValue() / totalOrders) * 100
                : 0.0;

        BigDecimal avgDiscountPerOrder = ordersWithDiscount > 0
                ? totalDiscounts.divide(BigDecimal.valueOf(ordersWithDiscount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Get discount breakdown by type
        Map<String, BigDecimal> discountByType = collectDiscountByType(startDate, endDate);

        // Top promo codes
        List<Map<String, Object>> topPromoCodes = collectTopPromoCodes(startDate, endDate, 10);

        return DiscountSummaryDto.builder()
                .totalDiscounts(totalDiscounts)
                .ordersWithDiscount(ordersWithDiscount)
                .discountUsageRate(roundToTwoDecimals(discountUsageRate))
                .avgDiscountPerOrder(avgDiscountPerOrder)
                .discountByType(discountByType)
                .topPromoCodes(topPromoCodes)
                .build();
    }

    /**
     * Collect discounts breakdown by type.
     */
    private Map<String, BigDecimal> collectDiscountByType(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> discountData = orderRepository.getDiscountsByType(startDate, endDate);
        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();

        for (Object[] row : discountData) {
            String type = row[0] != null ? (String) row[0] : "OTHER";
            BigDecimal amount = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            breakdown.put(type, amount);
        }

        return breakdown;
    }

    /**
     * Collect top promo codes by usage.
     */
    private List<Map<String, Object>> collectTopPromoCodes(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        List<Object[]> promoData = orderRepository.getTopPromoCodes(startDate, endDate, limit);

        return promoData.stream()
                .map(row -> {
                    Map<String, Object> promo = new LinkedHashMap<>();
                    promo.put("code", row[0]);
                    promo.put("usageCount", ((Number) row[1]).longValue());
                    promo.put("totalDiscountAmount", row[2]);
                    return promo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Collect refund summary details.
     */
    private RefundSummaryDto collectRefundSummary(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal totalRefunds = supportRepository.calculateTotalRefunds(startDate, endDate);
        totalRefunds = totalRefunds != null ? totalRefunds : BigDecimal.ZERO;

        Long refundCount = supportRepository.countRefundRequests(startDate, endDate);
        Long approvedRefunds = supportRepository.countApprovedRefunds(startDate, endDate);
        Long pendingRefunds = supportRepository.countPendingRefunds(startDate, endDate);
        Long rejectedRefunds = supportRepository.countRejectedRefunds(startDate, endDate);

        Double approvalRate = refundCount > 0
                ? (approvedRefunds.doubleValue() / refundCount) * 100
                : 0.0;

        BigDecimal avgRefundAmount = approvedRefunds > 0
                ? totalRefunds.divide(BigDecimal.valueOf(approvedRefunds), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Refund breakdown by reason
        Map<String, Long> refundByReason = collectRefundByReason(startDate, endDate);

        return RefundSummaryDto.builder()
                .totalRefundAmount(totalRefunds)
                .refundCount(refundCount)
                .approvedRefunds(approvedRefunds)
                .pendingRefunds(pendingRefunds)
                .rejectedRefunds(rejectedRefunds)
                .approvalRate(roundToTwoDecimals(approvalRate))
                .avgRefundAmount(avgRefundAmount)
                .refundByReason(refundByReason)
                .build();
    }

    /**
     * Collect refund breakdown by reason.
     */
    private Map<String, Long> collectRefundByReason(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> refundData = supportRepository.getRefundsByReason(startDate, endDate);
        Map<String, Long> breakdown = new LinkedHashMap<>();

        for (Object[] row : refundData) {
            String reason = row[0] != null ? (String) row[0] : "OTHER";
            Long count = ((Number) row[1]).longValue();
            breakdown.put(reason, count);
        }

        return breakdown;
    }

    /**
     * Collect daily revenue data for the period.
     */
    private List<DailyRevenueDto> collectDailyRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> dailyData = orderRepository.getDailyRevenue(startDate, endDate);

        return dailyData.stream()
                .map(row -> {
                    LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
                    BigDecimal gmv = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
                    Long orderCount = ((Number) row[2]).longValue();
                    BigDecimal commission = gmv.multiply(DEFAULT_COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal deliveryFees = row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO;
                    BigDecimal discounts = row[4] != null ? (BigDecimal) row[4] : BigDecimal.ZERO;

                    return DailyRevenueDto.builder()
                            .date(date)
                            .gmv(gmv)
                            .commissionRevenue(commission)
                            .deliveryFeeRevenue(deliveryFees)
                            .discounts(discounts)
                            .orderCount(orderCount)
                            .avgOrderValue(orderCount > 0
                                    ? gmv.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                                    : BigDecimal.ZERO)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Collect payment method breakdown.
     */
    private Map<String, BigDecimal> collectPaymentMethodBreakdown(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> paymentData = orderRepository.getRevenueByPaymentMethod(startDate, endDate);
        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();

        for (Object[] row : paymentData) {
            String method = row[0] != null ? (String) row[0] : "OTHER";
            BigDecimal amount = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            breakdown.put(method, amount);
        }

        return breakdown;
    }

    /**
     * Collect comparison with previous period.
     */
    private Map<String, Object> collectPeriodComparison(LocalDateTime startDate, LocalDateTime endDate) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween == 0) daysBetween = 1;

        LocalDateTime prevStart = startDate.minusDays(daysBetween);
        LocalDateTime prevEnd = startDate;

        // Current period metrics
        BigDecimal currentGmv = orderRepository.calculateGMV(startDate, endDate);
        currentGmv = currentGmv != null ? currentGmv : BigDecimal.ZERO;
        Long currentOrders = orderRepository.countOrdersToday(startDate, endDate);

        // Previous period metrics
        BigDecimal prevGmv = orderRepository.calculateGMV(prevStart, prevEnd);
        prevGmv = prevGmv != null ? prevGmv : BigDecimal.ZERO;
        Long prevOrders = orderRepository.countOrdersToday(prevStart, prevEnd);

        // Calculate changes
        Double gmvChange = calculatePercentageChange(prevGmv, currentGmv);
        Double ordersChange = calculatePercentageChange(
                BigDecimal.valueOf(prevOrders), BigDecimal.valueOf(currentOrders));

        Map<String, Object> comparison = new LinkedHashMap<>();
        comparison.put("previousPeriodStart", prevStart);
        comparison.put("previousPeriodEnd", prevEnd);
        comparison.put("previousGmv", prevGmv);
        comparison.put("previousOrders", prevOrders);
        comparison.put("gmvChangePercent", gmvChange);
        comparison.put("ordersChangePercent", ordersChange);
        comparison.put("gmvTrend", gmvChange >= 0 ? "UP" : "DOWN");
        comparison.put("ordersTrend", ordersChange >= 0 ? "UP" : "DOWN");

        return comparison;
    }

    /**
     * Calculate percentage change between two values.
     */
    private Double calculatePercentageChange(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Round to two decimal places.
     */
    private Double roundToTwoDecimals(Double value) {
        if (value == null) {
            return 0.0;
        }
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
