# Fraud Analytics Rules and Scoring Matrix

## Overview

The Fraud Analytics module provides comprehensive fraud detection and risk assessment for the food delivery platform. This document details the fraud detection rules, risk scoring formulas, and configuration parameters.

## Risk Score Scale

All risk scores use a normalized 0-100 scale:

| Score Range | Risk Level | Action Required |
|------------|------------|-----------------|
| 0-30       | LOW        | Normal monitoring |
| 31-60      | MEDIUM     | Enhanced monitoring |
| 61-80      | HIGH       | Manual review required |
| 81-100     | CRITICAL   | Immediate action required |

---

## 1. Payment Fraud Detection

### 1.1 Payment Failure Risk Score

**Formula:**
```
score = min(100, base_score + failure_rate_penalty + volume_penalty + pattern_penalty)

where:
  base_score = 0
  failure_rate_penalty = min(failure_rate × 0.8, 40)
  volume_penalty = {30 if failed_count >= 10, 20 if >= 5, 10 if >= 3, else 0}
  pattern_penalty = {30 if rate >= 80% && total >= 5, 20 if rate >= 50% && total >= 3, else 0}
```

**Detection Rules:**
- Flag users with > 3 failed payments in 60 minutes
- Alert on failure rate > 50% with 5+ attempts
- Detect card testing: rapid small-amount transactions (< $1.00)

### 1.2 Card Testing Detection

**Indicators:**
- Multiple transactions < $1.00 within 15 minutes
- Different card BINs from same device
- High decline rate for small amounts
- Sequential card testing patterns

**Risk Score:**
```
score = small_amount_penalty + rapid_succession_bonus + decline_rate_bonus
where:
  small_amount_penalty = min(small_count × 5, 40)
  rapid_succession_bonus = 20 if rapid
  decline_rate_bonus = min(decline_rate × 0.4, 40)
```

---

## 2. Behavioral Fraud Detection

### 2.1 Order Velocity Analysis

**Formula:**
```
score = hourly_velocity_score + daily_velocity_score

where:
  hourly_velocity_score = {50 if orders/hour > 2×threshold, 30 if > threshold, else 0}
  daily_velocity_score = {50 if orders/day > 10×threshold, 30 if > 5×, else 0}
```

**Default Threshold:** 5 orders per hour

### 2.2 Order Value Anomaly Detection

Uses z-score statistical analysis:
```
z_score = (order_value - mean_value) / standard_deviation

Anomaly flags:
  z > 4.0: Critical anomaly (60 points)
  z > 3.0: High anomaly (45 points)
  z > 2.0: Moderate anomaly (30 points)
  z > 1.5: Minor anomaly (15 points)
```

### 2.3 Refund Abuse Detection

**Formula:**
```
score = refund_rate_score + volume_score

where:
  refund_rate_score = {
    50 if refund_rate >= 50%,
    35 if >= 30%,
    20 if >= 20%,
    10 if >= 10%,
    else 0
  }
  volume_score = {50 if refunds >= 10, 30 if >= 5, 15 if >= 3, else 0}
```

### 2.4 Address Fraud Detection

**Indicators:**
- Multiple users at same delivery address
- High order concentration at single address
- Address hash collisions across accounts

**Scoring:**
```
score = user_concentration + orders_per_user_score

where:
  user_concentration = {60 if users >= 10, 40 if >= 5, 20 if >= 3}
  orders_per_user = order_count / user_count
  orders_per_user_score = {40 if > 10, 25 if > 5, 10 if > 2}
```

---

## 3. Account Integrity Detection

### 3.1 Fake Account Detection

**Formula:**
```
score = age_score + promo_on_first_score

where:
  age_score = {
    60 if account_age < 1 hour,
    40 if < 6 hours,
    20 if < 24 hours,
    10 if < 48 hours,
    else 0
  }
  promo_on_first_score = 40 if promo_used_on_first_order
```

### 3.2 Multi-Account Detection

**Indicators:**
- Device fingerprint reuse across accounts
- IP address clusters
- Payment token sharing
- Phone number pattern matching

**Device Sharing Score:**
```
score = user_sharing_penalty + order_activity_bonus

where:
  user_sharing_penalty = {70 if users >= 10, 50 if >= 5, 30 if >= 3, 15 if >= 2}
  order_activity_bonus = {30 if orders >= 50, 20 if >= 20, 10 if >= 10}
```

### 3.3 Device Trust Scoring

**Formula:**
```
risk_score = trust_deficit + emulator_penalty + rooted_penalty

where:
  trust_deficit = (100 - trust_score) × 0.5
  emulator_penalty = 25 if is_emulator
  rooted_penalty = 25 if is_rooted
```

---

## 4. Referral Fraud Detection

### 4.1 Device-Based Referral Fraud

**Scoring:**
```
score = {
  95 if signups_from_device >= 10,
  75 if >= 5,
  50 if >= 3,
  30 if >= 2,
  else 0
}
```

### 4.2 IP-Based Referral Fraud

Lower threshold due to legitimate IP sharing (offices, universities):
```
score = {
  90 if signups_from_ip >= 20,
  65 if >= 10,
  40 if >= 5,
  20 if >= 3,
  else 0
}
```

### 4.3 Circular Referral Detection

Uses Depth-First Search (DFS) graph traversal to detect referral loops:
```
Algorithm: DFS with cycle detection
Base score: 70 (any circular referral is highly suspicious)
Additional penalties:
  +15 if chain_length >= 5
  +10 if chain_length >= 3
  +15 if users_involved >= 10
  +10 if users_involved >= 5
```

