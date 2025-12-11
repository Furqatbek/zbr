package com.fooddelivery.analytics.financial.mapper;

import com.fooddelivery.analytics.financial.dto.*;
import com.fooddelivery.analytics.financial.model.*;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MapStruct mapper for financial analytics DTOs.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        imports = {BigDecimal.class, RoundingMode.class, LocalDate.class, LocalDateTime.class})
public interface FinancialMetricsMapper {

    // ============== Commission Mappings ==============

    @Mapping(target = "commissionType", source = "commissionType")
    @Mapping(target = "totalAmount", source = "totalAmount")
    @Mapping(target = "orderCount", source = "orderCount")
    CommissionMetricsDto.CommissionByTypeDto toCommissionByTypeDto(
            CommissionType commissionType, BigDecimal totalAmount, Long orderCount, BigDecimal percentageOfTotal);

    @Mapping(target = "restaurantId", source = "restaurantId")
    @Mapping(target = "totalCommission", source = "totalCommission")
    CommissionMetricsDto.RestaurantCommissionDto toRestaurantCommissionDto(
            Long restaurantId, String restaurantName, BigDecimal totalCommission,
            BigDecimal commissionRate, Long orderCount, BigDecimal averageOrderValue);

    @Mapping(target = "date", source = "date")
    @Mapping(target = "commission", source = "commission")
    CommissionMetricsDto.DailyCommissionDto toDailyCommissionDto(
            LocalDate date, BigDecimal commission, Long orderCount, BigDecimal averageCommission);

    // ============== Delivery Fee Mappings ==============

    @Mapping(target = "distanceRange", source = "distanceRange")
    DeliveryFeeMetricsDto.DeliveryFeeByDistanceDto toDeliveryFeeByDistanceDto(
            String distanceRange, BigDecimal totalFees, Long deliveryCount,
            BigDecimal averageFee, BigDecimal percentageOfTotal);

    @Mapping(target = "timeSlot", source = "timeSlot")
    DeliveryFeeMetricsDto.DeliveryFeeByTimeDto toDeliveryFeeByTimeDto(
            String timeSlot, BigDecimal totalFees, Long deliveryCount,
            BigDecimal averageFee, Boolean isPeakHour, BigDecimal peakSurchargeAmount);

    @Mapping(target = "date", source = "date")
    DeliveryFeeMetricsDto.DailyDeliveryFeeDto toDailyDeliveryFeeDto(
            LocalDate date, BigDecimal feesCollected, BigDecimal feesPaid,
            BigDecimal netRevenue, Long deliveryCount);

    // ============== Promotion Mappings ==============

    @Mapping(target = "promotionType", source = "promotionType")
    PromotionMetricsDto.PromotionByTypeDto toPromotionByTypeDto(
            PromotionType promotionType, BigDecimal totalValue, Long usageCount,
            BigDecimal averageDiscount, BigDecimal percentageOfTotal);

    @Mapping(target = "funder", source = "funder")
    PromotionMetricsDto.PromotionByFunderDto toPromotionByFunderDto(
            PromotionFunder funder, BigDecimal totalCost, Long promotionCount, BigDecimal percentageOfTotal);

    @Mapping(target = "promotionId", source = "promotionId")
    PromotionMetricsDto.TopPromotionDto toTopPromotionDto(
            Long promotionId, String promotionCode, String promotionName, PromotionType type,
            BigDecimal totalDiscountGiven, Long usageCount, BigDecimal platformCost, BigDecimal restaurantCost);

    @Mapping(target = "date", source = "date")
    PromotionMetricsDto.DailyPromotionDto toDailyPromotionDto(
            LocalDate date, BigDecimal totalDiscount, BigDecimal platformCost,
            BigDecimal restaurantCost, Long promotedOrders, BigDecimal usageRate);

    // ============== Restaurant Payout Mappings ==============

    @Mapping(target = "status", source = "status")
    RestaurantPayoutMetricsDto.PayoutByStatusDto toPayoutByStatusDto(
            PaymentStatus status, BigDecimal totalAmount, Long payoutCount, BigDecimal percentageOfTotal);

    @Mapping(target = "restaurantId", source = "restaurantId")
    RestaurantPayoutMetricsDto.RestaurantPayoutDetailDto toRestaurantPayoutDetailDto(
            Long restaurantId, String restaurantName, BigDecimal grossSales,
            BigDecimal commissionsDeducted, BigDecimal netPayout, Long orderCount,
            BigDecimal effectiveCommissionRate);

    @Mapping(target = "status", source = "status")
    RestaurantPayoutMetricsDto.DisputeSummaryDto toDisputeSummaryDto(
            DisputeStatus status, Long count, BigDecimal totalAmount, BigDecimal averageResolutionDays);

    @Mapping(target = "date", source = "date")
    RestaurantPayoutMetricsDto.DailyPayoutDto toDailyPayoutDto(
            LocalDate date, BigDecimal grossSales, BigDecimal netPayouts,
            Long restaurantCount, Long disputeCount);

    // ============== Courier Payout Mappings ==============

