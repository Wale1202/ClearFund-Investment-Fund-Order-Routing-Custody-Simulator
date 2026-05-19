package com.clearfund.controller;

import com.clearfund.dto.AuditEventResponse;
import com.clearfund.dto.CancelOrderRequest;
import com.clearfund.dto.CreateOrderRequest;
import com.clearfund.dto.OrderResponse;
import com.clearfund.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Advance RECEIVED -> ... -> SETTLEMENT_PENDING (or REJECTED). */
    @PostMapping("/{orderRef}/process")
    public OrderResponse processOrder(@PathVariable String orderRef) {
        return orderService.processOrder(orderRef);
    }

    /** Settle a SETTLEMENT_PENDING order. */
    @PostMapping("/{orderRef}/settle")
    public OrderResponse settleOrder(@PathVariable String orderRef) {
        return orderService.settleOrder(orderRef);
    }

    @PostMapping("/{orderRef}/cancel")
    public OrderResponse cancelOrder(@PathVariable String orderRef,
                                     @Valid @RequestBody CancelOrderRequest request) {
        return orderService.cancelOrder(orderRef, request.reason());
    }

    @GetMapping("/{orderRef}")
    public OrderResponse getOrder(@PathVariable String orderRef) {
        return orderService.getOrder(orderRef);
    }

    @GetMapping("/{orderRef}/audit")
    public List<AuditEventResponse> getAuditTrail(@PathVariable String orderRef) {
        return orderService.getAuditTrail(orderRef);
    }
}
