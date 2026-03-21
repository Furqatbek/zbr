package com.fooddelivery.compensation.controller;

import com.fooddelivery.compensation.dto.*;
import com.fooddelivery.compensation.entity.CompensationLog;
import com.fooddelivery.compensation.entity.CompensationRule;
import com.fooddelivery.compensation.entity.CustomerCredit;
import com.fooddelivery.compensation.mapper.CompensationMapper;
import com.fooddelivery.compensation.service.CompensationService;
import com.fooddelivery.compensation.service.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller for compensation management.
 */
@RestController
@RequestMapping("/api/v1/admin/compensations")
@RequiredArgsConstructor
@Tag(name = "Compensation", description = "Compensation management endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class CompensationController {

    private final CompensationService compensationService;
    private final CreditService creditService;
    private final CompensationMapper mapper;

    // === Rules Management ===

    @GetMapping("/rules")
    @Operation(summary = "Get all compensation rules")
    public ResponseEntity<List<CompensationRuleDto>> getAllRules() {
        List<CompensationRule> rules = compensationService.getAllRules();
        return ResponseEntity.ok(mapper.toRuleDtoList(rules));
    }

    @GetMapping("/rules/active")
    @Operation(summary = "Get active compensation rules")
    public ResponseEntity<List<CompensationRuleDto>> getActiveRules() {
        List<CompensationRule> rules = compensationService.getActiveRules();
        return ResponseEntity.ok(mapper.toRuleDtoList(rules));
    }

    @PatchMapping("/rules/{ruleId}/toggle")
    @Operation(summary = "Toggle rule active status")
    public ResponseEntity<CompensationRuleDto> toggleRuleStatus(@PathVariable Long ruleId) {
        CompensationRule rule = compensationService.toggleRuleStatus(ruleId);
        return ResponseEntity.ok(mapper.toRuleDto(rule));
    }

    // === Compensation Logs ===

    @GetMapping("/logs/user/{userId}")
    @Operation(summary = "Get compensation history for a user")
    public ResponseEntity<Page<CompensationLogDto>> getUserCompensationHistory(
            @PathVariable Long userId,
            Pageable pageable) {
        Page<CompensationLog> logs = compensationService.getCompensationHistory(userId, pageable);
        return ResponseEntity.ok(logs.map(mapper::toLogDto));
    }

    @GetMapping("/logs/order/{orderId}")
    @Operation(summary = "Get compensations for an order")
    public ResponseEntity<List<CompensationLogDto>> getOrderCompensations(@PathVariable Long orderId) {
        List<CompensationLog> logs = compensationService.getCompensationsForOrder(orderId);
        return ResponseEntity.ok(mapper.toLogDtoList(logs));
    }

    // === Manual Compensation ===

    @PostMapping("/manual")
    @Operation(summary = "Issue manual compensation")
    public ResponseEntity<CompensationLogDto> issueManualCompensation(
            @Valid @RequestBody ManualCompensationRequest request) {
        CompensationLog log = compensationService.issueManualCompensation(
                request.getUserId(),
                request.getOrderId(),
                request.getCompensationType(),
                request.getAmount(),
                request.getDescription()
        );
        return ResponseEntity.ok(mapper.toLogDto(log));
    }

    // === Customer Credits ===

    @GetMapping("/credits/{userId}")
    @Operation(summary = "Get customer credit balance")
    public ResponseEntity<CustomerCreditDto> getCustomerCredit(@PathVariable Long userId) {
        CustomerCredit credit = creditService.getOrCreateCredit(userId);
        return ResponseEntity.ok(mapper.toCreditDto(credit));
    }

    @GetMapping("/credits/{userId}/balance")
    @Operation(summary = "Get customer credit balance amount only")
    public ResponseEntity<BigDecimal> getCustomerCreditBalance(@PathVariable Long userId) {
        return ResponseEntity.ok(creditService.getBalance(userId));
    }

    @PostMapping("/credits/{userId}/add")
    @Operation(summary = "Add credit to customer account")
    public ResponseEntity<CustomerCreditDto> addCredit(
            @PathVariable Long userId,
            @RequestParam BigDecimal amount) {
        CustomerCredit credit = creditService.addCredit(userId, amount);
        return ResponseEntity.ok(mapper.toCreditDto(credit));
    }
}
