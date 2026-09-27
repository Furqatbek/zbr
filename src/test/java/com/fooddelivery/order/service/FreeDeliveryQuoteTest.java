package com.fooddelivery.order.service;

import com.fooddelivery.order.dto.DeliveryFeeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Showing a free delivery in the quote, before the customer commits.
 *
 * <p>The customer app could have inferred first-order eligibility from an empty
 * order list and refused to, because the fee calculation would still have
 * charged: "your delivery is on us" on the home screen, a delivery charge two
 * screens later, on a customer deciding whether to trust the platform. A flag
 * without this moves that false promise one step earlier rather than removing it.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Free delivery in the fee quote")
class FreeDeliveryQuoteTest {

    private static final Long USER = 7L;

    @Mock private DeliveryFeeSettingsService settingsService;
    @Mock private RouteDistanceService routeDistanceService;
    @Mock private com.fooddelivery.delivery.eta.DeliveryEtaService etaService;
    @Mock private DeliveryCreditService deliveryCreditService;

    @InjectMocks
    private DeliveryFeeCalculationService service;

    private DeliveryFeeResponse quoteOf(String fee) {
        return DeliveryFeeResponse.builder()
                .deliveryFee(new BigDecimal(fee))
                .payableDeliveryFee(new BigDecimal(fee))
                .deliveryFeeDiscount(BigDecimal.ZERO)
                .freeDeliveryApplied(false)
                .build();
    }

    @Test
    @DisplayName("an eligible customer sees the delivery waived")
    void waiverIsShown() {
        when(deliveryCreditService.hasSpendableCredit(USER)).thenReturn(true);

        DeliveryFeeResponse quote = service.withFreeDeliveryFor(quoteOf("8000"), USER);

        assertThat(quote.isFreeDeliveryApplied()).isTrue();
        assertThat(quote.getPayableDeliveryFee()).isEqualByComparingTo("0");
        assertThat(quote.getDeliveryFeeDiscount()).isEqualByComparingTo("8000");
        // The fee itself is untouched: the courier is paid from it, so zeroing it
        // would fund the promotion out of their earnings.
        assertThat(quote.getDeliveryFee()).isEqualByComparingTo("8000");
    }

    @Test
    @DisplayName("a customer with no credit is quoted the full fee")
    void noCreditNoWaiver() {
        when(deliveryCreditService.hasSpendableCredit(USER)).thenReturn(false);

        DeliveryFeeResponse quote = service.withFreeDeliveryFor(quoteOf("8000"), USER);

        assertThat(quote.isFreeDeliveryApplied()).isFalse();
        assertThat(quote.getPayableDeliveryFee()).isEqualByComparingTo("8000");
    }

    @Test
    @DisplayName("an anonymous quote is left alone")
    void anonymousQuote() {
        DeliveryFeeResponse quote = service.withFreeDeliveryFor(quoteOf("8000"), null);

        assertThat(quote.isFreeDeliveryApplied()).isFalse();
        verify(deliveryCreditService, never()).hasSpendableCredit(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("a zero fee is not dressed up as a waiver")
    void zeroFeeIsNotAWaiver() {
        // Nothing was waived, so claiming otherwise would tell the customer they
        // spent a benefit they still have.
        DeliveryFeeResponse quote = service.withFreeDeliveryFor(quoteOf("0"), USER);

        assertThat(quote.isFreeDeliveryApplied()).isFalse();
        verify(deliveryCreditService, never()).hasSpendableCredit(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("the quote asks about the credit, it does not spend it")
    void quoteDoesNotSpend() {
        // A quote is read on every address change and every basket edit. Spending
        // there would burn the benefit on a customer who never ordered.
        when(deliveryCreditService.hasSpendableCredit(USER)).thenReturn(true);

        service.withFreeDeliveryFor(quoteOf("8000"), USER);

        verify(deliveryCreditService, never()).spendOn(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());
    }
}
