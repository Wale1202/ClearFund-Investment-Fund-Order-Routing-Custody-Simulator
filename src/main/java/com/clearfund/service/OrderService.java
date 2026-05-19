package com.clearfund.service;

import com.clearfund.dto.AuditEventResponse;
import com.clearfund.dto.CreateOrderRequest;
import com.clearfund.dto.OrderResponse;

import java.util.List;

public interface OrderService {

    /** Creates the order in {@code RECEIVED}. Account/fund must exist. */
    OrderResponse placeOrder(CreateOrderRequest request);

    /**
     * Drives the order forward through VALIDATED -> ROUTED -> ACCEPTED ->
     * SETTLEMENT_PENDING. If a business rule fails the order ends in
     * REJECTED with a stored reason (not an exception).
     */
    OrderResponse processOrder(String orderRef);

    /** Settles a SETTLEMENT_PENDING order: moves cash and units, then SETTLED. */
    OrderResponse settleOrder(String orderRef);

    /** Cancels a non-terminal order and records the reason. */
    OrderResponse cancelOrder(String orderRef, String reason);

    OrderResponse getOrder(String orderRef);

    List<AuditEventResponse> getAuditTrail(String orderRef);
}
