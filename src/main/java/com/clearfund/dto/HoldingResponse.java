package com.clearfund.dto;

import java.math.BigDecimal;

/** A single custody position for an account. */
public record HoldingResponse(
        String accountRef,
        String fundCode,
        String fundName,
        BigDecimal units
) {
}
