package com.fooddelivery.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to move a restaurant to a different owner.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Restaurant ownership transfer")
public class TransferOwnershipRequest {

    /**
     * The user id, not an email. Resolve the email through
     * {@code GET /api/v1/users/search} first — accepting an email here would
     * make the destination of an irreversible transfer depend on a lookup the
     * caller never sees the result of.
     */
    @NotNull(message = "newOwnerId is required")
    @Schema(description = "User ID of the new owner", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long newOwnerId;
}