    @Mapping(target = "paymentType", source = "paymentType")
    CourierPayoutMetricsDto.PayoutByTypeDto toCourierPayoutByTypeDto(
            CourierPaymentType paymentType, BigDecimal totalAmount, Long count, BigDecimal percentageOfTotal);

    @Mapping(target = "bonusType", source = "bonusType")
    CourierPayoutMetricsDto.BonusByTypeDto toBonusByTypeDto(
            BonusType bonusType, BigDecimal totalAmount, Long recipientCount, BigDecimal averageBonus);

    @Mapping(target = "status", source = "status")
    CourierPayoutMetricsDto.PayoutByStatusDto toCourierPayoutByStatusDto(
            PaymentStatus status, BigDecimal totalAmount, Long count, BigDecimal percentageOfTotal);

    @Mapping(target = "courierId", source = "courierId")
    CourierPayoutMetricsDto.CourierPayoutDetailDto toCourierPayoutDetailDto(
            Long courierId, String courierName, BigDecimal totalEarnings, BigDecimal baseEarnings,
            BigDecimal bonusEarnings, BigDecimal tipEarnings, Long deliveryCount, BigDecimal averagePerDelivery);

    @Mapping(target = "date", source = "date")
    CourierPayoutMetricsDto.DailyCourierPayoutDto toDailyCourierPayoutDto(
            LocalDate date, BigDecimal totalPayouts, BigDecimal basePayments, BigDecimal bonuses,
            BigDecimal tips, Long activeCouriers, Long deliveries);

    // ============== GMV Mappings ==============

    @Mapping(target = "restaurantId", source = "restaurantId")
    GmvMetricsDto.RestaurantGmvDto toRestaurantGmvDto(
            Long restaurantId, String restaurantName, BigDecimal gmv,
            Long orderCount, BigDecimal percentageOfTotal);

    @Mapping(target = "category", source = "category")
    GmvMetricsDto.CategoryGmvDto toCategoryGmvDto(
            String category, BigDecimal gmv, Long orderCount, BigDecimal percentageOfTotal);

    @Mapping(target = "date", source = "date")
    GmvMetricsDto.DailyGmvDto toDailyGmvDto(
            LocalDate date, BigDecimal gmv, Long orderCount, BigDecimal averageOrderValue);

    // ============== Contribution Margin Mappings ==============

    ContributionMarginDto.RevenueBreakdownDto toRevenueBreakdownDto(
            BigDecimal commissionRevenue, BigDecimal commissionPercentage,
            BigDecimal deliveryFeeRevenue, BigDecimal deliveryFeePercentage,
            BigDecimal otherRevenue, BigDecimal otherPercentage);

    ContributionMarginDto.CostBreakdownDto toCostBreakdownDto(
            BigDecimal courierBasePay, BigDecimal courierBonuses, BigDecimal courierTips,
            BigDecimal promotionCosts, BigDecimal giftCardCosts, BigDecimal referralCosts);

    @Mapping(target = "tier", source = "tier")
    ContributionMarginDto.UnitEconomicsByTierDto toUnitEconomicsByTierDto(
            String tier, Long orderCount, BigDecimal gmv, BigDecimal revenue,
            BigDecimal costs, BigDecimal contributionMargin, BigDecimal unitEconomics);

    @Mapping(target = "orderValueRange", source = "orderValueRange")
    ContributionMarginDto.UnitEconomicsByOrderValueDto toUnitEconomicsByOrderValueDto(
            String orderValueRange, Long orderCount, BigDecimal averageOrderValue,
            BigDecimal revenue, BigDecimal costs, BigDecimal contributionMargin,
            BigDecimal unitEconomics, Boolean profitable);

    @Mapping(target = "date", source = "date")
    ContributionMarginDto.DailyContributionMarginDto toDailyContributionMarginDto(
            LocalDate date, BigDecimal gmv, BigDecimal revenue, BigDecimal costs,
            BigDecimal contributionMargin, BigDecimal marginPercentage,
            Long orderCount, BigDecimal unitEconomics);

    // ============== Entity to DTO Mappings ==============

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "restaurantId", source = "entity.restaurantId")
    @Mapping(target = "orderId", source = "entity.orderId")
    @Mapping(target = "commissionAmount", source = "entity.commissionAmount")
    default CommissionMetricsDto.RestaurantCommissionDto fromEntity(RestaurantCommission entity, String restaurantName) {
        return CommissionMetricsDto.RestaurantCommissionDto.builder()
                .restaurantId(entity.getRestaurantId())
                .restaurantName(restaurantName)
                .totalCommission(entity.getCommissionAmount())
                .commissionRate(entity.getCommissionRate())
                .build();
    }

    // ============== Utility Methods ==============

    /**
     * Calculate percentage of total.
     */
    default BigDecimal calculatePercentage(BigDecimal value, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate growth rate.
     */
    default BigDecimal calculateGrowthRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0
                    ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    /**
     * Safe division with null handling.
     */
    default BigDecimal safeDivide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    /**
     * Safe division with custom scale.
     */
    default BigDecimal safeDivide(BigDecimal numerator, Long denominator, int scale) {
        if (denominator == null || denominator == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(BigDecimal.valueOf(denominator), scale, RoundingMode.HALF_UP);
    }
}
