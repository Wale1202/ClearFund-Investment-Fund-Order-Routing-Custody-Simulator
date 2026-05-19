package com.clearfund.service;

import com.clearfund.dto.AuditEventResponse;
import com.clearfund.dto.CreateOrderRequest;
import com.clearfund.dto.OrderResponse;
import com.clearfund.dto.PagedResponse;
import com.clearfund.enums.OrderStatus;
import com.clearfund.enums.OrderType;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Each lifecycle method performs exactly one step of the state machine so
 * the transitions can be driven (and observed) individually via the API.
 */
public interface OrderService {

    OrderResponse placeOrder(CreateOrderRequest request);

    PagedResponse<OrderResponse> listOrders(OrderStatus status, OrderType type, Pageable pageable);

    OrderResponse getOrder(Long id);

    /** RECEIVED -> VALIDATED, or RECEIVED -> REJECTED if a rule fails. */
    OrderResponse validateOrder(Long id);

    /** VALIDATED -> ROUTED. */
    OrderResponse routeOrder(Long id);

    /** ROUTED -> ACCEPTED -> SETTLEMENT_PENDING (prices the order at NAV). */
    OrderResponse acceptOrder(Long id);

    /** SETTLEMENT_PENDING -> SETTLED (cash and units move here). */
    OrderResponse settleOrder(Long id);

    /** Any non-terminal state -> CANCELLED. */
    OrderResponse cancelOrder(Long id, String reason);

    List<AuditEventResponse> getAuditEvents(Long id);
}
