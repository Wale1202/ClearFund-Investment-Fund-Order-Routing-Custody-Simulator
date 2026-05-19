package com.clearfund.service;

import com.clearfund.dto.HealthSummaryResponse;
import com.clearfund.enums.OrderStatus;
import com.clearfund.repository.AccountRepository;
import com.clearfund.repository.FundOrderRepository;
import com.clearfund.repository.FundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SystemServiceImpl implements SystemService {

    private final AccountRepository accountRepository;
    private final FundRepository fundRepository;
    private final FundOrderRepository fundOrderRepository;

    public SystemServiceImpl(AccountRepository accountRepository,
                             FundRepository fundRepository,
                             FundOrderRepository fundOrderRepository) {
        this.accountRepository = accountRepository;
        this.fundRepository = fundRepository;
        this.fundOrderRepository = fundOrderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public HealthSummaryResponse healthSummary() {
        // Seed every status with 0 so the breakdown is always complete and
        // ordered, then overlay the actual counts.
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            byStatus.put(status.name(), 0L);
        }
        for (Object[] row : fundOrderRepository.countGroupedByStatus()) {
            byStatus.put(((OrderStatus) row[0]).name(), (Long) row[1]);
        }

        return new HealthSummaryResponse(
                "UP",
                Instant.now(),
                accountRepository.count(),
                fundRepository.count(),
                fundOrderRepository.count(),
                byStatus
        );
    }
}
