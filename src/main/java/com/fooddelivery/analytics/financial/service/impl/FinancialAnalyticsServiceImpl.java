package com.fooddelivery.analytics.financial.service.impl;

import com.fooddelivery.analytics.financial.dto.*;
import com.fooddelivery.analytics.financial.mapper.FinancialMetricsMapper;
import com.fooddelivery.analytics.financial.model.*;
import com.fooddelivery.analytics.financial.repository.*;
import com.fooddelivery.analytics.financial.service.FinancialAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of financial analytics service with optimized queries and caching.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialAnalyticsServiceImpl implements FinancialAnalyticsService {

    private final RestaurantCommissionRepository commissionRepository;
    private final CourierPaymentRepository courierPaymentRepository;
    private final CourierBonusRepository courierBonusRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final RestaurantPayoutRepository restaurantPayoutRepository;
    private final PayoutDisputeRepository disputeRepository;
    private final GiftCardUsageRepository giftCardUsageRepository;
    private final ReferralRewardRepository referralRewardRepository;
    private final FinancialMetricsMapper mapper;

    @Override
    @Cacheable(value = "gmv-metrics", key = "#request.startDate + '-' + #request.endDate",
               unless = "#result == null")
    public GmvMetricsDto calculateGmvMetrics(FinancialMetricsRequest request) {
        log.debug("Calculating GMV metrics for period: {} to {}", request.getStartDate(), request.getEndDate());

        LocalDateTime startDate = request.getStartDate();
        LocalDateTime endDate = request.getEndDate();

        // Get total GMV from order subtotals
        BigDecimal totalGmv = commissionRepository.getTotalOrderSubtotal(startDate, endDate);
        Long orderCount = commissionRepository.getOrderCount(startDate, endDate);

        // Calculate average order value
        BigDecimal averageOrderValue = orderCount > 0
                ? totalGmv.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Get delivery fee GMV
        Object[] deliveryMetrics = courierPaymentRepository.getCourierPaymentMetrics(startDate, endDate);
        BigDecimal deliveryFeeGmv = (BigDecimal) deliveryMetrics[0];
        BigDecimal tipGmv = (BigDecimal) deliveryMetrics[3];

        // Calculate food GMV (total GMV - delivery fees - tips)
        BigDecimal foodGmv = totalGmv.subtract(deliveryFeeGmv).subtract(tipGmv);

        // Calculate growth rate
        LocalDateTime previousStart = startDate.minus(ChronoUnit.DAYS.between(startDate, endDate), ChronoUnit.DAYS);
        BigDecimal previousGmv = commissionRepository.getTotalOrderSubtotal(previousStart, startDate);
        BigDecimal growthRate = mapper.calculateGrowthRate(totalGmv, previousGmv);

        // Build restaurant breakdown if requested
        List<GmvMetricsDto.RestaurantGmvDto> byRestaurant = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getIncludeRestaurantBreakdown())) {
            List<Object[]> restaurantData = commissionRepository.getTopRestaurantsByCommission(startDate, endDate);
            BigDecimal finalTotalGmv = totalGmv;
            byRestaurant = restaurantData.stream()
                    .limit(request.getTopN())
                    .map(row -> mapper.toRestaurantGmvDto(
                            (Long) row[0],
                            "Restaurant " + row[0], // Name would come from restaurant service
                            (BigDecimal) row[4], // Using order subtotal
                            (Long) row[3],
                            mapper.calculatePercentage((BigDecimal) row[4], finalTotalGmv)))
                    .collect(Collectors.toList());
        }

        // Build daily trend if requested
        List<GmvMetricsDto.DailyGmvDto> dailyTrend = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getIncludeDailyTrend())) {
            List<Object[]> dailyData = commissionRepository.getDailyCommissionTrend(startDate, endDate);
            dailyTrend = dailyData.stream()
                    .map(row -> mapper.toDailyGmvDto(
                            ((java.sql.Date) row[0]).toLocalDate(),
                            (BigDecimal) row[1],
                            ((Number) row[2]).longValue(),
                            (BigDecimal) row[3]))
                    .collect(Collectors.toList());
        }

        return GmvMetricsDto.builder()
                .totalGmv(totalGmv)
                .totalOrders(orderCount)
                .averageOrderValue(averageOrderValue)
                .foodGmv(foodGmv)
                .deliveryFeeGmv(deliveryFeeGmv)
                .tipGmv(tipGmv)
                .growthRate(growthRate)
                .previousPeriodGmv(previousGmv)
                .byRestaurant(byRestaurant)
                .dailyTrend(dailyTrend)
                .periodStart(startDate)
                .periodEnd(endDate)
                .build();
    }

    @Override
    @Cacheable(value = "commission-metrics", key = "#request.startDate + '-' + #request.endDate",
               unless = "#result == null")
    public CommissionMetricsDto calculateCommissionMetrics(FinancialMetricsRequest request) {
        log.debug("Calculating commission metrics for period: {} to {}", request.getStartDate(), request.getEndDate());

        LocalDateTime startDate = request.getStartDate();
        LocalDateTime endDate = request.getEndDate();

        // Get aggregated commission metrics
        Object[] metrics = commissionRepository.getCommissionMetrics(startDate, endDate);
        BigDecimal totalCommission = (BigDecimal) metrics[0];
        Long orderCount = ((Number) metrics[1]).longValue();
        BigDecimal avgCommissionPerOrder = (BigDecimal) metrics[2];
        BigDecimal effectiveRate = (BigDecimal) metrics[3];

        // Get percentage vs fixed breakdown
        Object[] typeBreakdown = commissionRepository.getCommissionByCalculationType(startDate, endDate);
        BigDecimal percentageCommission = (BigDecimal) typeBreakdown[0];
        BigDecimal fixedCommission = (BigDecimal) typeBreakdown[1];

        // Calculate growth rate
        LocalDateTime previousStart = startDate.minus(ChronoUnit.DAYS.between(startDate, endDate), ChronoUnit.DAYS);
        BigDecimal previousCommission = commissionRepository.getTotalCommission(previousStart, startDate);
        BigDecimal growthRate = mapper.calculateGrowthRate(totalCommission, previousCommission);

        // Build commission by type breakdown
        List<Object[]> byTypeData = commissionRepository.getCommissionByType(startDate, endDate);
        BigDecimal finalTotalCommission = totalCommission;
        List<CommissionMetricsDto.CommissionByTypeDto> byType = byTypeData.stream()
                .map(row -> mapper.toCommissionByTypeDto(
                        (CommissionType) row[0],
                        (BigDecimal) row[1],
                        ((Number) row[2]).longValue(),
                        mapper.calculatePercentage((BigDecimal) row[1], finalTotalCommission)))
                .collect(Collectors.toList());

        // Build top restaurants breakdown
        List<CommissionMetricsDto.RestaurantCommissionDto> topRestaurants = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getIncludeRestaurantBreakdown())) {
            List<Object[]> restaurantData = commissionRepository.getTopRestaurantsByCommission(startDate, endDate);
            topRestaurants = restaurantData.stream()
                    .limit(request.getTopN())
                    .map(row -> mapper.toRestaurantCommissionDto(
                            (Long) row[0],
                            "Restaurant " + row[0],
                            (BigDecimal) row[1],
                            (BigDecimal) row[2],
                            ((Number) row[3]).longValue(),
                            (BigDecimal) row[4]))
                    .collect(Collectors.toList());
        }

        // Build daily trend
        List<CommissionMetricsDto.DailyCommissionDto> dailyTrend = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getIncludeDailyTrend())) {
            List<Object[]> dailyData = commissionRepository.getDailyCommissionTrend(startDate, endDate);
            dailyTrend = dailyData.stream()
                    .map(row -> mapper.toDailyCommissionDto(
                            ((java.sql.Date) row[0]).toLocalDate(),
                            (BigDecimal) row[1],
                            ((Number) row[2]).longValue(),
                            (BigDecimal) row[3]))
                    .collect(Collectors.toList());
        }

        return CommissionMetricsDto.builder()
                .totalCommission(totalCommission)
                .orderCount(orderCount)
                .averageCommissionPerOrder(avgCommissionPerOrder)
                .effectiveCommissionRate(effectiveRate)
                .percentageBasedCommission(percentageCommission)
                .fixedCommission(fixedCommission)
                .growthRate(growthRate)
                .previousPeriodCommission(previousCommission)
                .byType(byType)
                .topRestaurants(topRestaurants)
                .dailyTrend(dailyTrend)
                .periodStart(startDate)
                .periodEnd(endDate)
                .build();
    }

    @Override
    @Cacheable(value = "delivery-fee-metrics", key = "#request.startDate + '-' + #request.endDate",
               unless = "#result == null")
    public DeliveryFeeMetricsDto calculateDeliveryFeeMetrics(FinancialMetricsRequest request) {
        log.debug("Calculating delivery fee metrics for period: {} to {}", request.getStartDate(), request.getEndDate());

        LocalDateTime startDate = request.getStartDate();
        LocalDateTime endDate = request.getEndDate();

        // Get courier payment metrics
        Object[] metrics = courierPaymentRepository.getCourierPaymentMetrics(startDate, endDate);
        BigDecimal totalFeesPaid = (BigDecimal) metrics[0];
        BigDecimal basePayments = (BigDecimal) metrics[1];
        BigDecimal distanceBonus = (BigDecimal) metrics[2];
        BigDecimal totalTips = (BigDecimal) metrics[3];
        BigDecimal peakBonus = (BigDecimal) metrics[4];
        Long deliveryCount = ((Number) metrics[5]).longValue();

        // Estimate fees collected (fees paid + platform margin)
        // In practice, this would come from order data
        BigDecimal platformMargin = BigDecimal.valueOf(0.15); // 15% platform margin on delivery fees
        BigDecimal totalFeesCollected = totalFeesPaid.divide(
                BigDecimal.ONE.subtract(platformMargin), 2, RoundingMode.HALF_UP);
        BigDecimal netRevenue = totalFeesCollected.subtract(totalFeesPaid);

        BigDecimal averageDeliveryFee = deliveryCount > 0
                ? totalFeesCollected.divide(BigDecimal.valueOf(deliveryCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal averageDistance = courierPaymentRepository.getAverageDistanceBonus(startDate, endDate);

        // Build daily trend
        List<DeliveryFeeMetricsDto.DailyDeliveryFeeDto> dailyTrend = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getIncludeDailyTrend())) {
            List<Object[]> dailyData = courierPaymentRepository.getDailyCourierPaymentTrend(startDate, endDate);
            dailyTrend = dailyData.stream()
                    .map(row -> {
                        BigDecimal dailyPaid = (BigDecimal) row[1];
                        BigDecimal dailyCollected = dailyPaid.divide(
                                BigDecimal.ONE.subtract(platformMargin), 2, RoundingMode.HALF_UP);
                        return mapper.toDailyDeliveryFeeDto(
                                ((java.sql.Date) row[0]).toLocalDate(),
                                dailyCollected,
                                dailyPaid,
                                dailyCollected.subtract(dailyPaid),
                                ((Number) row[6]).longValue());
                    })
                    .collect(Collectors.toList());
        }

        return DeliveryFeeMetricsDto.builder()
                .totalDeliveryFeesCollected(totalFeesCollected)
                .totalDeliveryFeesPaid(totalFeesPaid)
                .netDeliveryFeeRevenue(netRevenue)
                .deliveryCount(deliveryCount)
                .averageDeliveryFee(averageDeliveryFee)
                .averageDistance(averageDistance)
                .totalDistanceBonus(distanceBonus)
                .totalPeakSurcharge(peakBonus)
                .dailyTrend(dailyTrend)
                .periodStart(startDate)
                .periodEnd(endDate)
                .build();
    }

    @Override
    @Cacheable(value = "promotion-metrics", key = "#request.startDate + '-' + #request.endDate",
               unless = "#result == null")
    public PromotionMetricsDto calculatePromotionMetrics(FinancialMetricsRequest request) {
        log.debug("Calculating promotion metrics for period: {} to {}", request.getStartDate(), request.getEndDate());

        LocalDateTime startDate = request.getStartDate();
        LocalDateTime endDate = request.getEndDate();

        // Get promotion metrics
        Object[] metrics = promotionUsageRepository.getPromotionMetrics(startDate, endDate);
        BigDecimal totalDiscount = (BigDecimal) metrics[0];
        BigDecimal platformCost = (BigDecimal) metrics[1];
        BigDecimal restaurantCost = (BigDecimal) metrics[2];
        Long promotedOrderCount = ((Number) metrics[3]).longValue();
        BigDecimal avgDiscount = (BigDecimal) metrics[4];

        // Get gift card and referral costs
        BigDecimal giftCardRedemptions = giftCardUsageRepository.getTotalRedemptions(startDate, endDate);
        BigDecimal referralRewards = referralRewardRepository.getTotalRewardsPaid(startDate, endDate);

        // Get total order count for usage rate
        Long totalOrderCount = commissionRepository.getOrderCount(startDate, endDate);
        BigDecimal usageRate = totalOrderCount > 0
                ? BigDecimal.valueOf(promotedOrderCount)
                        .divide(BigDecimal.valueOf(totalOrderCount), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Build promotion by type breakdown
        List<Object[]> byTypeData = promotionUsageRepository.getPromotionByType(startDate, endDate);
        BigDecimal finalTotalDiscount = totalDiscount;
        List<PromotionMetricsDto.PromotionByTypeDto> byType = byTypeData.stream()
                .map(row -> mapper.toPromotionByTypeDto(
                        (PromotionType) row[0],
                        (BigDecimal) row[1],
                        ((Number) row[2]).longValue(),
                        (BigDecimal) row[3],
                        mapper.calculatePercentage((BigDecimal) row[1], finalTotalDiscount)))
                .collect(Collectors.toList());

        // Build promotion by funder breakdown
        List<Object[]> byFunderData = promotionUsageRepository.getPromotionByFunder(startDate, endDate);
        List<PromotionMetricsDto.PromotionByFunderDto> byFunder = byFunderData.stream()
                .map(row -> mapper.toPromotionByFunderDto(
                        (PromotionFunder) row[0],
                        (BigDecimal) row[1],
                        ((Number) row[2]).longValue(),
                        mapper.calculatePercentage((BigDecimal) row[1], finalTotalDiscount)))
                .collect(Collectors.toList());

        // Build top promotions
        List<Object[]> topPromosData = promotionUsageRepository.getTopPromotionsByUsage(startDate, endDate);
        List<PromotionMetricsDto.TopPromotionDto> topPromotions = topPromosData.stream()
                .limit(request.getTopN())
                .map(row -> mapper.toTopPromotionDto(
                        (Long) row[0],
                        (String) row[1],
                        (String) row[1], // Using code as name
                        (PromotionType) row[2],
                        (BigDecimal) row[3],
                        ((Number) row[4]).longValue(),
                        (BigDecimal) row[5],
                        (BigDecimal) row[6]))
                .collect(Collectors.toList());

        // Build daily trend
        List<PromotionMetricsDto.DailyPromotionDto> dailyTrend = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getIncludeDailyTrend())) {
            List<Object[]> dailyData = promotionUsageRepository.getDailyPromotionTrend(startDate, endDate);
            dailyTrend = dailyData.stream()
                    .map(row -> mapper.toDailyPromotionDto(
                            ((java.sql.Date) row[0]).toLocalDate(),
                            (BigDecimal) row[1],
                            (BigDecimal) row[2],
                            (BigDecimal) row[3],
                            ((Number) row[4]).longValue(),
                            usageRate))
                    .collect(Collectors.toList());
        }

        return PromotionMetricsDto.builder()
                .totalDiscountValue(totalDiscount)
                .platformCost(platformCost)
                .restaurantCost(restaurantCost)
                .promotedOrderCount(promotedOrderCount)
                .totalOrderCount(totalOrderCount)
                .promotionUsageRate(usageRate)
                .averageDiscountPerOrder(avgDiscount)
                .giftCardRedemptions(giftCardRedemptions)
                .referralRewardsPaid(referralRewards)
                .byType(byType)
                .byFunder(byFunder)
                .topPromotions(topPromotions)
                .dailyTrend(dailyTrend)
                .periodStart(startDate)
                .periodEnd(endDate)
                .build();
    }

    @Override
    @Cacheable(value = "restaurant-payout-metrics", key = "#request.startDate + '-' + #request.endDate",
               unless = "#result == null")
    public RestaurantPayoutMetricsDto calculateRestaurantPayoutMetrics(FinancialMetricsRequest request) {
        log.debug("Calculating restaurant payout metrics for period: {} to {}",
                request.getStartDate(), request.getEndDate());

        LocalDateTime startDate = request.getStartDate();
        LocalDateTime endDate = request.getEndDate();

        // Get payout metrics
        Object[] metrics = restaurantPayoutRepository.getPayoutMetrics(startDate, endDate);
        BigDecimal grossSales = (BigDecimal) metrics[0];
        BigDecimal commissionsDeducted = (BigDecimal) metrics[1];
        BigDecimal deliverySubsidies = (BigDecimal) metrics[2];
        BigDecimal promotionCosts = (BigDecimal) metrics[3];
        BigDecimal adjustments = (BigDecimal) metrics[4];
        BigDecimal fees = (BigDecimal) metrics[5];
        BigDecimal netPayout = (BigDecimal) metrics[6];
        Long restaurantCount = ((Number) metrics[7]).longValue();
        Long payoutCount = ((Number) metrics[8]).longValue();

        BigDecimal avgPayoutPerRestaurant = restaurantCount > 0
                ? netPayout.divide(BigDecimal.valueOf(restaurantCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Get pending/completed/failed payouts
        BigDecimal pendingPayouts = restaurantPayoutRepository.getTotalByStatus(PaymentStatus.PENDING);
        BigDecimal completedPayouts = restaurantPayoutRepository.getTotalByStatus(PaymentStatus.COMPLETED);
        BigDecimal failedPayouts = restaurantPayoutRepository.getTotalByStatus(PaymentStatus.FAILED);

        // Get average processing time
        BigDecimal avgProcessingTime = restaurantPayoutRepository.getAverageProcessingTimeHours(startDate, endDate);

        // Get dispute metrics
        Long disputeCount = disputeRepository.getDisputeCount(startDate, endDate);
        BigDecimal pendingDisputeAmount = disputeRepository.getTotalPendingDisputeAmount();

        // Build payouts by status
        List<Object[]> byStatusData = restaurantPayoutRepository.getPayoutsByStatus(startDate, endDate);
        BigDecimal finalNetPayout = netPayout;
        List<RestaurantPayoutMetricsDto.PayoutByStatusDto> byStatus = byStatusData.stream()
                .map(row -> mapper.toPayoutByStatusDto(
                        (PaymentStatus) row[0],
                        (BigDecimal) row[1],
                        ((Number) row[2]).longValue(),
                        mapper.calculatePercentage((BigDecimal) row[1], finalNetPayout)))
                .collect(Collectors.toList());

        // Build top restaurants
        List<RestaurantPayoutMetricsDto.RestaurantPayoutDetailDto> topRestaurants = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getIncludeRestaurantBreakdown())) {
            List<Object[]> restaurantData = restaurantPayoutRepository.getTopRestaurantsByPayout(startDate, endDate);
            topRestaurants = restaurantData.stream()
                    .limit(request.getTopN())
                    .map(row -> {
                        BigDecimal gross = (BigDecimal) row[1];
                        BigDecimal commission = (BigDecimal) row[2];
                        BigDecimal effectiveRate = gross.compareTo(BigDecimal.ZERO) > 0
                                ? commission.divide(gross, 4, RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(100))
                                : BigDecimal.ZERO;
                        return mapper.toRestaurantPayoutDetailDto(
                                (Long) row[0],
                                "Restaurant " + row[0],
                                gross,
                                commission,
                                (BigDecimal) row[3],
                                ((Number) row[4]).longValue(),
                                effectiveRate);
                    })
                    .collect(Collectors.toList());
        }

        // Build dispute summary
        List<Object[]> disputeData = disputeRepository.getDisputeSummaryByStatus(startDate, endDate);
        List<RestaurantPayoutMetricsDto.DisputeSummaryDto> disputeSummary = disputeData.stream()
                .map(row -> mapper.toDisputeSummaryDto(
                        (DisputeStatus) row[0],
                        ((Number) row[1]).longValue(),
                        (BigDecimal) row[2],
                        row[3] != null ? BigDecimal.valueOf(((Number) row[3]).doubleValue()) : null))
                .collect(Collectors.toList());

        // Build daily trend
        List<RestaurantPayoutMetricsDto.DailyPayoutDto> dailyTrend = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getIncludeDailyTrend())) {
            List<Object[]> dailyData = restaurantPayoutRepository.getDailyPayoutTrend(startDate, endDate);
            dailyTrend = dailyData.stream()
                    .map(row -> mapper.toDailyPayoutDto(
                            ((java.sql.Date) row[0]).toLocalDate(),
                            (BigDecimal) row[1],
                            (BigDecimal) row[2],
                            ((Number) row[3]).longValue(),
                            0L)) // Daily dispute count would need separate query
                    .collect(Collectors.toList());
        }

        return RestaurantPayoutMetricsDto.builder()
                .totalGrossSales(grossSales)
                .totalCommissionsDeducted(commissionsDeducted)
                .totalDeliverySubsidies(deliverySubsidies)
                .totalPromotionCosts(promotionCosts)
                .totalAdjustments(adjustments)
                .totalFees(fees)
                .netPayoutAmount(netPayout)
                .restaurantCount(restaurantCount)
                .averagePayoutPerRestaurant(avgPayoutPerRestaurant)
                .pendingPayouts(pendingPayouts)
                .completedPayouts(completedPayouts)
                .failedPayouts(failedPayouts)
                .averageProcessingTime(avgProcessingTime)
                .disputeCount(disputeCount)
                .disputeAmountPending(pendingDisputeAmount)
                .byStatus(byStatus)
                .topRestaurants(topRestaurants)
                .disputeSummary(disputeSummary)
                .dailyTrend(dailyTrend)
                .periodStart(startDate)
                .periodEnd(endDate)
                .build();
    }

    @Override
    @Cacheable(value = "courier-payout-metrics", key = "#request.startDate + '-' + #request.endDate",
               unless = "#result == null")
    public CourierPayoutMetricsDto calculateCourierPayoutMetrics(FinancialMetricsRequest request) {
        log.debug("Calculating courier payout metrics for period: {} to {}",
                request.getStartDate(), request.getEndDate());

        LocalDateTime startDate = request.getStartDate();
        LocalDateTime endDate = request.getEndDate();

        // Get courier payment metrics
        Object[] metrics = courierPaymentRepository.getCourierPaymentMetrics(startDate, endDate);
        BigDecimal totalPayments = (BigDecimal) metrics[0];
        BigDecimal basePayments = (BigDecimal) metrics[1];
        BigDecimal distanceBonuses = (BigDecimal) metrics[2];
        BigDecimal tips = (BigDecimal) metrics[3];
        BigDecimal peakBonuses = (BigDecimal) metrics[4];
        Long deliveryCount = ((Number) metrics[5]).longValue();
        Long activeCouriers = ((Number) metrics[6]).longValue();

        // Get bonus metrics
        Object[] bonusMetrics = courierBonusRepository.getBonusMetrics(startDate, endDate);
        BigDecimal totalIncentives = (BigDecimal) bonusMetrics[0];

        // Calculate totals
        BigDecimal totalPayouts = totalPayments.add(totalIncentives);
        BigDecimal avgPayoutPerDelivery = deliveryCount > 0
                ? totalPayouts.divide(BigDecimal.valueOf(deliveryCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal avgPayoutPerCourier = activeCouriers > 0
                ? totalPayouts.divide(BigDecimal.valueOf(activeCouriers), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal avgDeliveriesPerCourier = activeCouriers > 0
                ? BigDecimal.valueOf(deliveryCount).divide(BigDecimal.valueOf(activeCouriers), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Get tip statistics
        Object[] tipStats = courierPaymentRepository.getTipStatistics(startDate, endDate);
        Long totalDeliveries = ((Number) tipStats[0]).longValue();
        Long deliveriesWithTips = ((Number) tipStats[1]).longValue();
        BigDecimal avgTipPerDelivery = (BigDecimal) tipStats[2];
        BigDecimal tipRate = totalDeliveries > 0
                ? BigDecimal.valueOf(deliveriesWithTips)
                        .divide(BigDecimal.valueOf(totalDeliveries), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Get pending/completed payouts
        BigDecimal pendingPayouts = courierPaymentRepository.getTotalByStatus(PaymentStatus.PENDING, startDate, endDate);
        BigDecimal completedPayouts = courierPaymentRepository.getTotalByStatus(PaymentStatus.COMPLETED, startDate, endDate);

        // Build payment by type breakdown
        List<Object[]> byTypeData = courierPaymentRepository.getPaymentsByType(startDate, endDate);
        BigDecimal finalTotalPayouts = totalPayouts;
        List<CourierPayoutMetricsDto.PayoutByTypeDto> byPaymentType = byTypeData.stream()
                .map(row -> mapper.toCourierPayoutByTypeDto(
                        (CourierPaymentType) row[0],
                        (BigDecimal) row[1],
                        ((Number) row[2]).longValue(),
                        mapper.calculatePercentage((BigDecimal) row[1], finalTotalPayouts)))
                .collect(Collectors.toList());

        // Build bonus by type breakdown
        List<Object[]> bonusByTypeData = courierBonusRepository.getBonusesByType(startDate, endDate);
        List<CourierPayoutMetricsDto.BonusByTypeDto> byBonusType = bonusByTypeData.stream()
                .map(row -> mapper.toBonusByTypeDto(
                        (BonusType) row[0],
                        (BigDecimal) row[1],
                        ((Number) row[2]).longValue(),
                        (BigDecimal) row[3]))
                .collect(Collectors.toList());

        // Build payment by status breakdown
        List<Object[]> byStatusData = courierPaymentRepository.getPaymentsByStatus(startDate, endDate);
        List<CourierPayoutMetricsDto.PayoutByStatusDto> byStatus = byStatusData.stream()
                .map(row -> mapper.toCourierPayoutByStatusDto(
                        (PaymentStatus) row[0],
                        (BigDecimal) row[1],
                        ((Number) row[2]).longValue(),
                        mapper.calculatePercentage((BigDecimal) row[1], finalTotalPayouts)))
                .collect(Collectors.toList());

        // Build top couriers
        List<CourierPayoutMetricsDto.CourierPayoutDetailDto> topCouriers = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getIncludeCourierBreakdown())) {
            List<Object[]> courierData = courierPaymentRepository.getTopCouriersByEarnings(startDate, endDate);
            topCouriers = courierData.stream()
                    .limit(request.getTopN())
                    .map(row -> {
                        Long count = ((Number) row[5]).longValue();
                        BigDecimal total = (BigDecimal) row[1];
                        return mapper.toCourierPayoutDetailDto(
                                (Long) row[0],
                                "Courier " + row[0],
                                total,
                                (BigDecimal) row[2],
                                (BigDecimal) row[3],
                                (BigDecimal) row[4],
                                count,
                                count > 0 ? total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                                        : BigDecimal.ZERO);
                    })
                    .collect(Collectors.toList());
        }

        // Build daily trend
        List<CourierPayoutMetricsDto.DailyCourierPayoutDto> dailyTrend = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getIncludeDailyTrend())) {
            List<Object[]> dailyData = courierPaymentRepository.getDailyCourierPaymentTrend(startDate, endDate);
            dailyTrend = dailyData.stream()
                    .map(row -> mapper.toDailyCourierPayoutDto(
                            ((java.sql.Date) row[0]).toLocalDate(),
                            (BigDecimal) row[1],
                            (BigDecimal) row[2],
                            (BigDecimal) row[3],
                            (BigDecimal) row[4],
                            ((Number) row[5]).longValue(),
                            ((Number) row[6]).longValue()))
                    .collect(Collectors.toList());
        }

        return CourierPayoutMetricsDto.builder()
                .totalBasePayments(basePayments)
                .totalDistanceBonuses(distanceBonuses)
                .totalTips(tips)
                .totalPeakBonuses(peakBonuses)
                .totalIncentives(totalIncentives)
                .totalPayouts(totalPayouts)
                .activeCourierCount(activeCouriers)
                .totalDeliveries(deliveryCount)
                .averagePayoutPerDelivery(avgPayoutPerDelivery)
                .averagePayoutPerCourier(avgPayoutPerCourier)
                .averageDeliveriesPerCourier(avgDeliveriesPerCourier)
                .averageTipPerDelivery(avgTipPerDelivery)
                .tipRate(tipRate)
                .pendingPayouts(pendingPayouts)
                .completedPayouts(completedPayouts)
                .byPaymentType(byPaymentType)
                .byBonusType(byBonusType)
                .byStatus(byStatus)
                .topCouriers(topCouriers)
                .dailyTrend(dailyTrend)
                .periodStart(startDate)
                .periodEnd(endDate)
                .build();
    }

    @Override
    @Cacheable(value = "contribution-margin", key = "#request.startDate + '-' + #request.endDate",
               unless = "#result == null")
    public ContributionMarginDto calculateContributionMargin(FinancialMetricsRequest request) {
        log.debug("Calculating contribution margin for period: {} to {}",
                request.getStartDate(), request.getEndDate());

        LocalDateTime startDate = request.getStartDate();
        LocalDateTime endDate = request.getEndDate();

        // Get GMV
        BigDecimal gmv = commissionRepository.getTotalOrderSubtotal(startDate, endDate);
        Long orderCount = commissionRepository.getOrderCount(startDate, endDate);

        // Get commission revenue
        BigDecimal commissionRevenue = commissionRepository.getTotalCommission(startDate, endDate);

        // Get delivery fee revenue
        Object[] deliveryMetrics = courierPaymentRepository.getCourierPaymentMetrics(startDate, endDate);
        BigDecimal deliveryFeesPaid = (BigDecimal) deliveryMetrics[0];
        BigDecimal platformMargin = BigDecimal.valueOf(0.15);
        BigDecimal deliveryFeesCollected = deliveryFeesPaid.divide(
                BigDecimal.ONE.subtract(platformMargin), 2, RoundingMode.HALF_UP);
        BigDecimal deliveryFeeRevenue = deliveryFeesCollected.subtract(deliveryFeesPaid);

        // Get courier costs
        BigDecimal courierBasePay = (BigDecimal) deliveryMetrics[1];
        BigDecimal courierBonuses = ((BigDecimal) deliveryMetrics[2]).add((BigDecimal) deliveryMetrics[4]); // distance + peak
        BigDecimal courierTips = (BigDecimal) deliveryMetrics[3]; // Pass-through, not a cost
        BigDecimal incentives = courierBonusRepository.getTotalBonuses(startDate, endDate);
        BigDecimal totalCourierCosts = courierBasePay.add(courierBonuses).add(incentives);

        // Get promotion costs
        BigDecimal promotionCosts = promotionUsageRepository.getTotalPlatformCost(startDate, endDate);
        BigDecimal giftCardCosts = giftCardUsageRepository.getTotalRedemptions(startDate, endDate);
        BigDecimal referralCosts = referralRewardRepository.getTotalRewardsPaid(startDate, endDate);
        BigDecimal totalPromotionCosts = promotionCosts.add(giftCardCosts).add(referralCosts);

        // Calculate contribution margin
        BigDecimal totalRevenue = commissionRevenue.add(deliveryFeeRevenue);
        BigDecimal totalVariableCosts = totalCourierCosts.add(totalPromotionCosts);
        BigDecimal contributionMargin = totalRevenue.subtract(totalVariableCosts);

        BigDecimal contributionMarginPercentage = gmv.compareTo(BigDecimal.ZERO) > 0
                ? contributionMargin.divide(gmv, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        BigDecimal unitEconomics = orderCount > 0
                ? contributionMargin.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal revenuePerOrder = orderCount > 0
                ? totalRevenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal variableCostPerOrder = orderCount > 0
                ? totalVariableCosts.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Calculate break-even order value
        BigDecimal effectiveRate = commissionRepository.getCommissionMetrics(startDate, endDate)[3] != null
                ? (BigDecimal) commissionRepository.getCommissionMetrics(startDate, endDate)[3]
                : BigDecimal.ZERO;
        BigDecimal breakEvenOrderValue = effectiveRate.compareTo(BigDecimal.ZERO) > 0
                ? variableCostPerOrder.divide(effectiveRate, 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Calculate growth rate
        LocalDateTime previousStart = startDate.minus(ChronoUnit.DAYS.between(startDate, endDate), ChronoUnit.DAYS);
        BigDecimal previousCommission = commissionRepository.getTotalCommission(previousStart, startDate);
        BigDecimal previousDeliveryFees = courierPaymentRepository.getCourierPaymentMetrics(previousStart, startDate)[0] != null
                ? ((BigDecimal) courierPaymentRepository.getCourierPaymentMetrics(previousStart, startDate)[0])
                        .multiply(platformMargin)
                : BigDecimal.ZERO;
        BigDecimal previousRevenue = previousCommission.add(previousDeliveryFees);
        BigDecimal growthRate = mapper.calculateGrowthRate(totalRevenue, previousRevenue);

        // Build revenue breakdown
        ContributionMarginDto.RevenueBreakdownDto revenueBreakdown = mapper.toRevenueBreakdownDto(
                commissionRevenue,
                mapper.calculatePercentage(commissionRevenue, totalRevenue),
                deliveryFeeRevenue,
                mapper.calculatePercentage(deliveryFeeRevenue, totalRevenue),
                BigDecimal.ZERO,
                BigDecimal.ZERO);

        // Build cost breakdown
        ContributionMarginDto.CostBreakdownDto costBreakdown = mapper.toCostBreakdownDto(
                courierBasePay,
                courierBonuses.add(incentives),
                courierTips,
                promotionCosts,
                giftCardCosts,
                referralCosts);

        // Build daily trend
        List<ContributionMarginDto.DailyContributionMarginDto> dailyTrend = new ArrayList<>();
        if (Boolean.TRUE.equals(request.getIncludeDailyTrend())) {
            List<Object[]> commissionDaily = commissionRepository.getDailyCommissionTrend(startDate, endDate);
            List<Object[]> deliveryDaily = courierPaymentRepository.getDailyCourierPaymentTrend(startDate, endDate);
            List<Object[]> promotionDaily = promotionUsageRepository.getDailyPromotionTrend(startDate, endDate);

            // Merge daily data (simplified - in practice would use a more sophisticated approach)
            for (int i = 0; i < commissionDaily.size(); i++) {
                Object[] commRow = commissionDaily.get(i);
                LocalDate date = ((java.sql.Date) commRow[0]).toLocalDate();
                BigDecimal dailyGmv = (BigDecimal) commRow[1];
                BigDecimal dailyCommission = (BigDecimal) commRow[1];
                Long dailyOrders = ((Number) commRow[2]).longValue();

                BigDecimal dailyDelivery = i < deliveryDaily.size() ? (BigDecimal) deliveryDaily.get(i)[1] : BigDecimal.ZERO;
                BigDecimal dailyPromo = i < promotionDaily.size() ? (BigDecimal) promotionDaily.get(i)[2] : BigDecimal.ZERO;

                BigDecimal dailyRevenue = dailyCommission.add(dailyDelivery.multiply(platformMargin));
                BigDecimal dailyCosts = dailyDelivery.add(dailyPromo);
                BigDecimal dailyCm = dailyRevenue.subtract(dailyCosts);
                BigDecimal dailyMarginPct = dailyGmv.compareTo(BigDecimal.ZERO) > 0
                        ? dailyCm.divide(dailyGmv, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;
                BigDecimal dailyUe = dailyOrders > 0
                        ? dailyCm.divide(BigDecimal.valueOf(dailyOrders), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                dailyTrend.add(mapper.toDailyContributionMarginDto(
                        date, dailyGmv, dailyRevenue, dailyCosts, dailyCm, dailyMarginPct, dailyOrders, dailyUe));
            }
        }

        return ContributionMarginDto.builder()
                .gmv(gmv)
                .commissionRevenue(commissionRevenue)
                .deliveryFeeRevenue(deliveryFeeRevenue)
                .courierCosts(totalCourierCosts)
                .promotionCosts(totalPromotionCosts)
                .totalRevenue(totalRevenue)
                .totalVariableCosts(totalVariableCosts)
                .contributionMargin(contributionMargin)
                .contributionMarginPercentage(contributionMarginPercentage)
                .orderCount(orderCount)
                .unitEconomics(unitEconomics)
                .revenuePerOrder(revenuePerOrder)
                .variableCostPerOrder(variableCostPerOrder)
                .breakEvenOrderValue(breakEvenOrderValue)
                .growthRate(growthRate)
                .revenueBreakdown(revenueBreakdown)
                .costBreakdown(costBreakdown)
                .dailyTrend(dailyTrend)
                .periodStart(startDate)
                .periodEnd(endDate)
                .build();
    }

    @Override
    public FinancialSummaryDto getFinancialSummary(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Getting financial summary for period: {} to {}", startDate, endDate);

        // Build request for calculations
        FinancialMetricsRequest request = FinancialMetricsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .includeDailyTrend(false)
                .includeRestaurantBreakdown(false)
                .includeCourierBreakdown(false)
                .build();

        // Get key metrics
        GmvMetricsDto gmvMetrics = calculateGmvMetrics(request);
        CommissionMetricsDto commissionMetrics = calculateCommissionMetrics(request);
        DeliveryFeeMetricsDto deliveryFeeMetrics = calculateDeliveryFeeMetrics(request);
        PromotionMetricsDto promotionMetrics = calculatePromotionMetrics(request);
        ContributionMarginDto marginMetrics = calculateContributionMargin(request);

        // Get pending payouts
        BigDecimal restaurantPayoutsPending = restaurantPayoutRepository.getTotalByStatus(PaymentStatus.PENDING);
        BigDecimal courierPayoutsPending = courierPaymentRepository.getTotalByStatus(
                PaymentStatus.PENDING, startDate, endDate);

        return FinancialSummaryDto.builder()
                .gmv(gmvMetrics.getTotalGmv())
                .gmvGrowthRate(gmvMetrics.getGrowthRate())
                .totalRevenue(marginMetrics.getTotalRevenue())
                .revenueGrowthRate(marginMetrics.getGrowthRate())
                .commissionRevenue(commissionMetrics.getTotalCommission())
                .effectiveCommissionRate(commissionMetrics.getEffectiveCommissionRate())
                .deliveryFeeRevenue(deliveryFeeMetrics.getNetDeliveryFeeRevenue())
                .promotionCosts(promotionMetrics.getPlatformCost())
                .courierCosts(marginMetrics.getCourierCosts())
                .contributionMargin(marginMetrics.getContributionMargin())
                .contributionMarginPercentage(marginMetrics.getContributionMarginPercentage())
                .unitEconomics(marginMetrics.getUnitEconomics())
                .totalOrders(gmvMetrics.getTotalOrders())
                .averageOrderValue(gmvMetrics.getAverageOrderValue())
                .restaurantPayoutsPending(restaurantPayoutsPending)
                .courierPayoutsPending(courierPayoutsPending)
                .periodStart(startDate)
                .periodEnd(endDate)
                .build();
    }

    @Override
    public GmvMetricsDto getGmvByRestaurants(LocalDateTime startDate, LocalDateTime endDate, List<Long> restaurantIds) {
        log.debug("Getting GMV for restaurants: {} for period: {} to {}", restaurantIds, startDate, endDate);

        FinancialMetricsRequest request = FinancialMetricsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .restaurantIds(restaurantIds)
                .includeRestaurantBreakdown(true)
                .includeDailyTrend(true)
                .build();

        return calculateGmvMetrics(request);
    }

    @Override
    public RestaurantPayoutMetricsDto getRestaurantPayoutDetails(Long restaurantId,
                                                                  LocalDateTime startDate,
                                                                  LocalDateTime endDate) {
        log.debug("Getting payout details for restaurant: {} for period: {} to {}",
                restaurantId, startDate, endDate);

        FinancialMetricsRequest request = FinancialMetricsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .restaurantIds(List.of(restaurantId))
                .includeRestaurantBreakdown(true)
                .includeDailyTrend(true)
                .build();

        return calculateRestaurantPayoutMetrics(request);
    }

    @Override
    public CourierPayoutMetricsDto getCourierPayoutDetails(Long courierId,
                                                           LocalDateTime startDate,
                                                           LocalDateTime endDate) {
        log.debug("Getting payout details for courier: {} for period: {} to {}",
                courierId, startDate, endDate);

        FinancialMetricsRequest request = FinancialMetricsRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .courierIds(List.of(courierId))
                .includeCourierBreakdown(true)
                .includeDailyTrend(true)
                .build();

        return calculateCourierPayoutMetrics(request);
    }
}
