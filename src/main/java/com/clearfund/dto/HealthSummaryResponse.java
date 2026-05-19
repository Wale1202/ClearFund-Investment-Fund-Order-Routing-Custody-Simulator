package com.clearfund.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Lightweight operational snapshot for the dashboard / production-support
 * view: counts of core entities and a breakdown of orders by status.
 */
public record HealthSummaryResponse(
        String status,
        Instant generatedAt,
        long totalAccounts,
        long totalFunds,
        long totalOrders,
        Map<String, Long> ordersByStatus
) {
}
