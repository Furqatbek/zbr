package com.fooddelivery.compensation.controller;

import com.fooddelivery.compensation.dto.CompensationLogDto;
import com.fooddelivery.compensation.dto.CustomerCreditDto;
import com.fooddelivery.compensation.entity.CompensationLog;
import com.fooddelivery.compensation.entity.CustomerCredit;
import com.fooddelivery.compensation.mapper.CompensationMapper;
import com.fooddelivery.compensation.service.CompensationService;
import com.fooddelivery.compensation.service.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Customer-facing controller for credits and compensation history.
 */
@RestController
@RequestMapping("/api/v1/customer/credits")
@RequiredArgsConstructor
@Tag(name = "Customer Credits", description = "Customer credit and compensation endpoints")
@PreAuthorize("hasAnyRole('CONSUMER', 'CUSTOMER')")
public class CustomerCreditController {

    private final CreditService creditService;
    private final CompensationService compensationService;
    private final CompensationMapper mapper;

    @GetMapping("/balance")
    @Operation(summary = "Get my credit balance")
    public ResponseEntity<BigDecimal> getMyBalance(@AuthenticationPrincipal UserDetails user) {
        Long userId = extractUserId(user);
        return ResponseEntity.ok(creditService.getBalance(userId));
    }

    @GetMapping
    @Operation(summary = "Get my credit details")
    public ResponseEntity<CustomerCreditDto> getMyCreditDetails(@AuthenticationPrincipal UserDetails user) {
        Long userId = extractUserId(user);
        CustomerCredit credit = creditService.getOrCreateCredit(userId);
        return ResponseEntity.ok(mapper.toCreditDto(credit));
    }

    @GetMapping("/history")
    @Operation(summary = "Get my compensation history")
    public ResponseEntity<Page<CompensationLogDto>> getMyCompensationHistory(
            @AuthenticationPrincipal UserDetails user,
            Pageable pageable) {
        Long userId = extractUserId(user);
        Page<CompensationLog> logs = compensationService.getCompensationHistory(userId, pageable);
        return ResponseEntity.ok(logs.map(mapper::toLogDto));
    }

    /**
     * Extract user ID from UserDetails.
     * This assumes the username is the user ID or implements a custom UserDetails.
     */
    private Long extractUserId(UserDetails user) {
        // If using custom UserDetails with getId() method
        if (user instanceof com.fooddelivery.auth.security.UserPrincipal principal) {
            return principal.getId();
        }
        // Fallback: try parsing username as ID
        try {
            return Long.parseLong(user.getUsername());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Unable to extract user ID from authentication");
        }
    }
}
