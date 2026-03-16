package com.fooddelivery.platform.controller;

import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.common.dto.PagedResponse;
import com.fooddelivery.platform.dto.ApplyReferralRequest;
import com.fooddelivery.platform.dto.ReferralDto;
import jakarta.validation.Valid;
import com.fooddelivery.platform.service.ReferralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/referrals")
@RequiredArgsConstructor
@Tag(name = "Referrals", description = "Referral program endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ReferralController {

    private final ReferralService referralService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('CONSUMER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Generate referral code", description = "Generate a new referral code for the current user")
    public ResponseEntity<ApiResponse<ReferralDto>> generateCode(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        ReferralDto referral = referralService.generateReferralCode(currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Referral code generated", referral));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CONSUMER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Get my referrals", description = "Get referrals made by current user")
    public ResponseEntity<ApiResponse<PagedResponse<ReferralDto>>> getMyReferrals(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PageableDefault(size = 20) Pageable pageable) {

        PagedResponse<ReferralDto> referrals = referralService.getReferralsByUser(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(referrals));
    }

    @GetMapping("/my/stats")
    @PreAuthorize("hasAnyRole('CONSUMER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Get referral stats", description = "Get referral statistics for current user")
    public ResponseEntity<ApiResponse<ReferralService.ReferralStatsDto>> getMyStats(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        ReferralService.ReferralStatsDto stats = referralService.getUserReferralStats(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasAnyRole('CONSUMER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Get referral by code", description = "Get referral details by code")
    public ResponseEntity<ApiResponse<ReferralDto>> getReferralByCode(@PathVariable String code) {
        ReferralDto referral = referralService.getReferralByCode(code);
        return ResponseEntity.ok(ApiResponse.success(referral));
    }

    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('CONSUMER', 'PLATFORM', 'ADMIN')")
    @Operation(summary = "Apply referral code", description = "Apply a referral code to the current user's account")
    public ResponseEntity<ApiResponse<Void>> applyReferralCode(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody ApplyReferralRequest request) {

        referralService.processReferral(request.getCode(), currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Referral code applied successfully"));
    }
}
