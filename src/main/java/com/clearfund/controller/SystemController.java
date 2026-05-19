package com.clearfund.controller;

import com.clearfund.dto.HealthSummaryResponse;
import com.clearfund.service.SystemService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operational summary for the dashboard / production-support view.
 *
 * <pre>
 * GET /api/system/health-summary
 * 200 OK
 * { "status": "UP", "generatedAt": "2026-05-19T10:15:30Z",
 *   "totalAccounts": 3, "totalFunds": 2, "totalOrders": 42,
 *   "ordersByStatus": { "RECEIVED": 1, "VALIDATED": 0, "ROUTED": 0,
 *     "ACCEPTED": 0, "SETTLEMENT_PENDING": 4, "SETTLED": 35,
 *     "REJECTED": 1, "CANCELLED": 1 } }
 * </pre>
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    @GetMapping("/health-summary")
    public HealthSummaryResponse healthSummary() {
        return systemService.healthSummary();
    }
}
