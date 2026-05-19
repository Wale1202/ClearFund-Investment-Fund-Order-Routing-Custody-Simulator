package com.clearfund.enums;

/**
 * Lifecycle of a {@link com.clearfund.entity.SettlementInstruction}.
 *
 * <ul>
 *   <li>{@code PENDING} — raised when the order reaches SETTLEMENT_PENDING.</li>
 *   <li>{@code SETTLED} — cash and units have moved; order is SETTLED.</li>
 *   <li>{@code FAILED}  — settlement could not complete (reserved for future
 *       async/retry handling; the synchronous path rolls back instead).</li>
 * </ul>
 */
public enum SettlementStatus {
    PENDING,
    SETTLED,
    FAILED
}
