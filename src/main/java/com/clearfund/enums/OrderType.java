package com.clearfund.enums;

/**
 * The two order kinds ClearFund supports.
 *
 * <ul>
 *   <li>{@code SUBSCRIPTION} — investor pays cash to acquire fund units.</li>
 *   <li>{@code REDEMPTION}   — investor sells fund units back for cash.</li>
 * </ul>
 */
public enum OrderType {
    SUBSCRIPTION,
    REDEMPTION
}