### 4.4 Promo Abuse Detection

**Formula:**
```
score = promo_rate_score + no_paid_orders_penalty

where:
  promo_rate_score = {
    60 if promo_rate >= 90%,
    40 if >= 70%,
    25 if >= 50%,
    10 if >= 30%,
    else 0
  }
  no_paid_orders_penalty = 40 if user_has_never_paid_full_price
```

---

## 5. Security Log Analysis

### 5.1 Login Security Score

**Formula:**
```
score = failed_count_score + failure_rate_score

where:
  failed_count_score = {50 if >= 20, 35 if >= 10, 20 if >= 5, 10 if >= 3}
  failure_rate_score = {50 if rate >= 80%, 35 if >= 50%, 20 if >= 30%}
```

### 5.2 Brute Force Detection

**Indicators:**
- High velocity login attempts
- Multiple unique passwords tried
- Single IP targeting multiple users (credential stuffing)

**Formula:**
```
score = velocity_score + attempt_score + dictionary_score

attempts_per_minute = attempt_count / time_window_minutes
velocity_score = {40 if apm >= 10, 30 if >= 5, 20 if >= 2, 10 if >= 1}
attempt_score = {30 if attempts >= 100, 20 if >= 50, 10 if >= 20}
dictionary_score = {30 if unique_passwords >= 50, 20 if >= 20, 10 if >= 10}
```

### 5.3 IP Threat Score

**Formula:**
```
score = anonymization_score + activity_score + multi_user_score

where:
  anonymization_score = {
    50 if is_tor,
    35 if is_proxy,
    20 if is_vpn,
    else 0
  }
  activity_score = {25 if failed >= 20, 15 if >= 10, 10 if >= 5}
  multi_user_score = {25 if users >= 10, 15 if >= 5}
```

---

## 6. Overall Risk Calculation

### 6.1 Category Weights

```
payment_weight = 0.30    (highest - direct financial impact)
behavioral_weight = 0.25 (significant loss potential)
account_weight = 0.20    (platform trust)
referral_weight = 0.10   (typically lower impact)
security_weight = 0.15   (can escalate quickly)
```

### 6.2 Overall Score Formula

```
overall_score = (payment × 0.30) + (behavioral × 0.25) +
                (account × 0.20) + (referral × 0.10) + (security × 0.15)
```

---

## 7. Configuration Parameters

### 7.1 Default Thresholds

| Parameter | Default | Description |
|-----------|---------|-------------|
| `failedPaymentThreshold` | 3 | Failed payments to flag user |
| `paymentTimeWindowMinutes` | 60 | Time window for payment velocity |
| `velocityThreshold` | 5 | Max orders per hour |
| `refundRateThreshold` | 30.0% | High refund rate threshold |
| `orderValueZScore` | 3.0 | Z-score for anomaly detection |
| `addressUserThreshold` | 3 | Users per address to flag |
| `fakeAccountAgeThreshold` | 24 hours | New account age for scrutiny |
| `deviceShareThreshold` | 2 | Users per device to flag |
| `ipShareThreshold` | 5 | Users per IP to flag |
| `deviceSignupThreshold` | 2 | Signups per device |
| `ipSignupThreshold` | 5 | Signups per IP |
| `promoAbuseRate` | 80.0% | Promo abuse rate threshold |
| `failedLoginThreshold` | 5 | Failed logins to flag |
| `loginTimeWindowMinutes` | 15 | Time window for login velocity |
| `rateLimitThreshold` | 100 | Rate limit violation count |

### 7.2 Cache TTL Values

| Cache | TTL | Rationale |
|-------|-----|-----------|
| Fraud Metrics | 60s | Balance freshness with performance |
| Security Metrics | 30s | Near real-time for security events |
| Referral Metrics | 300s | Less volatile, longer cache |
| Summary | 120s | Aggregated data, moderate freshness |

---

## 8. API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/analytics/fraud/summary` | GET/POST | Overall fraud summary |
| `/api/v1/analytics/fraud/payment` | GET | Payment fraud metrics |
| `/api/v1/analytics/fraud/behavioral` | GET | Behavioral fraud metrics |
| `/api/v1/analytics/fraud/account-integrity` | GET | Account integrity metrics |
| `/api/v1/analytics/fraud/referral` | GET | Referral fraud metrics |
| `/api/v1/analytics/fraud/security` | GET | Security log metrics |
| `/api/v1/analytics/fraud/user/{id}/risk` | GET | User risk score |
| `/api/v1/analytics/fraud/alerts/realtime` | POST | Real-time alerts |
| `/api/v1/analytics/fraud/cache/invalidate` | POST | Clear cache |

---

## 9. Access Control

| Role | Accessible Endpoints |
|------|---------------------|
| ADMIN | All endpoints |
| FRAUD_ANALYST | All except cache invalidation |
| SECURITY_ANALYST | Security metrics, alerts |
| PAYMENT_ANALYST | Payment fraud metrics |
| MARKETING_ANALYST | Referral fraud metrics |

---

## 10. Monitoring Recommendations

1. **Real-time Alerts**
   - Critical risk scores (>80) trigger immediate alerts
   - Brute force detection alerts to security team
   - Payment spike detection for fraud team

2. **Daily Review**
   - High-risk user list review
   - Refund abuse patterns
   - New account quality metrics

3. **Weekly Analysis**
   - Trend comparison with previous periods
   - Referral program health check
   - Device fingerprint distribution analysis

4. **Monthly Reporting**
   - Financial impact assessment
   - False positive rate review
   - Threshold optimization recommendations
