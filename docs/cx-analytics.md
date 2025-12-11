# Customer Experience (CX) Analytics Module

## Overview

The CX Analytics module provides comprehensive metrics for measuring and analyzing customer satisfaction across the food delivery platform. It covers Net Promoter Score (NPS), ratings for restaurants, couriers, and app stores, as well as support ticket analytics.

## Architecture

```
analytics.cx/
├── controller/
│   └── CustomerExperienceAnalyticsController.java
├── service/
│   ├── CustomerExperienceAnalyticsService.java
│   └── CustomerExperienceAnalyticsServiceImpl.java
├── collector/
│   ├── NpsCollector.java
│   ├── RatingCollector.java
│   └── SupportTicketCollector.java
├── repository/
│   ├── NpsSurveyRepository.java
│   ├── RestaurantRatingRepository.java
│   ├── CourierRatingRepository.java
│   ├── AppStoreRatingRepository.java
│   └── SupportTicketRepository.java
├── model/
│   ├── NpsSurvey.java
│   ├── RestaurantRating.java
│   ├── CourierRating.java
│   ├── AppStoreRating.java
│   └── SupportTicket.java
├── dto/
│   ├── NpsMetricsDto.java
│   ├── RestaurantRatingMetricsDto.java
│   ├── CourierRatingMetricsDto.java
│   ├── AppStoreRatingMetricsDto.java
│   ├── SupportTicketMetricsDto.java
│   └── CxSummaryDto.java
├── mapper/
│   └── CxMapper.java
└── config/
    └── CxCacheConfig.java
```

## Metrics Definitions

### NPS (Net Promoter Score)

NPS measures customer loyalty and satisfaction using a 0-10 scale.

**Formula:**
```
NPS = ((Promoters - Detractors) / Total Responses) × 100
```

**Score Categories:**
- **Promoters (9-10)**: Loyal customers who will refer others
- **Passives (7-8)**: Satisfied but not enthusiastic customers
- **Detractors (0-6)**: Unhappy customers who may damage the brand

**NPS Score Interpretation:**
| Score Range | Interpretation |
|-------------|----------------|
| 70 to 100   | Excellent      |
| 50 to 69    | Great          |
| 0 to 49     | Good           |
| -100 to -1  | Needs Improvement |

**Available Metrics:**
- `npsScore`: The calculated NPS score (-100 to 100)
- `totalResponses`: Total number of survey responses
- `promotersCount/Percentage`: Count and percentage of promoters
- `passivesCount/Percentage`: Count and percentage of passives
- `detractorsCount/Percentage`: Count and percentage of detractors
- `scoreDistribution`: Distribution of scores 0-10
- `npsBySegment`: NPS broken down by user segment
- `npsByChannel`: NPS broken down by survey channel
- `npsTrend`: Daily NPS trend data

### Restaurant Ratings

Customer ratings for restaurants on a 1-5 star scale.

**Available Metrics:**
- `averageRating`: Overall average rating (1-5)
- `ratingCount`: Total number of ratings
- `distribution`: Distribution of ratings 1-5
- `foodQualityAvg`: Average food quality sub-rating
- `portionSizeAvg`: Average portion size sub-rating
- `valueForMoneyAvg`: Average value for money sub-rating
- `topRatedRestaurants`: List of highest-rated restaurants
- `lowestRatedRestaurants`: List of lowest-rated restaurants
- `ratingTrend`: Daily rating trend data

### Courier Ratings

Customer ratings for delivery couriers on a 1-5 star scale.

**Available Metrics:**
- `averageRating`: Overall average rating (1-5)
- `ratingCount`: Total number of ratings
- `distribution`: Distribution of ratings 1-5
- `professionalismAvg`: Average professionalism sub-rating
- `communicationAvg`: Average communication sub-rating
- `timelinessAvg`: Average timeliness sub-rating
- `avgTipAmount`: Average tip amount given
- `tipRate`: Percentage of orders with tips
- `topRatedCouriers`: List of highest-rated couriers
- `ratingTrend`: Daily rating trend data

### App Store Ratings

Ratings from iOS App Store and Google Play.

**Available Metrics:**
- `overallAverageRating`: Combined average rating
- `totalReviewCount`: Total number of reviews
- `iosPlatform`: iOS-specific metrics
  - `averageRating`: iOS average rating
  - `ratingCount`: Number of iOS ratings
  - `distribution`: iOS rating distribution
