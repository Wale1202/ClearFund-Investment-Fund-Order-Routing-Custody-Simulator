package com.clearfund.mapper;

import com.clearfund.dto.CashBalanceResponse;
import com.clearfund.dto.HoldingResponse;
import com.clearfund.entity.CashBalance;
import com.clearfund.entity.Holding;
import org.springframework.stereotype.Component;

/** Entity → DTO mapping for an account's custody and cash views. */
@Component
public class AccountMapper {

    public HoldingResponse toResponse(Holding holding) {
        return new HoldingResponse(
                holding.getAccount().getAccountRef(),
                holding.getFund().getFundCode(),
                holding.getFund().getFundName(),
                holding.getUnits()
        );
    }

    public CashBalanceResponse toResponse(CashBalance cashBalance) {
        return new CashBalanceResponse(
                cashBalance.getAccount().getAccountRef(),
                cashBalance.getCurrency(),
                cashBalance.getAmount()
        );
    }
}
