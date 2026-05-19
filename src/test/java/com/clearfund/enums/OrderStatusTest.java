package com.clearfund.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for the lifecycle state machine. Keeping the transition
 * rules verifiable in isolation means the services can trust
 * {@code canTransitionTo} without re-testing every path.
 */
class OrderStatusTest {

    @Test
    void happyPathTransitionsAreAllowed() {
        assertThat(OrderStatus.RECEIVED.canTransitionTo(OrderStatus.VALIDATED)).isTrue();
        assertThat(OrderStatus.VALIDATED.canTransitionTo(OrderStatus.ROUTED)).isTrue();
        assertThat(OrderStatus.ROUTED.canTransitionTo(OrderStatus.ACCEPTED)).isTrue();
        assertThat(OrderStatus.ACCEPTED.canTransitionTo(OrderStatus.SETTLEMENT_PENDING)).isTrue();
        assertThat(OrderStatus.SETTLEMENT_PENDING.canTransitionTo(OrderStatus.SETTLED)).isTrue();
    }

    @Test
    void skippingAStageIsRejected() {
        assertThat(OrderStatus.RECEIVED.canTransitionTo(OrderStatus.ROUTED)).isFalse();
        assertThat(OrderStatus.RECEIVED.canTransitionTo(OrderStatus.SETTLED)).isFalse();
        assertThat(OrderStatus.VALIDATED.canTransitionTo(OrderStatus.ACCEPTED)).isFalse();
    }

    @Test
    void everyNonTerminalStateCanBeRejectedOrCancelled() {
        assertThat(OrderStatus.RECEIVED.canTransitionTo(OrderStatus.REJECTED)).isTrue();
        assertThat(OrderStatus.VALIDATED.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.ROUTED.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(names = {"SETTLED", "REJECTED", "CANCELLED"})
    void terminalStatesAllowNoFurtherTransitions(OrderStatus terminal) {
        assertThat(terminal.isTerminal()).isTrue();
        for (OrderStatus target : OrderStatus.values()) {
            assertThat(terminal.canTransitionTo(target)).isFalse();
        }
    }

    @ParameterizedTest
    @EnumSource(names = {"RECEIVED", "VALIDATED", "ROUTED", "ACCEPTED", "SETTLEMENT_PENDING"})
    void inFlightStatesAreNotTerminal(OrderStatus inFlight) {
        assertThat(inFlight.isTerminal()).isFalse();
    }
}
