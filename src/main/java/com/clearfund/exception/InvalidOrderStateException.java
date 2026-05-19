package com.clearfund.exception;

import com.clearfund.enums.OrderStatus;

/**
 * Thrown when an order is asked to move to a status that the lifecycle
 * state machine does not allow from its current status (a caller/programming
 * error, distinct from a business rejection which is a normal outcome).
 */
public class InvalidOrderStateException extends BusinessRuleException {

    public InvalidOrderStateException(String orderRef, OrderStatus from, OrderStatus to) {
        super("Order %s cannot transition from %s to %s".formatted(orderRef, from, to));
    }
}
