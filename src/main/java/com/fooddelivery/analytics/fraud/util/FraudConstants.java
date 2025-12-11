package com.fooddelivery.analytics.fraud.util;

/**
 * Constants used across the fraud analytics module.
 */
public final class FraudConstants {

    private FraudConstants() {
        // Prevent instantiation
    }

    // ============================================
    // Risk Level Thresholds
    // ============================================
    public static final int LOW_RISK_THRESHOLD = 30;
    public static final int MEDIUM_RISK_THRESHOLD = 60;
    public static final int HIGH_RISK_THRESHOLD = 80;
    public static final int MAX_RISK_SCORE = 100;

    // ============================================
    // Default Detection Thresholds
    // ============================================

    // Payment Fraud
    public static final int DEFAULT_FAILED_PAYMENT_THRESHOLD = 3;
    public static final int DEFAULT_PAYMENT_TIME_WINDOW_MINUTES = 60;
    public static final double DEFAULT_CARD_TESTING_AMOUNT = 1.00;
    public static final int DEFAULT_RAPID_PAYMENT_COUNT = 5;

    // Behavioral Fraud
    public static final int DEFAULT_VELOCITY_THRESHOLD = 5; // orders per hour
    public static final double DEFAULT_ORDER_VALUE_Z_SCORE = 3.0;
    public static final double DEFAULT_REFUND_RATE_THRESHOLD = 30.0; // percent
    public static final int DEFAULT_ADDRESS_USER_THRESHOLD = 3;
    public static final double DEFAULT_RESTAURANT_ANOMALY_MULTIPLIER = 2.0;

    // Account Integrity
    public static final int DEFAULT_FAKE_ACCOUNT_AGE_HOURS = 24;
    public static final int DEFAULT_DEVICE_SHARE_THRESHOLD = 2;
    public static final int DEFAULT_IP_SHARE_THRESHOLD = 5;
    public static final int DEFAULT_LOW_TRUST_SCORE = 50;

    // Referral Fraud
    public static final int DEFAULT_DEVICE_SIGNUP_THRESHOLD = 2;
    public static final int DEFAULT_IP_SIGNUP_THRESHOLD = 5;
    public static final double DEFAULT_PROMO_ABUSE_RATE = 80.0; // percent
    public static final int DEFAULT_MIN_REFERRALS_FOR_ANALYSIS = 3;

    // Security
    public static final int DEFAULT_FAILED_LOGIN_THRESHOLD = 5;
    public static final int DEFAULT_LOGIN_TIME_WINDOW_MINUTES = 15;
    public static final int DEFAULT_RATE_LIMIT_THRESHOLD = 100;
    public static final int DEFAULT_BRUTE_FORCE_WINDOW_MINUTES = 5;

    // ============================================
    // Cache Configuration
    // ============================================
    public static final String CACHE_NAME_FRAUD_METRICS = "fraudMetrics";
    public static final String CACHE_NAME_SECURITY_METRICS = "securityMetrics";
    public static final String CACHE_NAME_REFERRAL_METRICS = "referralMetrics";
    public static final String CACHE_NAME_FRAUD_SUMMARY = "fraudSummary";

    public static final long CACHE_TTL_FRAUD_SECONDS = 60;
    public static final long CACHE_TTL_SECURITY_SECONDS = 30;
    public static final long CACHE_TTL_REFERRAL_SECONDS = 300;
    public static final long CACHE_TTL_SUMMARY_SECONDS = 120;

    // ============================================
    // Redis Key Prefixes
    // ============================================
    public static final String REDIS_PREFIX_FRAUD = "fraud:";
    public static final String REDIS_PREFIX_SECURITY = "security:";
    public static final String REDIS_PREFIX_REFERRAL = "referral:";
    public static final String REDIS_KEY_FAILED_LOGIN_COUNT = "failed_login_count:";
    public static final String REDIS_KEY_RATE_LIMIT_COUNT = "rate_limit_count:";
    public static final String REDIS_KEY_PAYMENT_ATTEMPT_COUNT = "payment_attempt_count:";

    // ============================================
    // Default List Sizes
    // ============================================
    public static final int DEFAULT_MAX_LIST_SIZE = 100;
    public static final int DEFAULT_TOP_THREATS_SIZE = 10;
    public static final int DEFAULT_RECENT_FLAGS_SIZE = 50;

    // ============================================
    // Time Periods
    // ============================================
    public static final int HOURS_IN_DAY = 24;
    public static final int DAYS_IN_WEEK = 7;
    public static final int DAYS_IN_MONTH = 30;

    // ============================================
    // Risk Indicators
    // ============================================
    public static final String INDICATOR_HIGH_FAILURE_COUNT = "HIGH_FAILURE_COUNT";
    public static final String INDICATOR_HIGH_FAILURE_RATE = "HIGH_FAILURE_RATE";
    public static final String INDICATOR_CARD_TESTING = "CARD_TESTING";
    public static final String INDICATOR_VELOCITY_VIOLATION = "VELOCITY_VIOLATION";
    public static final String INDICATOR_ABNORMAL_ORDER_VALUE = "ABNORMAL_ORDER_VALUE";
    public static final String INDICATOR_REFUND_ABUSE = "REFUND_ABUSE";
    public static final String INDICATOR_MULTI_ACCOUNT = "MULTI_ACCOUNT";
    public static final String INDICATOR_FAKE_ACCOUNT = "FAKE_ACCOUNT";
    public static final String INDICATOR_PROMO_ABUSE = "PROMO_ABUSE";
    public static final String INDICATOR_CIRCULAR_REFERRAL = "CIRCULAR_REFERRAL";
    public static final String INDICATOR_BRUTE_FORCE = "BRUTE_FORCE";
    public static final String INDICATOR_SUSPICIOUS_IP = "SUSPICIOUS_IP";
    public static final String INDICATOR_VPN_DETECTED = "VPN_DETECTED";
    public static final String INDICATOR_TOR_DETECTED = "TOR_DETECTED";
    public static final String INDICATOR_EMULATOR_DETECTED = "EMULATOR_DETECTED";
    public static final String INDICATOR_ROOTED_DEVICE = "ROOTED_DEVICE";
    public static final String INDICATOR_VERY_NEW_ACCOUNT = "VERY_NEW_ACCOUNT";
    public static final String INDICATOR_PROMO_ON_FIRST_ORDER = "PROMO_ON_FIRST_ORDER";

    // ============================================
    // Fraud Categories
    // ============================================
    public static final String CATEGORY_PAYMENT = "PAYMENT";
    public static final String CATEGORY_BEHAVIORAL = "BEHAVIORAL";
    public static final String CATEGORY_ACCOUNT = "ACCOUNT";
    public static final String CATEGORY_REFERRAL = "REFERRAL";
    public static final String CATEGORY_SECURITY = "SECURITY";

    // ============================================
    // Risk Levels
    // ============================================
    public static final String RISK_LEVEL_LOW = "LOW";
    public static final String RISK_LEVEL_MEDIUM = "MEDIUM";
    public static final String RISK_LEVEL_HIGH = "HIGH";
    public static final String RISK_LEVEL_CRITICAL = "CRITICAL";
}
