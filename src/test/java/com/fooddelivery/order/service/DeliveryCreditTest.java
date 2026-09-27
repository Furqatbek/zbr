package com.fooddelivery.order.service;

import com.fooddelivery.order.entity.DeliveryCredit;
import com.fooddelivery.order.entity.PromoCode;
import com.fooddelivery.order.repository.DeliveryCreditRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Free deliveries: who is owed one, and how one person gets exactly one.
 *
 * <p>Two ways to hand out the same benefit — a welcome credit and a referral —
 * is two ways for one person to collect twice for arriving once. The rule lives
 * in a partial unique index rather than in a service method, because a service
 * method is something a second code path can forget to call.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Delivery credits")
class DeliveryCreditTest {

    private static final Long USER = 7L;
    private static final Long ORDER = 500L;

    @Mock
    private DeliveryCreditRepository creditRepository;

    @InjectMocks
    private DeliveryCreditService service;

    private DeliveryCredit credit(DeliveryCredit.Reason reason) {
        return DeliveryCredit.builder()
                .id(1L).userId(USER).reason(reason).grantedAt(LocalDateTime.now()).build();
    }

    @Nested
    @DisplayName("granting")
    class Granting {

        @Test
        @DisplayName("a new customer gets one")
        void welcomeIsGranted() {
            service.grantWelcome(USER);

            verify(creditRepository).save(any(DeliveryCredit.class));
        }

        @Test
        @DisplayName("a second welcome credit is refused by the database, not by us")
        void welcomeIsOncePerPerson() {
            // The unique index doing its job. Registration can be retried, and a
            // retry must not be worth a second free delivery.
            when(creditRepository.save(any(DeliveryCredit.class)))
                    .thenThrow(new DataIntegrityViolationException("uq_delivery_credits_welcome"));

            // And it must not fail the sign-up: a customer who cannot create an
            // account is worse than one who pays for their first delivery.
            assertThatCode(() -> service.grantWelcome(USER)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("a referral rewards the referrer once, however often it completes")
        void referralRewardIsIdempotent() {
            // The trigger is an order reaching DELIVERED, and that runs again
            // for COMPLETED — plus events get redelivered.
            when(creditRepository.save(any(DeliveryCredit.class)))
                    .thenReturn(credit(DeliveryCredit.Reason.REFERRAL_REWARD))
                    .thenThrow(new DataIntegrityViolationException("uq_delivery_credits_referral"));

            assertThat(service.grantReferralReward(99L, 12L)).isTrue();
            assertThat(service.grantReferralReward(99L, 12L)).isFalse();
        }
    }

    @Nested
    @DisplayName("spending")
    class Spending {

        @Test
        @DisplayName("a credit covers the delivery fee")
        void creditCoversTheFee() {
            when(creditRepository.findSpendable(anyLong(), any()))
                    .thenReturn(List.of(credit(DeliveryCredit.Reason.WELCOME)));

            Optional<BigDecimal> spent = service.spendOn(USER, ORDER, new BigDecimal("8000"));

            assertThat(spent).contains(new BigDecimal("8000"));
        }

        @Test
        @DisplayName("a customer with no credit pays for delivery")
        void nothingToSpend() {
            when(creditRepository.findSpendable(anyLong(), any())).thenReturn(List.of());

            assertThat(service.spendOn(USER, ORDER, new BigDecimal("8000"))).isEmpty();
        }

        @Test
        @DisplayName("a free delivery is not spent on an order that has no fee")
        void zeroFeeKeepsTheCredit() {
            // Pickup, or a venue that does not charge. Spending it here would
            // burn the benefit on nothing, which the customer would rightly
            // read as having been robbed of it.
            assertThat(service.spendOn(USER, ORDER, BigDecimal.ZERO)).isEmpty();
            verify(creditRepository, never()).findSpendable(anyLong(), any());
        }

        @Test
        @DisplayName("the credit records which order spent it and what it was worth")
        void spendingIsRecorded() {
            DeliveryCredit c = credit(DeliveryCredit.Reason.WELCOME);
            when(creditRepository.findSpendable(anyLong(), any())).thenReturn(List.of(c));

            service.spendOn(USER, ORDER, new BigDecimal("8000"));

            // Without this, "has this person had their free delivery" is
            // answerable but "on what, and what did it cost us" is not.
            assertThat(c.getOrderId()).isEqualTo(ORDER);
            assertThat(c.getAmount()).isEqualByComparingTo("8000");
            assertThat(c.getUsedAt()).isNotNull();
            assertThat(c.isSpendable()).isFalse();
        }

        @Test
        @DisplayName("an expired credit is not spendable")
        void expiredIsNotSpendable() {
            DeliveryCredit c = credit(DeliveryCredit.Reason.WELCOME);
            c.setExpiresAt(LocalDateTime.now().minusDays(1));

            assertThat(c.isSpendable()).isFalse();
        }
    }

    @Nested
    @DisplayName("FREE_DELIVERY promo codes")
    class FreeDeliveryCodes {

        private PromoCode freeDelivery() {
            return PromoCode.builder()
                    .code("QAHVOON")
                    .discountType(PromoCode.DiscountType.FREE_DELIVERY)
                    .discountValue(BigDecimal.ZERO)
                    .build();
        }

        @Test
        @DisplayName("the discount is the delivery fee, whatever it happens to be")
        void discountIsTheFee() {
            // The reason this cannot be a FIXED code: the fee is distance-based,
            // so a flat amount short-changes someone far away and overpays
            // someone next door.
            assertThat(freeDelivery().calculateDiscount(new BigDecimal("45000"), new BigDecimal("8000")))
                    .isEqualByComparingTo("8000");
            assertThat(freeDelivery().calculateDiscount(new BigDecimal("45000"), new BigDecimal("15000")))
                    .isEqualByComparingTo("15000");
        }

        @Test
        @DisplayName("a cap makes 'free delivery up to 10 000' sayable")
        void capIsRespected() {
            PromoCode code = freeDelivery();
            code.setMaxDiscountAmount(new BigDecimal("10000"));

            assertThat(code.calculateDiscount(new BigDecimal("45000"), new BigDecimal("15000")))
                    .isEqualByComparingTo("10000");
        }

        @Test
        @DisplayName("the delivery fee may exceed the food, and the discount still stands")
        void feeMayExceedSubtotal() {
            // The subtotal clamp that protects the other two types would be
            // wrong here: a 12 000 delivery on a 9 000 coffee is entirely
            // possible, and the whole fee is what was promised.
            assertThat(freeDelivery().calculateDiscount(new BigDecimal("9000"), new BigDecimal("12000")))
                    .isEqualByComparingTo("12000");
        }

        @Test
        @DisplayName("a food discount is unaffected by the delivery fee")
        void otherTypesIgnoreTheFee() {
            PromoCode fixed = PromoCode.builder()
                    .discountType(PromoCode.DiscountType.FIXED)
                    .discountValue(new BigDecimal("5000")).build();

            assertThat(fixed.calculateDiscount(new BigDecimal("45000"), new BigDecimal("8000")))
                    .isEqualByComparingTo("5000");
        }
    }
}
