package com.clearfund.dto;

import java.time.Instant;

/** A single entry in an order's audit trail. */
public record AuditEventResponse(
        String fromStatus,
        String toStatus,
        String detail,
        Instant createdAt
) {
}