- `androidPlatform`: Android-specific metrics
  - `averageRating`: Android average rating
  - `ratingCount`: Number of Android ratings
  - `distribution`: Android rating distribution
- `ratingsByVersion`: Ratings broken down by app version
- `ratingsByCountry`: Ratings broken down by country
- `sentimentSummary`: Sentiment analysis summary
- `ratingTrend`: Daily rating trend data

### Support Tickets

Customer support ticket metrics with SLA tracking.

**Volume Metrics:**
- `totalTickets`: Total tickets in period
- `openTickets`: Currently open tickets
- `inProgressTickets`: Tickets being worked on
- `resolvedTickets`: Resolved tickets
- `closedTickets`: Closed tickets
- `ticketsPerDay/Week/Month`: Volume rates
- `ticketsByType`: Distribution by ticket type
- `ticketsByStatus`: Distribution by status
- `ticketsByPriority`: Distribution by priority
- `ticketsByChannel`: Distribution by channel

**Ticket Types:**
- `LATE_DELIVERY`: Delivery delays
- `MISSING_ITEMS`: Missing order items
- `WRONG_ORDER`: Incorrect order received
- `COLD_FOOD`: Food quality issues
- `REFUND_REQUEST`: Refund requests
- `PAYMENT_ISSUE`: Payment problems
- `ACCOUNT_ISSUE`: Account-related issues
- `PROMO_CODE_ISSUE`: Promotion code problems
- `APP_BUG`: Application bugs
- `RESTAURANT_COMPLAINT`: Restaurant complaints
- `COURIER_COMPLAINT`: Courier complaints
- `OTHER`: Other issues

**Performance Metrics:**
- `avgResolutionTimeHours`: Average time to resolve
- `medianResolutionTimeHours`: Median resolution time
- `p90ResolutionTimeHours`: 90th percentile resolution time
- `avgFirstResponseTimeMinutes`: Average time to first response
- `resolutionRate`: Percentage of tickets resolved
- `reopenRate`: Percentage of tickets reopened
- `escalationRate`: Percentage of tickets escalated

**SLA Metrics:**
- `slaHours`: Configured SLA target
- `ticketsWithinSla`: Tickets resolved within SLA
- `ticketsBreachedSla`: Tickets that breached SLA
- `slaComplianceRate`: Percentage within SLA
- `currentlyBreached`: Currently breached open tickets
- `atRiskOfBreach`: Tickets at risk of breaching SLA
- `slaBreachesByType`: Breaches by ticket type
- `slaBreachesByPriority`: Breaches by priority

**CSAT Metrics:**
- `avgCsatScore`: Average customer satisfaction score (1-5)
- `csatResponseCount`: Number of CSAT responses
- `csatResponseRate`: CSAT response rate
- `csatDistribution`: Distribution of CSAT scores
- `satisfiedRate`: Percentage satisfied (4-5)
- `dissatisfiedRate`: Percentage dissatisfied (1-2)

**Agent Performance:**
- `agentId`: Agent identifier
- `ticketsHandled`: Total tickets handled
- `ticketsResolved`: Tickets resolved by agent
- `avgResolutionTimeHours`: Agent's average resolution time
- `avgFirstResponseMinutes`: Agent's average response time
- `csatScore`: Agent's CSAT score
- `slaBreaches`: Agent's SLA breaches
- `resolutionRate`: Agent's resolution rate

### CX Summary

Comprehensive summary combining all CX metrics.

**Overall Score Calculation:**
```
CX Score = (NPS × 30%) + (Ratings × 40%) + (Support × 30%)
```

Where:
- NPS is normalized from -100..100 to 0..100
- Ratings are normalized from 1..5 to 0..100
- Support is based on SLA compliance and CSAT

**CX Status Levels:**
| Score Range | Status    |
|-------------|-----------|
| 80-100      | EXCELLENT |
| 60-79       | GOOD      |
| 40-59       | FAIR      |
| 20-39       | POOR      |
| 0-19        | CRITICAL  |

## API Endpoints

### NPS Metrics
```
GET /api/v1/analytics/cx/nps
```
**Parameters:**
- `startDate` (required): ISO datetime
- `endDate` (required): ISO datetime
- `includeDistribution` (default: true): Include score distribution
- `includeTrend` (default: false): Include daily trend
- `includeSegments` (default: false): Include segment breakdowns

