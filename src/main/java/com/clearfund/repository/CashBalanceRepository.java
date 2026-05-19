package com.clearfund.repository;

import com.clearfund.entity.CashBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CashBalanceRepository extends JpaRepository<CashBalance, Long> {

    Optional<CashBalance> findByAccountIdAndCurrency(Long accountId, String currency);

    List<CashBalance> findByAccountId(Long accountId);
}
