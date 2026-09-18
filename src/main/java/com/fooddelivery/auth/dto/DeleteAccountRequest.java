package com.fooddelivery.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optional body of {@code DELETE /api/v1/auth/account}.
 *
 * <p>Every field is optional and nothing here is validated. A deletion request
 * must never be refused over the free-text reason attached to it: the apps
 * leave the field blank when the person says nothing, and Apple's review
 * explicitly checks that in-app deletion completes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Optional context for an account deletion")
public class DeleteAccountRequest {

    @Schema(description = "Free text the user typed when asked why they are leaving",
            example = "Switching to another platform")
    private String reason;
}
