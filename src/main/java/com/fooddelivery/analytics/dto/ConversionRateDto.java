package com.fooddelivery.analytics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO containing conversion funnel metrics.
 *
 * <p>Funnel Steps:</p>
 * <ol>
 *   <li>Viewed restaurant</li>
 *   <li>Added to cart</li>
 *   <li>Started checkout</li>
 *   <li>Payment completed</li>
 * </ol>
 *
 * <p>Conversion Rate Formula:</p>
 * <pre>
 * conversionRate = (payments_completed / restaurant_views) * 100
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionRateDto {

    /**
     * Number of restaurant views (funnel top).
     */
    private Long restaurantViews;

    /**
     * Number of add-to-cart events.
     */
    private Long addToCartEvents;

    /**
     * Number of checkout started events.
     */
    private Long checkoutStartEvents;

    /**
     * Number of successful payments (funnel bottom).
     */
    private Long paymentCompletedEvents;

    /**
     * Overall conversion rate (payments / views) as percentage.
     */
    private Double overallConversionRate;

    /**
     * Restaurant view to add-to-cart conversion rate.
     */
    private Double viewToCartRate;

    /**
     * Add-to-cart to checkout conversion rate.
     */
    private Double cartToCheckoutRate;

    /**
     * Checkout to payment conversion rate.
     */
    private Double checkoutToPaymentRate;

    /**
     * Funnel drop-off at each step.
     */
    private List<FunnelStep> funnelSteps;

    /**
     * Time period for the metrics (in days).
     */
    private Integer periodDays;

    /**
     * Reference date (end date for the period).
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate referenceDate;

    /**
     * Timestamp when this metric was calculated.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime calculatedAt;

    /**
     * Calculate all conversion rates and build funnel steps.
     */
    public void calculateConversionRates() {
        // Overall conversion rate
        if (restaurantViews != null && restaurantViews > 0) {
            this.overallConversionRate = paymentCompletedEvents != null ?
                    Math.round((double) paymentCompletedEvents / restaurantViews * 10000) / 100.0 : 0.0;
            this.viewToCartRate = addToCartEvents != null ?
                    Math.round((double) addToCartEvents / restaurantViews * 10000) / 100.0 : 0.0;
        }

        if (addToCartEvents != null && addToCartEvents > 0) {
            this.cartToCheckoutRate = checkoutStartEvents != null ?
                    Math.round((double) checkoutStartEvents / addToCartEvents * 10000) / 100.0 : 0.0;
        }

        if (checkoutStartEvents != null && checkoutStartEvents > 0) {
            this.checkoutToPaymentRate = paymentCompletedEvents != null ?
                    Math.round((double) paymentCompletedEvents / checkoutStartEvents * 10000) / 100.0 : 0.0;
        }

        // Build funnel steps with drop-off analysis
        this.funnelSteps = List.of(
                FunnelStep.builder()
                        .stepNumber(1)
                        .stepName("Restaurant View")
                        .count(restaurantViews != null ? restaurantViews : 0L)
                        .conversionFromPrevious(100.0)
                        .dropOffFromPrevious(0.0)
                        .build(),
                FunnelStep.builder()
                        .stepNumber(2)
                        .stepName("Add to Cart")
                        .count(addToCartEvents != null ? addToCartEvents : 0L)
                        .conversionFromPrevious(viewToCartRate != null ? viewToCartRate : 0.0)
                        .dropOffFromPrevious(viewToCartRate != null ? 100.0 - viewToCartRate : 100.0)
                        .build(),
                FunnelStep.builder()
                        .stepNumber(3)
                        .stepName("Checkout Started")
                        .count(checkoutStartEvents != null ? checkoutStartEvents : 0L)
                        .conversionFromPrevious(cartToCheckoutRate != null ? cartToCheckoutRate : 0.0)
                        .dropOffFromPrevious(cartToCheckoutRate != null ? 100.0 - cartToCheckoutRate : 100.0)
                        .build(),
                FunnelStep.builder()
                        .stepNumber(4)
                        .stepName("Payment Completed")
                        .count(paymentCompletedEvents != null ? paymentCompletedEvents : 0L)
                        .conversionFromPrevious(checkoutToPaymentRate != null ? checkoutToPaymentRate : 0.0)
                        .dropOffFromPrevious(checkoutToPaymentRate != null ? 100.0 - checkoutToPaymentRate : 100.0)
                        .build()
        );
    }

    /**
     * Inner class representing a single funnel step.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunnelStep {
        private Integer stepNumber;
        private String stepName;
        private Long count;
        private Double conversionFromPrevious;
        private Double dropOffFromPrevious;
    }
}
