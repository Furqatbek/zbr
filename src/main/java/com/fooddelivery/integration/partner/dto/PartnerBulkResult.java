package com.fooddelivery.integration.partner.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * The outcome of a bulk menu update.
 *
 * <p>Partial success is reported rather than rolled back. A markup change across
 * four hundred dishes must not be lost because three of them were deleted here
 * last week — the partner gets the three back by id and can reconcile them,
 * while the other 397 prices are already live.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Result of a bulk menu update")
public class PartnerBulkResult {

    private int updated;

    /**
     * Every id we could not find, in one response. Restos does the same on
     * their side for the same reason: one round trip per unknown id turns a
     * reconciliation into an afternoon.
     */
    @Builder.Default
    @Schema(description = "Partner product ids that matched no item in this venue")
    private List<String> unknownItemIds = new ArrayList<>();

    @Builder.Default
    @Schema(description = "Updates that were rejected, with the reason")
    private List<String> rejected = new ArrayList<>();
}
