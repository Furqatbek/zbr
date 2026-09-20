package com.fooddelivery.common.security;

import com.fooddelivery.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Finishing a key rotation.
 *
 * <p>The rotation itself is configuration: make the new key primary, keep the
 * old one in {@code previous-keys}, restart. Nothing breaks at that point —
 * which is exactly why it is easy to stop there and leave the old key
 * configured forever. These two endpoints are what make the last step visible
 * and then done.
 */
@RestController
@RequestMapping("/api/v1/admin/security")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin — secrets", description = "Encryption key rotation")
public class SecretRotationController {

    private final SecretRewrapService rewrapService;

    @GetMapping("/rewrap")
    @Operation(summary = "How much is still under an old key",
            description = "remaining = 0 means every stored secret is under the current primary "
                    + "key and the previous keys can be removed from configuration.")
    public ResponseEntity<ApiResponse<SecretRewrapService.RewrapResult>> status() {
        return ResponseEntity.ok(ApiResponse.success(rewrapService.status()));
    }

    @PostMapping("/rewrap")
    @Operation(summary = "Re-encrypt stored secrets under the current primary key",
            description = "Safe to run repeatedly. A secret whose original key is not configured "
                    + "is reported rather than skipped silently — it has to be reissued.")
    public ResponseEntity<ApiResponse<SecretRewrapService.RewrapResult>> rewrap() {
        SecretRewrapService.RewrapResult result = rewrapService.rewrapPartnerCredentials();
        return ResponseEntity.ok(ApiResponse.success(
                result.getRemaining() == 0
                        ? "All stored secrets are under the current key. Previous keys can be removed."
                        : result.getRemaining() + " secret(s) could not be rewrapped — see failures.",
                result));
    }
}
