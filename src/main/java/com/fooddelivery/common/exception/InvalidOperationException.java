package com.fooddelivery.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an operation is not valid in the current state.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidOperationException extends RuntimeException {

    private final String operation;
    private final String currentState;

    public InvalidOperationException(String message) {
        super(message);
        this.operation = null;
        this.currentState = null;
    }

    public InvalidOperationException(String operation, String currentState, String message) {
        super(String.format("Cannot perform '%s' operation. Current state: '%s'. %s",
                operation, currentState, message));
        this.operation = operation;
        this.currentState = currentState;
    }

    public String getOperation() {
        return operation;
    }

    public String getCurrentState() {
        return currentState;
    }
}
