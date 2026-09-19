package com.fooddelivery.integration.partner.controller;

import com.fooddelivery.common.annotation.RateLimited;
import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.integration.partner.dto.PartnerBulkResult;
import com.fooddelivery.integration.partner.dto.PartnerItemUpdate;
import com.fooddelivery.integration.partner.entity.PartnerCapability;
import com.fooddelivery.integration.partner.security.PartnerPrincipal;
import com.fooddelivery.integration.partner.service.PartnerAccessService;
import com.fooddelivery.integration.partner.service.PartnerMenuService;
import com.fooddelivery.restaurant.entity.Restaurant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Menu changes pushed by a partner POS.
 *
 * <p>Everything here is addressed by the PARTNER's identifiers — their venue id
 * in the path, their product id in the body — so an integrator never stores
 * ours. Restos asked us to hold to that and they were right that it is far
 * cheaper now than after a year of orders have gone through.
 */
@RestController
@RequestMapping("/api/v1/partner/venues/{externalVenueId}/menu")
@RequiredArgsConstructor
@Tag(name = "Partner — menu", description = "Price and availability updates from an integrated POS")
public class PartnerMenuController {

    private final PartnerAccessService accessService;
    private final PartnerMenuService menuService;

    @PatchMapping("/items/{externalItemId}")
    @RateLimited(requestsPerMinute = 60, burstCapacity = 120, keyType = RateLimited.KeyType.PARTNER)
    @Operation(summary = "Update one item",
            description = "Change price and/or availability of a single item. Omitted fields are "
                    + "left unchanged — this is a partial update, not a replacement.")
    public ResponseEntity<ApiResponse<Void>> updateItem(
            @AuthenticationPrincipal PartnerPrincipal partner,
            @PathVariable String externalVenueId,
            @PathVariable String externalItemId,
            @Valid @RequestBody PartnerItemUpdate body) {

        Restaurant restaurant = accessService.resolveVenue(
                partner, externalVenueId, PartnerCapability.MENU_WRITE);

        // The path is authoritative for which item is being changed; a body that
        // also carries an id must not be able to redirect the write somewhere
        // else, so the path value is copied over it rather than compared.
        body.setExternalItemId(externalItemId);

        menuService.applyUpdate(restaurant, partner.getPartnerCode(), body);
        return ResponseEntity.ok(ApiResponse.success("Item updated"));
    }

    @PostMapping("/items")
    @RateLimited(requestsPerMinute = 60, burstCapacity = 120, keyType = RateLimited.KeyType.PARTNER)
    @Operation(summary = "Update many items",
            description = "One call for a menu-wide change. Reports what it could not apply rather "
                    + "than rolling the whole batch back.")
    public ResponseEntity<ApiResponse<PartnerBulkResult>> updateItems(
            @AuthenticationPrincipal PartnerPrincipal partner,
            @PathVariable String externalVenueId,
            @RequestBody @Valid @NotEmpty(message = "At least one item is required")
            @Size(max = 1000, message = "At most 1000 items per call")
            List<@Valid PartnerItemUpdate> body) {

        Restaurant restaurant = accessService.resolveVenue(
                partner, externalVenueId, PartnerCapability.MENU_WRITE);

        PartnerBulkResult result = menuService.applyUpdates(restaurant, partner.getPartnerCode(), body);
        return ResponseEntity.ok(ApiResponse.success("Menu updated", result));
    }
}
