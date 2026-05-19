package com.clearfund.service;

import com.clearfund.dto.AuditEventResponse;
import com.clearfund.dto.CreateOrderRequest;
import com.clearfund.dto.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse placeOrder(CreateOrderRequest request);

    OrderResponse getOrder(String orderRef);

    List<AuditEventResponse> getAuditTrail(String orderRef);
}
