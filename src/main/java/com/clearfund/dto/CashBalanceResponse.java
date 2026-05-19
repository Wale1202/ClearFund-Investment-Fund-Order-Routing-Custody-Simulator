package com.clearfund.dto;

import java.math.BigDecimal;

/** An account's cash position in one currency. */
public record CashBalanceResponse(
        String accountRef,
        String currency,
        BigDecimal amount
) {
}
