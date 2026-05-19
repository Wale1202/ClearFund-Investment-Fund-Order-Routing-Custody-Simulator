package com.clearfund.controller;

import com.clearfund.dto.AuditEventResponse;
import com.clearfund.dto.CancelOrderRequest;
import com.clearfund.dto.CreateOrderRequest;
import com.clearfund.dto.OrderResponse;
import com.clearfund.dto.PagedResponse;
import com.clearfund.enums.OrderStatus;
import com.clearfund.enums.OrderType;
import com.clearfund.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Order lifecycle API.
 *
 * <p>Place an order:</p>
 * <pre>
 * POST /api/orders
 * { "accountRef": "ACC-1", "fundCode": "CFEQ01",
 *   "orderType": "SUBSCRIPTION", "cashAmount": 5000.00 }
 *
 * 201 Created
 * { "orderRef": "ORD-20260519-1A2B3C4D", "accountRef": "ACC-1",
 *   "fundCode": "CFEQ01", "orderType": "SUBSCRIPTION", "status": "RECEIVED",
 *   "cashAmount": 5000.00, "units": null, "navUsed": null,
 *   "tradeDate": "2026-05-19", "settlementDate": "2026-05-21",
 *   "rejectReason": null, "createdAt": "2026-05-19T10:15:30Z" }
 * </pre>
 *
 * <p>Invalid body → 422 with a consistent error envelope:</p>
 * <pre>
 * { "timestamp": "...", "status": 422, "error": "VALIDATION_FAILED",
 *   "messages": ["cashAmount must be greater than 0"], "path": "/api/orders" }
 * </pre>
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(request));
    }

    /**
     * Paginated, filterable order list.
     *
     * <pre>
     * GET /api/orders?status=SETTLED&type=SUBSCRIPTION&page=0&size=20&sort=id,desc
     *
     * 200 OK
     * { "content": [ ...OrderResponse... ], "page": 0, "size": 20,
     *   "totalElements": 42, "totalPages": 3, "first": true, "last": false }
     * </pre>
     */
    @GetMapping
    public PagedResponse<OrderResponse> listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) OrderType type,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return orderService.listOrders(status, type, pageable);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

    @PostMapping("/{id}/validate")
    public OrderResponse validate(@PathVariable Long id) {
        return orderService.validateOrder(id);
    }

    @PostMapping("/{id}/route")
    public OrderResponse route(@PathVariable Long id) {
        return orderService.routeOrder(id);
    }

    @PostMapping("/{id}/accept")
    public OrderResponse accept(@PathVariable Long id) {
        return orderService.acceptOrder(id);
    }

    @PostMapping("/{id}/settle")
    public OrderResponse settle(@PathVariable Long id) {
        return orderService.settleOrder(id);
    }

    /**
     * <pre>
     * POST /api/orders/42/cancel
     * { "reason": "client changed mind" }
     * 200 OK -> OrderResponse with status "CANCELLED"
     * </pre>
     */
    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id,
                                @Valid @RequestBody CancelOrderRequest request) {
        return orderService.cancelOrder(id, request.reason());
    }

    @GetMapping("/{id}/audit-events")
    public List<AuditEventResponse> auditEvents(@PathVariable Long id) {
        return orderService.getAuditEvents(id);
    }
}
