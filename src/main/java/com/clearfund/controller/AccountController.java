package com.clearfund.controller;

import com.clearfund.dto.CashBalanceResponse;
import com.clearfund.dto.HoldingResponse;
import com.clearfund.service.AccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Custody and cash views for an account.
 *
 * <pre>
 * GET /api/accounts/1/holdings
 * 200 OK
 * [ { "accountRef": "ACC-1", "fundCode": "CFEQ01",
 *     "fundName": "ClearFund Equity", "units": 400.000000 } ]
 *
 * GET /api/accounts/1/cash-balances
 * 200 OK
 * [ { "accountRef": "ACC-1", "currency": "GBP", "amount": 5000.00 } ]
 *
 * Unknown account -> 404 NOT_FOUND
 * </pre>
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}/holdings")
    public List<HoldingResponse> holdings(@PathVariable Long id) {
        return accountService.getHoldings(id);
    }

    @GetMapping("/{id}/cash-balances")
    public List<CashBalanceResponse> cashBalances(@PathVariable Long id) {
        return accountService.getCashBalances(id);
    }
}
