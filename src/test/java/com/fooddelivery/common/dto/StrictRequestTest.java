package com.fooddelivery.common.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.integration.restos.dto.RestosProduct;
import com.fooddelivery.restaurant.dto.SaveItemOptionRequest;
import com.fooddelivery.restaurant.dto.SaveItemVariantRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A field we do not understand is refused, not absorbed.
 *
 * <p>Jackson is lenient platform-wide, which is right for a partner's payload —
 * Restos may add a field tomorrow and our import should not break — and wrong
 * for a body we act on. An unsupported field silently dropped produces the
 * failure that looks like success, which is the most expensive kind: it
 * survives testing and shows up later as wrong data.
 *
 * <p>The customer app found three of that shape in a week, which is why this is
 * a rule and not a patch.
 */
@DisplayName("Strict request bodies")
class StrictRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("an unknown field is refused, and named")
    void unknownFieldIsRefused() {
        String json = """
                {"name": "Large", "priceDelta": 8000, "prise": 9000}
                """;

        assertThatThrownBy(() -> mapper.readValue(json, SaveItemVariantRequest.class))
                .rootCause()
                .isInstanceOf(BusinessException.class)
                // The typo itself, so the fix is obvious from the response.
                .hasMessageContaining("prise");
    }

    @Test
    @DisplayName("a body using only known fields is read normally")
    void knownFieldsAreFine() {
        String json = """
                {"groupName": "Extras", "name": "Cheese", "priceDelta": 5000,
                 "maxSelections": 3, "required": false, "inStock": true}
                """;

        assertThatCode(() -> {
            SaveItemOptionRequest request = mapper.readValue(json, SaveItemOptionRequest.class);
            assertThat(request.getName()).isEqualTo("Cheese");
            assertThat(request.getMaxSelections()).isEqualTo(3);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a field that exists but is misspelled in case is still refused")
    void caseMattersToo() {
        // Jackson matches names exactly, so "maxselections" would have been
        // dropped and the cap silently left at its default.
        String json = """
                {"name": "Cheese", "maxselections": 5}
                """;

        assertThatThrownBy(() -> mapper.readValue(json, SaveItemOptionRequest.class))
                .rootCause()
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("maxselections");
    }

    @Test
    @DisplayName("a partner's payload stays lenient — that leniency is deliberate")
    void partnerPayloadsRemainLenient() {
        // Restos sends createdAt, updatedAt, marginPercentage and more that we
        // do not model. Refusing those would break the menu import the day they
        // add a column, which is exactly the wrong failure for an inbound feed.
        String json = """
                {"id": 88, "name": "Ice Latte", "price": 32000,
                 "createdAt": "2026-05-06T12:38:56", "somethingNewNextYear": true}
                """;

        assertThatCode(() -> {
            RestosProduct product = mapper.readValue(json, RestosProduct.class);
            assertThat(product.getName()).isEqualTo("Ice Latte");
        }).doesNotThrowAnyException();
    }
}
