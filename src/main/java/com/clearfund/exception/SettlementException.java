package com.clearfund.exception;

/**
 * Thrown when settlement cannot complete (e.g. insufficient cash or
 * holdings). It is a {@link BusinessRuleException} so the global handler
 * maps it to HTTP 409, and because it is a RuntimeException it triggers the
 * {@code @Transactional} rollback that guarantees no partial settlement.
 */
public class SettlementException extends BusinessRuleException {

    public SettlementException(String message) {
        super(message);
    }
}
