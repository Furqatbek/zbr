# Financial Analytics Module

## Overview

The Financial Analytics module provides comprehensive financial metrics and analytics for the food delivery platform. It calculates key financial indicators including GMV, commission revenue, delivery fees, promotions, payouts, and contribution margin.

## Key Metrics

### 1. Gross Merchandise Value (GMV)

**Formula:**
```
GMV = Sum of all order values before deductions
```

**Components:**
- Food GMV: Value of food items ordered
- Delivery Fee GMV: Delivery fees charged to customers
- Tip GMV: Tips received by couriers

**Example:**
```json
{
  "totalGmv": 100000.00,
  "totalOrders": 500,
  "averageOrderValue": 200.00,
  "foodGmv": 85000.00,
  "deliveryFeeGmv": 10000.00,
  "tipGmv": 5000.00,
  "growthRate": 15.5
}
```

### 2. Commission Revenue

**Formula:**
```
Commission = Platform fee charged to restaurants per order
           = Order Subtotal × Commission Rate (percentage)
           + Fixed Commission (if applicable)
```

**Commission Types:**
- `PERCENTAGE`: Based on order value (e.g., 15% of order subtotal)
- `FIXED`: Fixed amount per order (e.g., $2.00 per order)
- `HYBRID`: Combination of percentage and fixed
- `TIERED`: Variable rates based on volume or tier
- `PROMOTIONAL`: Special rates during promotions

**Example:**
```json
{
  "totalCommission": 15000.00,
  "orderCount": 500,
  "averageCommissionPerOrder": 30.00,
  "effectiveCommissionRate": 0.15,
  "percentageBasedCommission": 12000.00,
  "fixedCommission": 3000.00
}
```

### 3. Delivery Fee Metrics

**Formulas:**
```
Net Delivery Fee Revenue = Fees Collected from Customers - Fees Paid to Couriers
Average Delivery Fee = Total Fees Collected / Number of Deliveries
```

**Components:**
- Fees collected from customers
- Base payments to couriers
- Distance bonuses
- Peak hour surcharges
- Tips (pass-through)

**Example:**
```json
{
  "totalDeliveryFeesCollected": 10000.00,
  "totalDeliveryFeesPaid": 8500.00,
  "netDeliveryFeeRevenue": 1500.00,
  "deliveryCount": 1000,
  "averageDeliveryFee": 10.00,
  "totalDistanceBonus": 500.00,
  "totalPeakSurcharge": 300.00
}
```

### 4. Promotion Metrics

**Formulas:**
```
Total Discount = Sum of all discounts applied to orders
Platform Cost = Discounts funded by the platform
Restaurant Cost = Discounts funded by restaurants
Promotion Usage Rate = (Promoted Orders / Total Orders) × 100
```

**Promotion Types:**
- `PERCENTAGE_DISCOUNT`: e.g., 20% off
- `FIXED_DISCOUNT`: e.g., $5 off
- `FREE_DELIVERY`: Waived delivery fee
- `BOGO`: Buy one get one
- `CASHBACK`: Money back after purchase
- `FIRST_ORDER`: New customer discounts
- `LOYALTY_REWARD`: Points-based rewards

**Funding Types:**
- `PLATFORM`: 100% funded by the platform
- `RESTAURANT`: 100% funded by the restaurant
- `SHARED`: Split between platform and restaurant

**Example:**
```json
{
  "totalDiscountValue": 8000.00,
  "platformCost": 5000.00,
  "restaurantCost": 3000.00,
  "promotedOrderCount": 400,
  "totalOrderCount": 500,
  "promotionUsageRate": 80.00,
  "averageDiscountPerOrder": 20.00
}
```

### 5. Restaurant Payout Metrics

**Formula:**
```
Net Payout = Gross Sales
           - Commission Deducted
           - Delivery Subsidies
           - Promotion Costs
           + Adjustments
           - Fees
```

**Example:**
```json
{
  "totalGrossSales": 100000.00,
  "totalCommissionsDeducted": 15000.00,
  "totalDeliverySubsidies": 1000.00,
  "totalPromotionCosts": 3000.00,
  "totalAdjustments": 500.00,
  "totalFees": 200.00,
  "netPayoutAmount": 81300.00,
  "restaurantCount": 50,
  "averagePayoutPerRestaurant": 1626.00
}
```

### 6. Courier Payout Metrics

**Formula:**
```
Courier Payout = Base Pay
               + Distance Bonus
               + Tips
               + Peak Bonus
               + Incentives/Bonuses
```

**Bonus Types:**
- `SIGNUP_BONUS`: New courier bonus
- `COMPLETION_BONUS`: Delivery completion incentives
- `STREAK_BONUS`: Consecutive delivery bonuses
- `PEAK_HOUR_BONUS`: Busy period incentives
- `REFERRAL_BONUS`: For referring other couriers
- `PERFORMANCE_BONUS`: High rating rewards
- `QUEST_BONUS`: Challenge completion rewards
- `RETENTION_BONUS`: Loyalty incentives

**Example:**
```json
{
  "totalBasePayments": 35000.00,
  "totalDistanceBonuses": 5000.00,
  "totalTips": 8000.00,
  "totalPeakBonuses": 2000.00,
  "totalIncentives": 5000.00,
  "totalPayouts": 55000.00,
  "activeCourierCount": 100,
  "averagePayoutPerDelivery": 55.00,
  "tipRate": 70.00
}
```

