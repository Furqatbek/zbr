package com.fooddelivery.auth.service;

import com.fooddelivery.auth.entity.OtpCode;
import com.fooddelivery.auth.repository.OtpCodeRepository;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.sms.service.SmsNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for OTP generation, sending, and verification.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpCodeRepository otpCodeRepository;
    private final SmsNotificationService smsNotificationService;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.otp.rate-limit-per-hour:5}")
    private int rateLimitPerHour;

    @Value("${app.otp.code-length:6}")
    private int codeLength;

    /**
     * Generate and send OTP to phone number.
     *
     * @param phone   the phone number
     * @param purpose the purpose of OTP
     * @return the generated OTP code entity
     */
    @Transactional
    public OtpCode generateAndSendOtp(String phone, OtpCode.OtpPurpose purpose) {
        String normalizedPhone = normalizePhone(phone);

        // Rate limiting check
        checkRateLimit(normalizedPhone);

        // Invalidate any existing OTPs for this phone
        otpCodeRepository.invalidateAllForPhone(normalizedPhone);

        // Generate new OTP
        String code = generateOtpCode();

        OtpCode otp = OtpCode.builder()
                .phone(normalizedPhone)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .maxAttempts(maxAttempts)
                .purpose(purpose)
                .build();

        otp = otpCodeRepository.save(otp);

        // Send OTP via SMS
        smsNotificationService.sendOtp(normalizedPhone, code);

        log.info("OTP sent to phone: {} for purpose: {}", maskPhone(normalizedPhone), purpose);

        return otp;
    }

    /**
     * Verify OTP code.
     *
     * @param phone the phone number
     * @param code  the OTP code
     * @return true if verification successful
     */
    @Transactional
    public boolean verifyOtp(String phone, String code) {
        String normalizedPhone = normalizePhone(phone);

        Optional<OtpCode> otpOpt = otpCodeRepository.findByPhoneAndCode(
                normalizedPhone, code, LocalDateTime.now());

        if (otpOpt.isEmpty()) {
            // Try to find any valid OTP to increment attempts
            otpCodeRepository.findLatestValidOtp(normalizedPhone, LocalDateTime.now())
                    .ifPresent(otp -> {
                        otp.incrementAttempts();
                        otpCodeRepository.save(otp);
                        log.warn("Invalid OTP attempt for phone: {}, attempts: {}",
                                maskPhone(normalizedPhone), otp.getAttempts());
                    });
            return false;
        }

        OtpCode otp = otpOpt.get();

        if (!otp.isValid()) {
            log.warn("OTP is no longer valid for phone: {}", maskPhone(normalizedPhone));
            return false;
        }

        // Mark as used
        otp.markAsUsed();
        otpCodeRepository.save(otp);

        log.info("OTP verified successfully for phone: {}", maskPhone(normalizedPhone));
        return true;
    }

    /**
     * Get remaining attempts for the latest OTP.
     */
    @Transactional(readOnly = true)
    public int getRemainingAttempts(String phone) {
        String normalizedPhone = normalizePhone(phone);
        return otpCodeRepository.findLatestValidOtp(normalizedPhone, LocalDateTime.now())
                .map(otp -> otp.getMaxAttempts() - otp.getAttempts())
                .orElse(0);
    }

    /**
     * Check if there's a valid OTP pending for phone.
     */
    @Transactional(readOnly = true)
    public boolean hasValidOtp(String phone) {
        String normalizedPhone = normalizePhone(phone);
        return otpCodeRepository.findLatestValidOtp(normalizedPhone, LocalDateTime.now())
                .isPresent();
    }

    /**
     * Get seconds until current OTP expires.
     */
    @Transactional(readOnly = true)
    public int getSecondsUntilExpiry(String phone) {
        String normalizedPhone = normalizePhone(phone);
        return otpCodeRepository.findLatestValidOtp(normalizedPhone, LocalDateTime.now())
                .map(otp -> {
                    long seconds = java.time.Duration.between(
                            LocalDateTime.now(), otp.getExpiresAt()).getSeconds();
                    return Math.max(0, (int) seconds);
                })
                .orElse(0);
    }

    /**
     * Rate limiting check.
     */
    private void checkRateLimit(String phone) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentCount = otpCodeRepository.countRecentOtps(phone, oneHourAgo);

        if (recentCount >= rateLimitPerHour) {
            log.warn("Rate limit exceeded for phone: {}", maskPhone(phone));
            throw new BusinessException("Too many OTP requests. Please try again later.");
        }
    }

    /**
     * Generate random OTP code.
     */
    private String generateOtpCode() {
        StringBuilder sb = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Normalize phone number.
     */
    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }

        // Remove all non-digit characters except leading +
        String digits = phone.replaceAll("[^0-9+]", "");

        // Handle Uzbekistan numbers
        if (digits.startsWith("+998")) {
            return digits.substring(1); // Remove +
        } else if (digits.startsWith("998")) {
            return digits;
        } else if (digits.startsWith("8") && digits.length() == 10) {
            return "998" + digits.substring(1);
        } else if (digits.length() == 9 && !digits.startsWith("+")) {
            return "998" + digits;
        }

        return digits.startsWith("+") ? digits.substring(1) : digits;
    }

    /**
     * Mask phone for logging.
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) {
            return "***";
        }
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }

    /**
     * Cleanup expired OTPs (runs daily at 3 AM).
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredOtps() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        int deleted = otpCodeRepository.deleteExpiredOtps(cutoff);
        log.info("Cleaned up {} expired OTP records", deleted);
    }
}
