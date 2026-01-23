package com.fooddelivery.order.service;

import com.fooddelivery.order.dto.PromoValidationRequest;
import com.fooddelivery.order.dto.PromoValidationResponse;
import com.fooddelivery.order.entity.PromoCode;
import com.fooddelivery.order.repository.PromoCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for promo code operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromoService {

    private final PromoCodeRepository promoCodeRepository;

    /**
     * Validate a promo code.
     */
    @Transactional(readOnly = true)
    public PromoValidationResponse validatePromoCode(PromoValidationRequest request) {
        // Find promo code
        PromoCode promo = promoCodeRepository.findByCodeIgnoreCase(request.getPromoCode()).orElse(null);

        if (promo == null) {
            return buildInvalidResponse(request.getPromoCode(), "Invalid promo code");
        }

        // Check if promo is active and valid
        if (!promo.isValid()) {
            if (!promo.getIsActive()) {
                return buildInvalidResponse(request.getPromoCode(), "This promo code is no longer active");
            }
            if (promo.getExpiresAt() != null && promo.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
                return buildInvalidResponse(request.getPromoCode(), "This promo code has expired");
            }
            if (promo.getUsageLimit() != null && promo.getUsageCount() >= promo.getUsageLimit()) {
                return buildInvalidResponse(request.getPromoCode(), "This promo code has reached its usage limit");
            }
            return buildInvalidResponse(request.getPromoCode(), "This promo code is not valid");
        }

        // Check restaurant restriction
        if (promo.getRestaurantId() != null && !promo.getRestaurantId().equals(request.getRestaurantId())) {
            return buildInvalidResponse(request.getPromoCode(), "This promo code is not valid for this restaurant");
        }

        // Check minimum order amount
        if (promo.getMinOrderAmount() != null && request.getSubtotal().compareTo(promo.getMinOrderAmount()) < 0) {
            return buildInvalidResponse(request.getPromoCode(),
                    "Minimum order amount of " + promo.getMinOrderAmount() + " required for this promo code");
        }

        // Calculate discount
        BigDecimal discountAmount = promo.calculateDiscount(request.getSubtotal());
        BigDecimal newTotal = request.getSubtotal().subtract(discountAmount);

        return PromoValidationResponse.builder()
                .valid(true)
                .promoCode(promo.getCode())
                .discountType(promo.getDiscountType().name())
                .discountValue(promo.getDiscountValue())
                .discountAmount(discountAmount)
                .newTotal(newTotal)
                .minimumOrder(promo.getMinOrderAmount())
                .maxDiscount(promo.getMaxDiscountAmount())
                .expiresAt(promo.getExpiresAt())
                .build();
    }

    private PromoValidationResponse buildInvalidResponse(String code, String errorMessage) {
        return PromoValidationResponse.builder()
                .valid(false)
                .promoCode(code)
                .errorMessage(errorMessage)
                .build();
    }
}
