package com.fooddelivery.platform.controller;

import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.order.repository.PaymentRepository;
import com.fooddelivery.platform.dto.AnalyticsDto;
import com.fooddelivery.platform.dto.ReferralDto;
import com.fooddelivery.platform.service.ReferralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Platform administration endpoints")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM')")
public class AdminController {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ReferralService referralService;

    @org.springframework.beans.factory.annotation.Value("${app.currency:UZS}")
    private String currency = "UZS";

    @GetMapping("/analytics/orders")
    @Operation(summary = "Get order analytics", description = "Get basic order metrics")
    public ResponseEntity<ApiResponse<AnalyticsDto>> getOrderAnalytics(
            @RequestParam(defaultValue = "7") int days) {

        LocalDateTime since = LocalDateTime.now().minusDays(days);

        long totalOrders = orderRepository.countByStatusSince(
                com.fooddelivery.order.entity.OrderStatus.COMPLETED, since);
        BigDecimal gmv = orderRepository.sumCompletedOrdersGMVSince(since);
        BigDecimal avgTicket = orderRepository.avgOrderValueSince(since);

        AnalyticsDto analytics = AnalyticsDto.builder()
                .periodDays(days)
                .totalOrders(totalOrders)
                .gmv(gmv != null ? gmv : BigDecimal.ZERO)
                .averageOrderValue(avgTicket != null ? avgTicket : BigDecimal.ZERO)
                .currency(currency)
                .build();

        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @GetMapping("/referrals")
    @Operation(summary = "Get all referrals", description = "List all referrals in the system")
    public ResponseEntity<ApiResponse<PagedResponse<ReferralDto>>> getAllReferrals(
            @PageableDefault(size = 20) Pageable pageable) {

        PagedResponse<ReferralDto> referrals = referralService.getAllReferrals(pageable);
        return ResponseEntity.ok(ApiResponse.success(referrals));
    }
}
