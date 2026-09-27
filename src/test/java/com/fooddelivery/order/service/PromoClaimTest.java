package com.fooddelivery.order.service;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.order.entity.PromoCode;
import com.fooddelivery.order.repository.PromoCodeRepository;
import com.fooddelivery.order.repository.PromoCodeUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Taking a promo code, as opposed to telling someone it is valid.
 *
 * <p>Until this existed, {@code /orders/validate-promo} answered "valid, you
 * save 5 000" and the order then charged the full amount: {@code discountCode}
 * was on the request, nothing read it, and {@code Order.discount} was never
 * set. A discount the interface promises and the invoice omits is the version
 * of this bug that takes money.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Claiming a promo code")
class PromoClaimTest {

    private static final Long USER = 7L;
    private static final Long RESTAURANT = 3L;

    @Mock
    private PromoCodeRepository promoCodeRepository;

    @Mock
    private PromoCodeUsageRepository usageRepository;

    @InjectMocks
    private PromoService promoService;

    private PromoCode code;

    @BeforeEach
    void setUp() {
        code = PromoCode.builder()
                .id(1L)
                .code("QAHVOON")
                .discountType(PromoCode.DiscountType.FIXED)
                .discountValue(new BigDecimal("5000"))
                .userUsageLimit(1)
                .usageCount(0)
                .isActive(true)
                .build();
    }

    private void codeExists() {
        when(promoCodeRepository.findByCodeIgnoreCase("QAHVOON")).thenReturn(Optional.of(code));
    }

    @Test
    @DisplayName("a good code returns what it is worth")
    void goodCodeIsClaimed() {
        codeExists();
        when(usageRepository.countByPromoCodeIdAndUserId(1L, USER)).thenReturn(0L);
        when(promoCodeRepository.claimOneUse(1L)).thenReturn(1);

        PromoService.Claim claim = promoService.claim(
                "QAHVOON", USER, RESTAURANT, new BigDecimal("45000"), new BigDecimal("8000"));

        assertThat(claim.discount()).isEqualByComparingTo("5000");
        assertThat(claim.code()).isEqualTo("QAHVOON");
    }

    @Test
    @DisplayName("the same customer cannot use a once-per-customer code twice")
    void perUserLimitIsEnforced() {
        // user_usage_limit has been on the table since promo codes shipped and
        // could never be enforced: usage_count is global, so nothing recorded
        // WHO spent a use. "First order free" without this is free forever.
        codeExists();
        when(usageRepository.countByPromoCodeIdAndUserId(1L, USER)).thenReturn(1L);

        assertThatThrownBy(() -> promoService.claim(
                "QAHVOON", USER, RESTAURANT, new BigDecimal("45000"), new BigDecimal("8000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already used");

        // And crucially the global count is untouched — a refused claim must
        // not consume campaign budget.
        verify(promoCodeRepository, never()).claimOneUse(anyLong());
    }

    @Test
    @DisplayName("the last use goes to one order, not to both")
    void globalLimitIsTakenAtomically() {
        // claimOneUse returning 0 is the database saying someone else took it
        // between our read and our write. Read-then-increment would have given
        // the last use away twice.
        codeExists();
        when(usageRepository.countByPromoCodeIdAndUserId(1L, USER)).thenReturn(0L);
        when(promoCodeRepository.claimOneUse(1L)).thenReturn(0);

        assertThatThrownBy(() -> promoService.claim(
                "QAHVOON", USER, RESTAURANT, new BigDecimal("45000"), new BigDecimal("8000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("usage limit");
    }

    @Test
    @DisplayName("a code for another restaurant is refused")
    void restaurantRestrictionIsEnforced() {
        code.setRestaurantId(99L);
        codeExists();

        assertThatThrownBy(() -> promoService.claim(
                "QAHVOON", USER, RESTAURANT, new BigDecimal("45000"), new BigDecimal("8000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not valid for this restaurant");
        verify(promoCodeRepository, never()).claimOneUse(anyLong());
    }

    @Test
    @DisplayName("an expired code is refused, and says so")
    void expiredCodeIsRefused() {
        code.setExpiresAt(LocalDateTime.now().minusDays(1));
        codeExists();

        assertThatThrownBy(() -> promoService.claim(
                "QAHVOON", USER, RESTAURANT, new BigDecimal("45000"), new BigDecimal("8000")))
                .isInstanceOf(BusinessException.class)
                // Not a generic "invalid": the customer can tell the difference
                // between a typo and a campaign that ended.
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("below the minimum order the code does not apply")
    void minimumOrderIsEnforced() {
        code.setMinOrderAmount(new BigDecimal("50000"));
        codeExists();

        assertThatThrownBy(() -> promoService.claim(
                "QAHVOON", USER, RESTAURANT, new BigDecimal("45000"), new BigDecimal("8000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("minimum order");
    }

    @Test
    @DisplayName("an unknown code is refused without touching anything")
    void unknownCodeIsRefused() {
        when(promoCodeRepository.findByCodeIgnoreCase("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promoService.claim(
                "NOPE", USER, RESTAURANT, new BigDecimal("45000"), new BigDecimal("8000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid promo code");
        verify(usageRepository, never()).save(any());
    }

    @Test
    @DisplayName("a percentage discount respects its cap")
    void percentageRespectsCap() {
        code.setDiscountType(PromoCode.DiscountType.PERCENTAGE);
        code.setDiscountValue(new BigDecimal("20"));
        code.setMaxDiscountAmount(new BigDecimal("6000"));
        codeExists();
        when(usageRepository.countByPromoCodeIdAndUserId(1L, USER)).thenReturn(0L);
        when(promoCodeRepository.claimOneUse(1L)).thenReturn(1);

        // 20% of 45 000 is 9 000, capped at 6 000.
        assertThat(promoService.claim("QAHVOON", USER, RESTAURANT, new BigDecimal("45000"), new BigDecimal("8000"))
                .discount()).isEqualByComparingTo("6000");
    }

    @Test
    @DisplayName("a discount never exceeds the subtotal")
    void discountCannotExceedSubtotal() {
        // Otherwise a fixed code larger than a small basket produces a negative
        // total, which downstream is a payment we owe the customer.
        code.setDiscountValue(new BigDecimal("50000"));
        codeExists();
        when(usageRepository.countByPromoCodeIdAndUserId(1L, USER)).thenReturn(0L);
        when(promoCodeRepository.claimOneUse(1L)).thenReturn(1);

        assertThat(promoService.claim("QAHVOON", USER, RESTAURANT, new BigDecimal("12000"), new BigDecimal("8000"))
                .discount()).isEqualByComparingTo("12000");
    }
}