### Restaurant Ratings
```
GET /api/v1/analytics/cx/ratings/restaurant
GET /api/v1/analytics/cx/ratings/restaurant/{restaurantId}
```
**Parameters:**
- `startDate` (required): ISO datetime
- `endDate` (required): ISO datetime
- `includeDistribution` (default: true): Include rating distribution
- `includeTrend` (default: false): Include daily trend
- `includeTopRestaurants` (default: false): Include top/bottom performers

### Courier Ratings
```
GET /api/v1/analytics/cx/ratings/courier
GET /api/v1/analytics/cx/ratings/courier/{courierId}
```
**Parameters:**
- `startDate` (required): ISO datetime
- `endDate` (required): ISO datetime
- `includeDistribution` (default: true): Include rating distribution
- `includeTrend` (default: false): Include daily trend
- `includeTopCouriers` (default: false): Include top/bottom performers

### App Store Ratings
```
GET /api/v1/analytics/cx/ratings/app-store
```
**Parameters:**
- `startDate` (required): ISO datetime
- `endDate` (required): ISO datetime
- `includeVersionBreakdown` (default: false): Include version breakdown
- `includeCountryBreakdown` (default: false): Include country breakdown
- `includeTrend` (default: false): Include daily trend

### Support Tickets
```
GET /api/v1/analytics/cx/support-tickets
```
**Parameters:**
- `startDate` (required): ISO datetime
- `endDate` (required): ISO datetime
- `slaHours` (default: 24): SLA hours for compliance calculation
- `includeTrend` (default: false): Include daily trend
- `includeAgentPerformance` (default: false): Include agent metrics

### CX Summary
```
GET /api/v1/analytics/cx/summary
```
**Parameters:**
- `startDate` (required): ISO datetime
- `endDate` (required): ISO datetime

### Cache Management
```
POST /api/v1/analytics/cx/cache/invalidate/nps
POST /api/v1/analytics/cx/cache/invalidate/ratings
POST /api/v1/analytics/cx/cache/invalidate/support-tickets
POST /api/v1/analytics/cx/cache/invalidate/all
```

## Caching Strategy

| Cache | TTL | Description |
|-------|-----|-------------|
| NPS Metrics | 10 min | NPS data changes infrequently |
| Restaurant Ratings | 5 min | Moderate change frequency |
| Courier Ratings | 5 min | Moderate change frequency |
| App Store Ratings | 5 min | External data, moderate updates |
| Support Tickets | 1 min | Real-time operational data |
| CX Summary | 5 min | Aggregate of all metrics |

Cache invalidation happens automatically when new data is submitted through the respective APIs. Manual cache invalidation is available through the cache management endpoints.

## Database Schema

### Tables

**nps_surveys**
- Survey responses with NPS scores (0-10)
- Indexed on: user_id, score, created_at, survey_channel

**restaurant_ratings**
- Restaurant ratings with sub-ratings
- Indexed on: restaurant_id, score, created_at

**courier_ratings**
- Courier ratings with sub-ratings
- Indexed on: courier_id, score, created_at

**app_store_ratings**
- App store reviews from iOS and Android
- Indexed on: platform, score, created_at, country

**support_tickets**
- Support tickets with full lifecycle tracking
- Indexed on: type, status, priority, created_at, sla_due_at

## Example Usage

### Get NPS Metrics for Last 30 Days
```bash
curl -X GET "http://localhost:8080/api/v1/analytics/cx/nps?\
startDate=2024-01-01T00:00:00&\
endDate=2024-01-31T23:59:59&\
includeDistribution=true&\
includeTrend=true"
```

### Get Support Ticket Metrics with Custom SLA
```bash
curl -X GET "http://localhost:8080/api/v1/analytics/cx/support-tickets?\
startDate=2024-01-01T00:00:00&\
endDate=2024-01-31T23:59:59&\
slaHours=12&\
includeAgentPerformance=true"
```

### Get CX Summary
```bash
curl -X GET "http://localhost:8080/api/v1/analytics/cx/summary?\
startDate=2024-01-01T00:00:00&\
endDate=2024-01-31T23:59:59"
```

## Best Practices

1. **Date Range Selection**: Use appropriate date ranges for meaningful analysis. Weekly or monthly periods work best for trend analysis.

2. **SLA Configuration**: Set SLA hours based on business requirements. Default is 24 hours, but high-priority tickets may need shorter SLAs.

3. **Cache Usage**: Use cache invalidation sparingly. The TTLs are designed to balance freshness with performance.

4. **Agent Performance**: Review agent metrics to identify training needs and recognize top performers.

5. **NPS Trends**: Monitor NPS trends weekly to identify changes in customer sentiment early.
