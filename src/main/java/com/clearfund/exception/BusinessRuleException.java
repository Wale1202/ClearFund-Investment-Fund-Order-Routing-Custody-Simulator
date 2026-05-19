package com.clearfund.exception;

/**
 * Thrown when a request is well-formed but violates a domain rule
 * (e.g. fund closed, account suspended, illegal status transition,
 * insufficient units for a redemption).
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
