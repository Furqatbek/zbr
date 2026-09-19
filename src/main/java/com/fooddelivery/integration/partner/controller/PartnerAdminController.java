package com.fooddelivery.integration.partner.controller;

import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.integration.partner.entity.Partner;
import com.fooddelivery.integration.partner.entity.PartnerCapability;
import com.fooddelivery.integration.partner.entity.PartnerKey;
import com.fooddelivery.integration.partner.entity.PartnerVenueGrant;
import com.fooddelivery.integration.partner.service.PartnerAccessService;
import com.fooddelivery.integration.partner.service.PartnerKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Administering partners, their credentials and their venue grants.
 *
 * <p>Under {@code /admin/} rather than {@code /partner/} deliberately: a partner
 * must never be able to widen its own access, and putting these under the
 * partner prefix would place them behind the partner key filter where a
 * misconfigured rule could expose exactly that.
 */
@RestController
@RequestMapping("/api/v1/admin/partners")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin — partners", description = "Issue and revoke partner API access")
public class PartnerAdminController {

    private final PartnerKeyService keyService;
    private final PartnerAccessService accessService;

    @PostMapping
    @Operation(summary = "Register a partner")
    public ResponseEntity<ApiResponse<PartnerView>> register(@Valid @RequestBody RegisterPartnerBody body) {
        Partner partner = keyService.registerPartner(body.getCode(), body.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Partner registered", PartnerView.of(partner)));
    }

    @PostMapping("/{partnerId}/keys")
    @Operation(summary = "Issue an API key",
            description = "The secret is returned ONCE and cannot be retrieved again. "
                    + "If it is lost, issue a new key and revoke the old one.")
    public ResponseEntity<ApiResponse<PartnerKeyService.IssuedKey>> issueKey(
            @PathVariable Long partnerId,
            @RequestBody(required = false) IssueKeyBody body) {

        String label = body != null ? body.getLabel() : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Key issued. Copy the secret now — it is not stored and cannot be shown again.",
                        keyService.issueKey(partnerId, label)));
    }

    @GetMapping("/{partnerId}/keys")
    @Operation(summary = "List a partner's keys",
            description = "Metadata only. Secrets are stored as digests and are not recoverable.")
    public ResponseEntity<ApiResponse<List<KeyView>>> listKeys(@PathVariable Long partnerId) {
        return ResponseEntity.ok(ApiResponse.success(
                keyService.listKeys(partnerId).stream().map(KeyView::of).toList()));
    }

    @DeleteMapping("/keys/{keyId}")
    @Operation(summary = "Revoke an API key", description = "Takes effect on the next request.")
    public ResponseEntity<ApiResponse<Void>> revokeKey(@PathVariable Long keyId) {
        keyService.revokeKey(keyId);
        return ResponseEntity.ok(ApiResponse.success("Key revoked"));
    }

    @PutMapping("/{partnerId}/venues")
    @Operation(summary = "Grant or update access to a venue",
            description = "Maps the partner's own venue id to one of our restaurants and says what "
                    + "they may do to it. Sending an empty capability set leaves the mapping in "
                    + "place but removes every permission.")
    public ResponseEntity<ApiResponse<GrantView>> grantVenue(
            @PathVariable Long partnerId,
            @Valid @RequestBody GrantVenueBody body) {

        PartnerVenueGrant grant = accessService.grantVenue(
                partnerId, body.getRestaurantId(), body.getExternalVenueId(),
                body.getCapabilities(), body.getPushOrders());
        return ResponseEntity.ok(ApiResponse.success("Venue granted", GrantView.of(grant)));
    }

    @PutMapping("/{partnerId}/outbound")
    @Operation(summary = "Configure where we push orders to this partner",
            description = "The API key is the credential THEY issued US. It is stored, never "
                    + "returned and never logged. Omit it to change the URL without re-sending it.")
    public ResponseEntity<ApiResponse<PartnerView>> configureOutbound(
            @PathVariable Long partnerId,
            @RequestBody OutboundBody body) {

        Partner partner = accessService.configureOutbound(
                partnerId, body.getBaseUrl(), body.getApiKey(), body.getAuthHeader());
        return ResponseEntity.ok(ApiResponse.success("Outbound configured", PartnerView.of(partner)));
    }

    @GetMapping("/{partnerId}/venues")
    @Operation(summary = "List a partner's venue grants")
    public ResponseEntity<ApiResponse<List<GrantView>>> listGrants(@PathVariable Long partnerId) {
        return ResponseEntity.ok(ApiResponse.success(
                accessService.listGrants(partnerId).stream().map(GrantView::of).toList()));
    }

    @DeleteMapping("/{partnerId}/venues/{restaurantId}")
    @Operation(summary = "Revoke a partner's access to a venue")
    public ResponseEntity<ApiResponse<Void>> revokeVenue(@PathVariable Long partnerId,
                                                         @PathVariable Long restaurantId) {
        accessService.revokeVenue(partnerId, restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Venue access revoked"));
    }

    // --- request bodies ----------------------------------------------------

    @Data
    public static class RegisterPartnerBody {
        @NotBlank(message = "code is required")
        private String code;
        private String name;
    }

    @Data
    public static class IssueKeyBody {
        private String label;
    }

    @Data
    public static class GrantVenueBody {
        @NotNull(message = "restaurantId is required")
        private Long restaurantId;

        @NotBlank(message = "externalVenueId is required")
        private String externalVenueId;

        /**
         * Absent means none. A grant that can do nothing is the safe default —
         * mapping a venue and authorising writes into its kitchen are separate
         * decisions, and the second one should have to be typed.
         */
        private Set<PartnerCapability> capabilities;

        /**
         * Whether this venue's orders print on the partner's till. Absent
         * leaves it as it was, so editing capabilities cannot silently switch a
         * kitchen over — or silently switch it back.
         */
        private Boolean pushOrders;
    }

    @Data
    public static class OutboundBody {
        private String baseUrl;
        /** Write-only. Omit to leave the stored credential untouched. */
        private String apiKey;
        /** Defaults to Authorization: Bearer when absent. */
        private String authHeader;
    }

    // --- responses ---------------------------------------------------------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartnerView {
        private Long id;
        private String code;
        private String name;
        private Boolean active;

        static PartnerView of(Partner partner) {
            return PartnerView.builder()
                    .id(partner.getId()).code(partner.getCode())
                    .name(partner.getName()).active(partner.getActive())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyView {
        private Long id;
        /** The public half only. The secret is a digest and is never returned. */
        private String keyId;
        private String environment;
        private String label;
        private Boolean active;
        private LocalDateTime createdAt;
        private LocalDateTime revokedAt;
        private LocalDateTime lastUsedAt;

        static KeyView of(PartnerKey key) {
            return KeyView.builder()
                    .id(key.getId()).keyId(key.getKeyId()).environment(key.getEnvironment())
                    .label(key.getLabel()).active(key.getActive())
                    .createdAt(key.getCreatedAt()).revokedAt(key.getRevokedAt())
                    .lastUsedAt(key.getLastUsedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrantView {
        private Long id;
        private Long restaurantId;
        private String restaurantName;
        private String externalVenueId;
        private Set<PartnerCapability> capabilities;
        private Boolean pushOrders;

        static GrantView of(PartnerVenueGrant grant) {
            return GrantView.builder()
                    .id(grant.getId())
                    .restaurantId(grant.getRestaurant().getId())
                    .restaurantName(grant.getRestaurant().getName())
                    .externalVenueId(grant.getExternalVenueId())
                    .capabilities(grant.getCapabilities())
                    .pushOrders(grant.isPushOrders())
                    .build();
        }
    }
}
