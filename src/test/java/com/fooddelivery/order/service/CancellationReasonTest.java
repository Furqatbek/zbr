package com.fooddelivery.order.service;

import com.fooddelivery.order.dto.CancelOrderRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A cancellation reason that can be counted.
 *
 * <p>The app sent {@code reason} as the localised string it showed the customer,
 * so three people choosing the same reason sent "Wrong delivery address",
 * "Неверный адрес доставки" and "Noto'g'ri yetkazib berish manzili". The column
 * held free text in three languages, which meant the most operationally useful
 * signal in the flow — the address step failed — could not be detected without
 * matching strings per language.
 *
 * <p>{@code reasonCode} is a constrained string rather than an enum, and these
 * tests pin that choice: an unknown enum constant fails Jackson binding and
 * answers 400 while a customer is trying to cancel, which is the worst moment to
 * be strict about vocabulary. A value we have not seen is recorded instead.
 */
@DisplayName("Cancellation reason code")
class CancellationReasonTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private CancelOrderRequest request(String reasonCode) {
        CancelOrderRequest r = new CancelOrderRequest();
        r.setReason("Wrong delivery address");
        r.setReasonCode(reasonCode);
        return r;
    }

    @Test
    @DisplayName("a recommended code is accepted")
    void recommendedCodesPass() {
        for (String code : new String[]{"WRONG_ADDRESS", "ORDERED_BY_MISTAKE",
                "TOO_SLOW", "CHANGED_MIND", "OTHER"}) {
            assertThat(validator.validate(request(code)))
                    .withFailMessage("rejected %s", code)
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("a code we have never seen is accepted, not refused")
    void unknownCodeIsAccepted() {
        // The whole reason this is not an enum. A reason added in a shipped app
        // must not fail the cancel.
        assertThat(validator.validate(request("RESTAURANT_TOO_FAR"))).isEmpty();
    }

    @Test
    @DisplayName("no code at all is fine — the field is optional")
    void codeIsOptional() {
        // Clients that already cancel keep working without changing anything.
        assertThat(validator.validate(request(null))).isEmpty();
    }

    @Test
    @DisplayName("free text in the code field is refused")
    void displayTextIsRefused() {
        // The point is a value that groups. "Wrong delivery address" in here
        // would recreate exactly the problem this field exists to solve.
        assertThat(validator.validate(request("Wrong delivery address"))).isNotEmpty();
        assertThat(validator.validate(request("wrong-address"))).isNotEmpty();
    }

    @Test
    @DisplayName("the reason itself is still required")
    void reasonStillRequired() {
        CancelOrderRequest r = new CancelOrderRequest();
        r.setReasonCode("WRONG_ADDRESS");

        assertThat(validator.validate(r))
                .extracting(v -> v.getMessage())
                .contains("Cancellation reason is required");
    }
}
