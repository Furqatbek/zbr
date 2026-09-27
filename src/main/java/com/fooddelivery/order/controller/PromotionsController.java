package com.fooddelivery.order.controller;

import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.order.dto.MyPromotionsDto;
import com.fooddelivery.order.service.DeliveryCreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * What the signed-in customer is currently owed.
 *
 * <p>Exists because the app refused to infer it. Eligibility was derivable from
 * an empty order list, and showing the poster on that basis would have promised
 * free delivery on the home screen while the checkout charged for it — a false
 * promise to a customer deciding whether to trust the platform at all. So the
 * server says, or nothing is shown.
 *
 * <p>Answered from the same credits the order path spends, so the promise and
 * the discount cannot disagree.
 */
@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
@Tag(name = "Promotions", description = "What this customer is currently owed")
@SecurityRequirement(name = "bearerAuth")
public class PromotionsController {

    private final DeliveryCreditService deliveryCreditService;

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "My promotions",
            description = "Whether the server will waive delivery on this customer's next order.")
    public ResponseEntity<ApiResponse<MyPromotionsDto>> myPromotions(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        boolean eligible = deliveryCreditService.hasSpendableCredit(currentUser.getId());
        boolean used = deliveryCreditService.hasSpentACredit(currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(MyPromotionsDto.builder()
                .firstOrderFreeDeliveryEligible(eligible)
                .firstOrderFreeDeliveryUsed(used)
                .firstOrderFreeDelivery(MyPromotionsDto.FirstOrderFreeDelivery.builder()
                        .eligible(eligible)
                        .used(used)
                        .build())
                .freeDeliveriesAvailable(deliveryCreditService.available(currentUser.getId()))
                .build()));
    }
}
