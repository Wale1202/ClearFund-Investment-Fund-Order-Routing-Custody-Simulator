package com.clearfund.dto;

import com.clearfund.enums.OrderStatus;
import com.clearfund.enums.OrderType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Read model returned to API clients. Entities are never serialized directly.
 */
public record OrderResponse(
        String orderRef,
        String accountRef,
        String fundCode,
        OrderType orderType,
        OrderStatus status,
        BigDecimal cashAmount,
        BigDecimal units,
        BigDecimal navUsed,
        LocalDate tradeDate,
        LocalDate settlementDate,
        String rejectReason,
        Instant createdAt
) {
}
