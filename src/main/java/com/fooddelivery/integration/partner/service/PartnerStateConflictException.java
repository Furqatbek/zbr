package com.fooddelivery.integration.partner.service;

/**
 * Two systems disagree about where an order is.
 *
 * <p>Answered as 409 rather than the 422 we use for a bad transition
 * internally. This is not a malformed request — the partner asked for something
 * reasonable against a state that has moved underneath them, which is the
 * textbook conflict. Restos returns 409 for the mirror case, so a partner
 * seeing the same code in both directions can write one handler.
 *
 * <p>Deliberately NOT a subclass of InvalidOperationException: the handler for
 * that class would claim this one too and answer 422, because @ExceptionHandler
 * matches by assignability and a @ResponseStatus on the subclass does not
 * override an explicit handler.
 */
public class PartnerStateConflictException extends RuntimeException {
    public PartnerStateConflictException(String message) {
        super(message);
    }
}
