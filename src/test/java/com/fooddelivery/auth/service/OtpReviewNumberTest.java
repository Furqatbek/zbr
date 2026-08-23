package com.fooddelivery.auth.service;

import com.fooddelivery.auth.entity.OtpCode;
import com.fooddelivery.auth.repository.OtpCodeRepository;
import com.fooddelivery.sms.service.SmsNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * App-store review test numbers: a reviewer cannot receive our SMS, so one
 * configured number accepts a fixed code and sends nothing. This must never
 * leak into behaviour for real numbers, and must stay off unless configured.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OtpService — app-store review numbers")
class OtpReviewNumberTest {

    @Mock private OtpCodeRepository otpCodeRepository;
    @Mock private SmsNotificationService smsNotificationService;

    @InjectMocks private OtpService otpService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "otpExpiryMinutes", 5);
        ReflectionTestUtils.setField(otpService, "maxAttempts", 3);
        ReflectionTestUtils.setField(otpService, "rateLimitPerHour", 5);
        ReflectionTestUtils.setField(otpService, "codeLength", 6);
        when(otpCodeRepository.save(any(OtpCode.class))).thenAnswer(i -> i.getArgument(0));
        when(otpCodeRepository.countRecentOtps(anyString(), any())).thenReturn(0L);
    }

    private void configureReview(String numbers, String code) {
        ReflectionTestUtils.setField(otpService, "reviewNumbers", numbers);
        ReflectionTestUtils.setField(otpService, "reviewCode", code);
    }

    @Test
    @DisplayName("review number: stores the fixed code and sends NO SMS")
    void reviewNumberSkipsSms() {
        configureReview("+998900000000", "123456");

        OtpCode otp = otpService.generateAndSendOtp("+998900000000", OtpCode.OtpPurpose.LOGIN);

        assertThat(otp.getCode()).isEqualTo("123456");
        verify(smsNotificationService, never()).sendOtp(anyString(), anyString());
    }

    @Test
    @DisplayName("review number matches regardless of formatting")
    void reviewNumberNormalised() {
        configureReview("+998 90 000 00 00", "123456");

        OtpCode otp = otpService.generateAndSendOtp("998900000000", OtpCode.OtpPurpose.LOGIN);

        assertThat(otp.getCode()).isEqualTo("123456");
        verify(smsNotificationService, never()).sendOtp(anyString(), anyString());
    }

    @Test
    @DisplayName("a real number is unaffected: random code, SMS sent")
    void realNumberStillSendsSms() {
        configureReview("+998900000000", "123456");

        OtpCode otp = otpService.generateAndSendOtp("+998901234567", OtpCode.OtpPurpose.LOGIN);

        assertThat(otp.getCode()).isNotEqualTo("123456");
        assertThat(otp.getCode()).hasSize(6);
        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(smsNotificationService).sendOtp(anyString(), sent.capture());
        assertThat(sent.getValue()).isEqualTo(otp.getCode());
    }

    @Test
    @DisplayName("disabled by default: unconfigured means every number is real")
    void offByDefault() {
        configureReview("", "");

        otpService.generateAndSendOtp("+998900000000", OtpCode.OtpPurpose.LOGIN);

        verify(smsNotificationService).sendOtp(anyString(), anyString());
    }

    @Test
    @DisplayName("fails closed when only half configured (number set, code missing)")
    void halfConfiguredFailsClosed() {
        configureReview("+998900000000", "");

        otpService.generateAndSendOtp("+998900000000", OtpCode.OtpPurpose.LOGIN);

        verify(smsNotificationService).sendOtp(anyString(), anyString());
    }
}
