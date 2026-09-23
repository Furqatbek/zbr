package com.fooddelivery.common.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fooddelivery.common.exception.BusinessException;

/**
 * A request body that refuses fields it does not understand.
 *
 * <p>Jackson is configured leniently across this platform
 * ({@code fail-on-unknown-properties: false}), which is right for reading a
 * partner's payload — Restos may add a field tomorrow and our import should not
 * break — and wrong for a body we act on. An unsupported field that is quietly
 * dropped produces the most expensive failure there is: one that looks like it
 * worked. The customer app found three of those in a week.
 *
 * <p>Extending this makes the failure immediate and specific: 400, naming the
 * field. Deliberately NOT applied to everything. The cost of a false positive
 * differs by endpoint — an unexpected field on a menu edit is a typo worth
 * catching, while the same on checkout would stop a customer ordering because a
 * shipped app sends one field too many. Tighten where wrong data is the risk;
 * stay lenient where a refusal costs an order.
 */
public abstract class StrictRequest {

    @JsonAnySetter
    void rejectUnknownField(String name, Object value) {
        throw new BusinessException("Unknown field '" + name + "' in the request body. "
                + "Check the spelling, or the endpoint documentation for what this accepts.");
    }
}
