package com.clearfund.service;

import com.clearfund.entity.FundOrder;
import com.clearfund.entity.SettlementInstruction;

public interface SettlementService {

    /** Raises a PENDING settlement instruction for an order. */
    SettlementInstruction createInstruction(FundOrder order);

    /**
     * Settles a SETTLEMENT_PENDING order: moves cash and units, marks the
     * instruction and order SETTLED. Returns the (now SETTLED) order.
     */
    FundOrder settle(Long orderId);
}
