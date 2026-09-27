package com.fooddelivery.order.service;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.order.dto.PromoValidationRequest;
import com.fooddelivery.order.dto.PromoValidationResponse;
import com.fooddelivery.order.entity.PromoCode;
import com.fooddelivery.order.entity.PromoCodeUsage;
import com.fooddelivery.order.repository.PromoCodeRepository;
import com.fooddelivery.order.repository.PromoCodeUsageRepository;
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
    private final PromoCodeUsageRepository usageRepository;

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

        // What the code is worth. A FREE_DELIVERY code is worth the delivery fee,
        // so without one it cannot be priced — and answering 0 would display "you
        // save nothing" for a promotion that works. Absent is the honest answer;
        // discountType tells the client to render words instead of a number.
        boolean freeDelivery = promo.getDiscountType() == PromoCode.DiscountType.FREE_DELIVERY;
        BigDecimal discountAmount;
        if (freeDelivery && request.getDeliveryFee() == null) {
            discountAmount = null;
        } else {
            discountAmount = promo.calculateDiscount(request.getSubtotal(),
                    request.getDeliveryFee() != null ? request.getDeliveryFee() : BigDecimal.ZERO);
        }

        // A free delivery does not reduce the food, so the basket total is
        // unchanged by it.
        BigDecimal newTotal = (discountAmount == null || freeDelivery)
                ? request.getSubtotal()
                : request.getSubtotal().subtract(discountAmount);

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

    /**
     * Claim a promo code for an order that is about to be created.
     *
     * <p>Everything {@link #validatePromoCode} checks, plus the two things it
     * cannot: the per-customer limit, which needs to know who is asking, and
     * the global limit, which has to be taken atomically or two simultaneous
     * checkouts both spend the last use.
     *
     * <p>Validation and application are deliberately separate calls with the
     * same rules. A code can stop being usable between the customer seeing
     * "−5 000 so'm applied" and tapping pay — it expires, someone else takes
     * the last one — and the order is what must be right.
     *
     * @return the code and what it is worth on this subtotal
     * @throws BusinessException with a message written for the customer
     */
    @Transactional
    public Claim claim(String code, Long userId, Long restaurantId,
                       BigDecimal subtotal, BigDecimal deliveryFee) {
        PromoCode promo = promoCodeRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BusinessException("Invalid promo code"));

        if (!promo.isValid()) {
            throw new BusinessException(reasonItIsNotValid(promo));
        }
        if (promo.getRestaurantId() != null && !promo.getRestaurantId().equals(restaurantId)) {
            throw new BusinessException("This promo code is not valid for this restaurant");
        }
        if (promo.getMinOrderAmount() != null && subtotal.compareTo(promo.getMinOrderAmount()) < 0) {
            throw new BusinessException("This promo code needs a minimum order of "
                    + promo.getMinOrderAmount());
        }

        // The per-customer limit, which validate cannot check because it is not
        // told who is asking.
        int perUser = promo.getUserUsageLimit() != null ? promo.getUserUsageLimit() : 1;
        if (perUser > 0 && usageRepository.countByPromoCodeIdAndUserId(promo.getId(), userId) >= perUser) {
            throw new BusinessException(perUser == 1
                    ? "You have already used this promo code"
                    : "You have used this promo code the maximum number of times");
        }

        if (promoCodeRepository.claimOneUse(promo.getId()) == 0) {
            throw new BusinessException("This promo code has reached its usage limit");
        }

        BigDecimal discount = promo.calculateDiscount(subtotal, deliveryFee);
        log.info("Promo {} ({}) claimed by user {} on restaurant {}: -{}",
                promo.getCode(), promo.getDiscountType(), userId, restaurantId, discount);
        return new Claim(promo.getId(), promo.getCode(), discount,
                promo.getDiscountType() == PromoCode.DiscountType.FREE_DELIVERY);
    }

    /**
     * Record that a claimed code was used on this order.
     *
     * <p>Separate from {@link #claim} only because the order has no id until it
     * is saved.
     */
    @Transactional
    public void recordUsage(Claim claim, Long userId, Long orderId) {
        usageRepository.save(PromoCodeUsage.builder()
                .promoCodeId(claim.promoCodeId())
                .userId(userId)
                .orderId(orderId)
                .discountAmount(claim.discount())
                .build());
    }

    /**
     * A code taken for an order, and what it took off.
     *
     * @param freeDelivery true when the code already paid for the delivery, so
     *                     a delivery credit must not be spent on the same order
     */
    public record Claim(Long promoCodeId, String code, BigDecimal discount, boolean freeDelivery) {
    }

    private String reasonItIsNotValid(PromoCode promo) {
        if (!promo.getIsActive()) {
            return "This promo code is no longer active";
        }
        if (promo.getStartsAt() != null && promo.getStartsAt().isAfter(java.time.LocalDateTime.now())) {
            return "This promo code is not active yet";
        }
        if (promo.getExpiresAt() != null && promo.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            return "This promo code has expired";
        }
        if (promo.getUsageLimit() != null && promo.getUsageCount() >= promo.getUsageLimit()) {
            return "This promo code has reached its usage limit";
        }
        return "This promo code is not valid";
    }

    private PromoValidationResponse buildInvalidResponse(String code, String errorMessage) {
        return PromoValidationResponse.builder()
                .valid(false)
                .promoCode(code)
                .errorMessage(errorMessage)
                .build();
    }
}
