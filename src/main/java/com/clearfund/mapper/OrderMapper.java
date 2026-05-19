package com.clearfund.mapper;

import com.clearfund.dto.AuditEventResponse;
import com.clearfund.dto.OrderResponse;
import com.clearfund.entity.AuditEvent;
import com.clearfund.entity.FundOrder;
import org.springframework.stereotype.Component;

/**
 * Hand-written entity → DTO mapping. Kept manual (no MapStruct) in the
 * first version to keep the build simple and the mapping easy to read.
 */
@Component
public class OrderMapper {

    public OrderResponse toResponse(FundOrder order) {
        return new OrderResponse(
                order.getOrderRef(),
                order.getAccount().getAccountRef(),
                order.getFund().getFundCode(),
                order.getOrderType(),
                order.getStatus(),
                order.getCashAmount(),
                order.getUnits(),
                order.getNavUsed(),
                order.getTradeDate(),
                order.getSettlementDate(),
                order.getRejectReason(),
                order.getCreatedAt()
        );
    }

    public AuditEventResponse toResponse(AuditEvent event) {
        return new AuditEventResponse(
                event.getFromStatus(),
                event.getToStatus(),
                event.getDetail(),
                event.getCreatedAt()
        );
    }
}
