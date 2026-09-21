package com.fooddelivery.integration.restos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fooddelivery.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A menu someone already has, for a venue whose system we cannot reach.
 *
 * <p>Takes the venue's own response body, unedited. Whoever can reach them runs
 * the curl and pastes the result; asking an operator to reshape JSON by hand
 * before an import is asking for a menu that differs from the venue's in ways
 * nobody can later reconstruct.
 *
 * <p>Two ways to pass it, because the two things an operator has in hand are
 * the whole response and the array inside it:
 *
 * <pre>
 * { "payload":    { "success": true, "data": [ ...categories... ] } }
 * { "categories": [ ...categories... ] }
 * </pre>
 *
 * Exactly one. Both, or neither, is a mistake worth failing on rather than
 * guessing at — the wrong guess imports half a menu and deactivates the rest.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Import a menu payload the caller already has, with no outbound fetch")
public class SuppliedMenuImportRequest {

    @Schema(description = "The venue's id in their system, recorded against the import")
    @NotNull(message = "External restaurant ID is required")
    private Long externalRestaurantId;

    @Schema(description = "The venue's response body exactly as their API returned it")
    private RestosApiResponse<List<RestosCategory>> payload;

    @Schema(description = "Just the categories, if the envelope was already unwrapped")
    private List<RestosCategory> categories;

    @Schema(description = "False creates what is new and leaves existing items alone; "
            + "true updates in place and may deactivate what is missing")
    @Builder.Default
    private Boolean overwriteExisting = false;

    /**
     * The categories to import, from whichever form was sent.
     *
     * @throws BusinessException when the request is ambiguous or the payload
     *         carries an upstream failure — a menu is never imported from a
     *         response that said it failed
     */
    public List<RestosCategory> resolveCategories() {
        if (payload != null && categories != null) {
            throw new BusinessException("Send either 'payload' or 'categories', not both");
        }

        List<RestosCategory> resolved;
        if (payload != null) {
            if (!payload.isSuccess()) {
                throw new BusinessException("The supplied payload reports failure upstream: "
                        + payload.getMessage());
            }
            resolved = payload.getData();
        } else {
            resolved = categories;
        }

        if (resolved == null || resolved.isEmpty()) {
            throw new BusinessException("No menu in the request: send 'payload' (the venue's whole "
                    + "response) or 'categories' (the array inside it)");
        }
        return resolved;
    }
}
