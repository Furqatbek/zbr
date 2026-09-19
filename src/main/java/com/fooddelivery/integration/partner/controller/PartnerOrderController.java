package com.fooddelivery.integration.partner.controller;

import com.fooddelivery.common.annotation.RateLimited;
import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.integration.partner.dto.PartnerOrderStatus;
import com.fooddelivery.integration.partner.security.PartnerPrincipal;
import com.fooddelivery.integration.partner.service.PartnerOrderService;
import com.fooddelivery.order.dto.OrderDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Order progress reported by the venue's till.
 *
 * <p>Orders are addressed by OUR reference ({@code FD-YYYYMMDD-XXXXXX}) because
 * the order is ours — it was created here and pushed to them. Menus go the
 * other way and use their ids. Which side owns the thing decides whose
 * identifier names it.
 */
@RestController
@RequestMapping("/api/v1/partner/orders")
@RequiredArgsConstructor
@Tag(name = "Partner — orders", description = "Order status reported by an integrated POS")
public class PartnerOrderController {

    private final PartnerOrderService partnerOrderService;

    @PostMapping("/{externalOrderNo}/status")
    @RateLimited(requestsPerMinute = 120, burstCapacity = 240, keyType = RateLimited.KeyType.PARTNER)
    @Operation(summary = "Report an order's status",
            description = "Safe to retry: reporting a status the order already has succeeds and "
                    + "changes nothing. A status the order cannot reach answers 409 naming both, "
                    + "so a genuine disagreement is visible rather than silently applied.")
    public ResponseEntity<ApiResponse<OrderDto>> reportStatus(
            @AuthenticationPrincipal PartnerPrincipal partner,
            @PathVariable String externalOrderNo,
            @Valid @RequestBody StatusReport body) {

        OrderDto order = partnerOrderService.report(
                partner, externalOrderNo, body.getStatus(), body.getReason());
        return ResponseEntity.ok(ApiResponse.success("Status recorded", order));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusReport {

        @NotNull(message = "status is required")
        private PartnerOrderStatus status;

        /**
         * Why, in the venue's words. Shown to the customer when an order is
         * declined, so it is worth sending something better than "declined" —
         * "we have run out of lamb" is a different conversation from silence.
         */
        @Size(max = 500, message = "reason is too long")
        private String reason;
    }
}
