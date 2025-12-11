package com.fooddelivery.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a payment operation fails.
 */
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class PaymentException extends RuntimeException {

    private final String paymentProvider;
    private final String errorCode;

    public PaymentException(String message) {
        super(message);
        this.paymentProvider = null;
        this.errorCode = null;
    }

    public PaymentException(String message, String paymentProvider, String errorCode) {
        super(message);
        this.paymentProvider = paymentProvider;
        this.errorCode = errorCode;
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
        this.paymentProvider = null;
        this.errorCode = null;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
