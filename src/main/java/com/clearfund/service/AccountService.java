package com.clearfund.service;

import com.clearfund.dto.CashBalanceResponse;
import com.clearfund.dto.HoldingResponse;

import java.util.List;

public interface AccountService {

    List<HoldingResponse> getHoldings(Long accountId);

    List<CashBalanceResponse> getCashBalances(Long accountId);
}
