package com.fooddelivery.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for cancelling an order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cancel order request")
public class CancelOrderRequest {

    @Schema(description = "Cancellation reason", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Cancellation reason is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;

    /**
     * A stable identifier for the reason, so cancellations can be counted.
     *
     * <p>{@link #reason} is what the customer read on their screen, in their
     * language, which makes it free text in three languages and useless for
     * grouping. This is the same choice expressed once.
     *
     * <p>A constrained string rather than an enum on purpose: a value we have
     * not seen is recorded, not refused. An unknown enum constant fails binding
     * and answers 400 — at the moment a customer is trying to cancel an order,
     * which is the worst possible time to be strict about vocabulary.
     *
     * <p>Recommended: {@code WRONG_ADDRESS}, {@code ORDERED_BY_MISTAKE},
     * {@code TOO_SLOW}, {@code CHANGED_MIND}, {@code OTHER}.
     */
    @Schema(description = "Stable reason identifier for reporting, e.g. WRONG_ADDRESS",
            example = "WRONG_ADDRESS")
    @Pattern(regexp = "[A-Z][A-Z0-9_]{0,39}",
            message = "reasonCode must be upper-case letters, digits and underscores, e.g. WRONG_ADDRESS")
    private String reasonCode;

    @Schema(description = "Request refund")
    @Builder.Default
    private Boolean requestRefund = true;
}
