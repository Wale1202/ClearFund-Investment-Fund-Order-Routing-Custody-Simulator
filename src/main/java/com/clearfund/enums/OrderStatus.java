package com.clearfund.enums;

import java.util.Set;

/**
 * Order lifecycle states and the legal transitions between them.
 *
 * <pre>
 * RECEIVED -> VALIDATED -> ROUTED -> ACCEPTED -> SETTLEMENT_PENDING -> SETTLED
 *     \           \          \
 *      \-----------+----------+--------> REJECTED   (with a stored reason)
 *      \-----------+----------+--------> CANCELLED  (caller requested)
 * </pre>
 *
 * Keeping the allowed transitions on the enum gives the service layer a
 * single source of truth for the state machine.
 */
public enum OrderStatus {

    RECEIVED,
    VALIDATED,
    ROUTED,
    ACCEPTED,
    SETTLEMENT_PENDING,
    SETTLED,
    REJECTED,
    CANCELLED;

    private Set<OrderStatus> nextStates = Set.of();

    static {
        RECEIVED.nextStates           = Set.of(VALIDATED, REJECTED, CANCELLED);
        VALIDATED.nextStates          = Set.of(ROUTED, REJECTED, CANCELLED);
        ROUTED.nextStates             = Set.of(ACCEPTED, REJECTED, CANCELLED);
        ACCEPTED.nextStates           = Set.of(SETTLEMENT_PENDING, CANCELLED);
        SETTLEMENT_PENDING.nextStates = Set.of(SETTLED, CANCELLED);
        // SETTLED, REJECTED, CANCELLED are terminal.
    }

    public boolean canTransitionTo(OrderStatus target) {
        return nextStates.contains(target);
    }

    public boolean isTerminal() {
        return nextStates.isEmpty();
    }
}
