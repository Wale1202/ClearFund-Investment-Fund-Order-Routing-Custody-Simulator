package com.clearfund.service;

import com.clearfund.dto.CashBalanceResponse;
import com.clearfund.dto.HoldingResponse;
import com.clearfund.exception.ResourceNotFoundException;
import com.clearfund.mapper.AccountMapper;
import com.clearfund.repository.AccountRepository;
import com.clearfund.repository.CashBalanceRepository;
import com.clearfund.repository.HoldingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final HoldingRepository holdingRepository;
    private final CashBalanceRepository cashBalanceRepository;
    private final AccountMapper accountMapper;

    public AccountServiceImpl(AccountRepository accountRepository,
                              HoldingRepository holdingRepository,
                              CashBalanceRepository cashBalanceRepository,
                              AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.holdingRepository = holdingRepository;
        this.cashBalanceRepository = cashBalanceRepository;
        this.accountMapper = accountMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HoldingResponse> getHoldings(Long accountId) {
        requireAccount(accountId);
        return holdingRepository.findByAccountId(accountId).stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashBalanceResponse> getCashBalances(Long accountId) {
        requireAccount(accountId);
        return cashBalanceRepository.findByAccountId(accountId).stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    private void requireAccount(Long accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw ResourceNotFoundException.of("Account", String.valueOf(accountId));
        }
    }
}
