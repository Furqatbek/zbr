/**
 * Fraud & Security Analytics Module for Food Delivery Platform.
 *
 * <h2>Overview</h2>
 * <p>This module provides comprehensive fraud detection and security analytics
 * for the food delivery platform. It monitors user behavior, payment patterns,
 * account integrity, referral activity, and security events to identify and
 * prevent fraudulent activities.</p>
 *
 * <h2>Package Structure</h2>
 * <ul>
 *   <li>{@code controller} - REST API endpoints for fraud analytics</li>
 *   <li>{@code service} - Business logic and fraud detection algorithms</li>
 *   <li>{@code repository} - Data access layer with fraud detection queries</li>
 *   <li>{@code collector} - Data collectors for aggregating fraud metrics</li>
 *   <li>{@code model} - JPA entities for fraud-related data</li>
 *   <li>{@code dto} - Data transfer objects for API responses</li>
 *   <li>{@code mapper} - MapStruct mappers for entity-DTO conversion</li>
 *   <li>{@code util} - Utility classes including risk calculators</li>
 *   <li>{@code config} - Configuration classes including Redis cache</li>
 * </ul>
 *
 * <h2>Fraud Categories</h2>
 * <ol>
 *   <li><b>Payment Fraud</b> - Failed payments, card testing, payment velocity</li>
 *   <li><b>Behavioral Fraud</b> - Order velocity, refund abuse, address fraud</li>
 *   <li><b>Account Integrity</b> - Fake accounts, multi-account detection</li>
 *   <li><b>Referral Fraud</b> - Device/IP reuse, circular referrals, promo abuse</li>
 *   <li><b>Security Logs</b> - Login failures, brute force, VPN/TOR detection</li>
 * </ol>
 *
 * <h2>Risk Scoring</h2>
 * <p>All risk scores use a 0-100 scale:</p>
 * <ul>
 *   <li>0-30: LOW risk</li>
 *   <li>31-60: MEDIUM risk</li>
 *   <li>61-80: HIGH risk</li>
 *   <li>81-100: CRITICAL risk</li>
 * </ul>
 *
 * <h2>Caching Strategy</h2>
 * <ul>
 *   <li>Fraud metrics: 60 seconds TTL</li>
 *   <li>Security metrics: 30 seconds TTL</li>
 *   <li>Referral metrics: 5 minutes TTL</li>
 *   <li>Summary: 2 minutes TTL</li>
 * </ul>
 *
 * @see com.fooddelivery.analytics.fraud.service.FraudAnalyticsService
 * @see com.fooddelivery.analytics.fraud.util.FraudRiskCalculator
 * @since 1.0
 */
package com.fooddelivery.analytics.fraud;