### 7. Contribution Margin

**Formulas:**
```
Total Revenue = Commission Revenue + Delivery Fee Revenue
Total Variable Costs = Courier Costs + Promotion Costs
Contribution Margin = Total Revenue - Total Variable Costs
Contribution Margin % = (Contribution Margin / GMV) × 100
Unit Economics = Contribution Margin / Number of Orders
Break-Even Order Value = Variable Cost per Order / Effective Commission Rate
```

**Example:**
```json
{
  "gmv": 100000.00,
  "commissionRevenue": 15000.00,
  "deliveryFeeRevenue": 1500.00,
  "courierCosts": 8500.00,
  "promotionCosts": 5000.00,
  "totalRevenue": 16500.00,
  "totalVariableCosts": 13500.00,
  "contributionMargin": 3000.00,
  "contributionMarginPercentage": 3.00,
  "orderCount": 500,
  "unitEconomics": 6.00,
  "revenuePerOrder": 33.00,
  "variableCostPerOrder": 27.00,
  "breakEvenOrderValue": 180.00
}
```

## API Endpoints

### POST /api/v1/analytics/financial/gmv
Calculate GMV metrics for a specified period.

### POST /api/v1/analytics/financial/commission
Calculate commission revenue metrics.

### POST /api/v1/analytics/financial/delivery-fees
Calculate delivery fee metrics.

### POST /api/v1/analytics/financial/promotions
Calculate promotion and discount metrics.

### POST /api/v1/analytics/financial/restaurant-payouts
Calculate restaurant payout metrics.

### POST /api/v1/analytics/financial/courier-payouts
Calculate courier payout metrics.

### POST /api/v1/analytics/financial/contribution-margin
Calculate contribution margin and unit economics.

### GET /api/v1/analytics/financial/summary
Get high-level financial summary.

### GET /api/v1/analytics/financial/restaurants/{restaurantId}/payouts
Get payout details for a specific restaurant.

### GET /api/v1/analytics/financial/couriers/{courierId}/payouts
Get payout details for a specific courier.

## Request Format

```json
{
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-01-31T23:59:59",
  "restaurantIds": [1, 2, 3],
  "courierIds": [10, 20],
  "zoneIds": [100],
  "categories": ["Italian", "Chinese"],
  "includeDailyTrend": true,
  "includeRestaurantBreakdown": true,
  "includeCourierBreakdown": false,
  "topN": 10,
  "compareToPreviousPeriod": true
}
```

## Caching Configuration

| Metric | Cache TTL | Rationale |
|--------|-----------|-----------|
| GMV | 60s | Real-time critical |
| Commission | 300s | Moderate update frequency |
| Delivery Fees | 120s | Near real-time |
| Promotions | 300s | Moderate update frequency |
| Restaurant Payouts | 300s | Batch processing |
| Courier Payouts | 300s | Batch processing |
| Contribution Margin | 60s | Real-time critical |

## Database Schema

### restaurant_commissions
Tracks commission earned per order.

### courier_payments
Tracks payments to couriers for deliveries.

### courier_bonuses
Tracks incentives and bonuses for couriers.

### promotion_usages
Tracks promotion/discount usage with cost attribution.

### restaurant_payouts
Tracks periodic payouts to restaurants.

### payout_disputes
Tracks disputes raised on payouts.

### gift_card_usages
Tracks gift card redemptions.

### referral_rewards
Tracks referral program rewards.

## Architecture

```
analytics.financial/
├── controller/
│   └── FinancialAnalyticsController.java
├── service/
│   ├── FinancialAnalyticsService.java
│   └── impl/
│       └── FinancialAnalyticsServiceImpl.java
├── repository/
│   ├── RestaurantCommissionRepository.java
│   ├── CourierPaymentRepository.java
│   ├── CourierBonusRepository.java
│   ├── PromotionUsageRepository.java
│   ├── RestaurantPayoutRepository.java
│   ├── PayoutDisputeRepository.java
│   ├── GiftCardUsageRepository.java
│   └── ReferralRewardRepository.java
├── dto/
│   ├── GmvMetricsDto.java
│   ├── CommissionMetricsDto.java
│   ├── DeliveryFeeMetricsDto.java
│   ├── PromotionMetricsDto.java
│   ├── RestaurantPayoutMetricsDto.java
│   ├── CourierPayoutMetricsDto.java
│   ├── ContributionMarginDto.java
│   ├── FinancialSummaryDto.java
│   └── FinancialMetricsRequest.java
├── model/
│   ├── RestaurantCommission.java
│   ├── CourierPayment.java
│   ├── CourierBonus.java
│   ├── PromotionUsage.java
│   ├── RestaurantPayout.java
│   ├── PayoutDispute.java
│   ├── GiftCardUsage.java
│   ├── ReferralReward.java
│   └── enums/
├── mapper/
│   └── FinancialMetricsMapper.java
├── config/
│   └── FinancialCacheConfig.java
└── util/
```

## Best Practices

1. **Query Optimization**: Use indexed columns for filtering
2. **Caching**: Leverage Redis for frequently accessed metrics
3. **Pagination**: Use `topN` parameter for large result sets
4. **Date Ranges**: Always specify appropriate date ranges
5. **Aggregations**: Use repository methods with native queries for complex aggregations
