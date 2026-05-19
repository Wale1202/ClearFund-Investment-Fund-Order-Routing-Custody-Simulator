package com.clearfund.enums;

import java.util.Set;

/**
 * Order lifecycle states. Allowed transitions are encoded here so the
 * service layer has a single source of truth for the state machine.
 *
 * <pre>
 * RECEIVED -> VALIDATED -> ROUTED -> EXECUTED -> SETTLED
 *      \           \          \          \
 *       REJECTED    REJECTED   CANCELLED   FAILED
 * </pre>
 */
public enum OrderStatus {

    RECEIVED,
    VALIDATED,
    ROUTED,
    EXECUTED,
    SETTLED,
    REJECTED,
    CANCELLED,
    FAILED;

    private Set<OrderStatus> nextStates = Set.of();

    static {
        RECEIVED.nextStates = Set.of(VALIDATED, REJECTED);
        VALIDATED.nextStates = Set.of(ROUTED, REJECTED);
        ROUTED.nextStates = Set.of(EXECUTED, CANCELLED);
        EXECUTED.nextStates = Set.of(SETTLED, FAILED);
        // SETTLED, REJECTED, CANCELLED, FAILED are terminal.
    }

    public boolean canTransitionTo(OrderStatus target) {
        return nextStates.contains(target);
    }

    public boolean isTerminal() {
        return nextStates.isEmpty();
    }
}
