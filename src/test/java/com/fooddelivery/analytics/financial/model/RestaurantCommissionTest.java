package com.fooddelivery.analytics.financial.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RestaurantCommission money math")
class RestaurantCommissionTest {

    @Test
    @DisplayName("PERCENTAGE: 15% of 100.00 = 15.00")
    void percentageCommission() {
        RestaurantCommission c = RestaurantCommission.builder()
                .restaurantId(1L).orderId(1L)
                .orderSubtotal(new BigDecimal("100.00"))
                .commissionRate(new BigDecimal("15.00"))
                .commissionType(CommissionType.PERCENTAGE)
                .build();

        c.calculateCommission();

        assertThat(c.getCommissionAmount()).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("PERCENTAGE rounds HALF_UP to 2 dp")
    void percentageRounding() {
        RestaurantCommission c = RestaurantCommission.builder()
                .restaurantId(1L).orderId(2L)
                .orderSubtotal(new BigDecimal("99.99"))
                .commissionRate(new BigDecimal("15.00"))
                .commissionType(CommissionType.PERCENTAGE)
                .build();

        c.calculateCommission();

        // 99.99 * 15 / 100 = 14.9985 -> 15.00
        assertThat(c.getCommissionAmount()).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("FIXED: uses fixed amount regardless of subtotal")
    void fixedCommission() {
        RestaurantCommission c = RestaurantCommission.builder()
                .restaurantId(1L).orderId(3L)
                .orderSubtotal(new BigDecimal("250.00"))
                .commissionRate(new BigDecimal("15.00"))
                .fixedCommission(new BigDecimal("5.00"))
                .commissionType(CommissionType.FIXED)
                .build();

        c.calculateCommission();

        assertThat(c.getCommissionAmount()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("HYBRID: fixed + percentage")
    void hybridCommission() {
        RestaurantCommission c = RestaurantCommission.builder()
                .restaurantId(1L).orderId(4L)
                .orderSubtotal(new BigDecimal("100.00"))
                .commissionRate(new BigDecimal("10.00"))
                .fixedCommission(new BigDecimal("2.50"))
                .commissionType(CommissionType.HYBRID)
                .build();

        c.calculateCommission();

        // 2.50 + (100.00 * 10 / 100 = 10.00) = 12.50
        assertThat(c.getCommissionAmount()).isEqualByComparingTo("12.50");
    }

    @Test
    @DisplayName("PERCENTAGE with zero subtotal = 0")
    void zeroSubtotal() {
        RestaurantCommission c = RestaurantCommission.builder()
                .restaurantId(1L).orderId(5L)
                .orderSubtotal(BigDecimal.ZERO)
                .commissionRate(new BigDecimal("15.00"))
                .commissionType(CommissionType.PERCENTAGE)
                .build();

        c.calculateCommission();

        assertThat(c.getCommissionAmount()).isEqualByComparingTo("0.00");
    }
}
