package com.clearfund.service;

import com.clearfund.audit.AuditService;
import com.clearfund.entity.Account;
import com.clearfund.entity.CashBalance;
import com.clearfund.entity.Fund;
import com.clearfund.entity.FundOrder;
import com.clearfund.entity.Holding;
import com.clearfund.entity.SettlementInstruction;
import com.clearfund.enums.OrderStatus;
import com.clearfund.enums.SettlementStatus;
import com.clearfund.exception.InvalidOrderStateException;
import com.clearfund.exception.ResourceNotFoundException;
import com.clearfund.exception.SettlementException;
import com.clearfund.repository.CashBalanceRepository;
import com.clearfund.repository.FundOrderRepository;
import com.clearfund.repository.HoldingRepository;
import com.clearfund.repository.SettlementInstructionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Settlement simulation.
 *
 * <p>The whole of {@link #settle} runs in a single {@code @Transactional}
 * boundary: the cash move, the holding move, the instruction update, the
 * order transition and the audit rows either all commit or all roll back. We
 * additionally validate sufficiency <em>before</em> mutating anything, so an
 * insufficient-funds/holdings failure throws cleanly with zero side effects
 * (the transaction rollback is then just belt-and-braces for an unexpected
 * infrastructure error part-way through the writes).</p>
 */
@Service
public class SettlementServiceImpl implements SettlementService {

    private static final DateTimeFormatter REF_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final FundOrderRepository fundOrderRepository;
    private final CashBalanceRepository cashBalanceRepository;
    private final HoldingRepository holdingRepository;
    private final SettlementInstructionRepository settlementInstructionRepository;
    private final AuditService auditService;

    public SettlementServiceImpl(FundOrderRepository fundOrderRepository,
                                 CashBalanceRepository cashBalanceRepository,
                                 HoldingRepository holdingRepository,
                                 SettlementInstructionRepository settlementInstructionRepository,
                                 AuditService auditService) {
        this.fundOrderRepository = fundOrderRepository;
        this.cashBalanceRepository = cashBalanceRepository;
        this.holdingRepository = holdingRepository;
        this.settlementInstructionRepository = settlementInstructionRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public SettlementInstruction createInstruction(FundOrder order) {
        SettlementInstruction instruction = SettlementInstruction.builder()
                .orderId(order.getId())
                .instructionRef(generateRef(order.getTradeDate()))
                .status(SettlementStatus.PENDING)
                .settlementDate(order.getSettlementDate())
                .amount(order.getCashAmount())
                .currency(order.getFund().getCurrency())
                .build();
        return settlementInstructionRepository.save(instruction);
    }

    @Override
    @Transactional
    public FundOrder settle(Long orderId) {
        FundOrder order = fundOrderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", String.valueOf(orderId)));
        if (order.getStatus() != OrderStatus.SETTLEMENT_PENDING) {
            throw new InvalidOrderStateException(order.getOrderRef(),
                    order.getStatus(), OrderStatus.SETTLED);
        }

        SettlementInstruction instruction = settlementInstructionRepository
                .findByOrderId(orderId)
                .orElseGet(() -> createInstruction(order));

        auditService.recordTransition(order, OrderStatus.SETTLEMENT_PENDING,
                OrderStatus.SETTLEMENT_PENDING,
                "Settlement started; instruction " + instruction.getInstructionRef());

        Account account = order.getAccount();
        Fund fund = order.getFund();

        switch (order.getOrderType()) {
            case SUBSCRIPTION -> {
                CashBalance cash = requireCash(account, fund.getCurrency());
                if (cash.getAmount().compareTo(order.getCashAmount()) < 0) {
                    throw new SettlementException(
                            "Insufficient cash to settle %s: required %s %s, available %s"
                                    .formatted(order.getOrderRef(), order.getCashAmount(),
                                            fund.getCurrency(), cash.getAmount()));
                }
                cash.setAmount(cash.getAmount().subtract(order.getCashAmount()));
                cashBalanceRepository.save(cash);

                Holding holding = holdingOrNew(account, fund);
                holding.setUnits(holding.getUnits().add(order.getUnits()));
                holdingRepository.save(holding);
            }
            case REDEMPTION -> {
                Holding holding = requireHolding(account, fund);
                if (holding.getUnits().compareTo(order.getUnits()) < 0) {
                    throw new SettlementException(
                            "Insufficient holdings to settle %s: required %s units, available %s"
                                    .formatted(order.getOrderRef(), order.getUnits(),
                                            holding.getUnits()));
                }
                holding.setUnits(holding.getUnits().subtract(order.getUnits()));
                holdingRepository.save(holding);

                CashBalance cash = cashOrNew(account, fund.getCurrency());
                cash.setAmount(cash.getAmount().add(order.getCashAmount()));
                cashBalanceRepository.save(cash);
            }
        }

        instruction.setStatus(SettlementStatus.SETTLED);
        settlementInstructionRepository.save(instruction);

        order.setStatus(OrderStatus.SETTLED);
        auditService.recordTransition(order, OrderStatus.SETTLEMENT_PENDING,
                OrderStatus.SETTLED,
                "Settlement completed; instruction " + instruction.getInstructionRef());
        return fundOrderRepository.save(order);
    }

    // ------------------------------------------------------------------ //

    private CashBalance requireCash(Account account, String currency) {
        return cashBalanceRepository.findByAccountIdAndCurrency(account.getId(), currency)
                .orElseThrow(() -> new SettlementException(
                        "No %s cash balance for account %s"
                                .formatted(currency, account.getAccountRef())));
    }

    private CashBalance cashOrNew(Account account, String currency) {
        return cashBalanceRepository.findByAccountIdAndCurrency(account.getId(), currency)
                .orElseGet(() -> CashBalance.builder()
                        .account(account).currency(currency).amount(BigDecimal.ZERO).build());
    }

    private Holding requireHolding(Account account, Fund fund) {
        return holdingRepository.findByAccountIdAndFundId(account.getId(), fund.getId())
                .orElseThrow(() -> new SettlementException(
                        "No holding in %s for account %s"
                                .formatted(fund.getFundCode(), account.getAccountRef())));
    }

    private Holding holdingOrNew(Account account, Fund fund) {
        return holdingRepository.findByAccountIdAndFundId(account.getId(), fund.getId())
                .orElseGet(() -> Holding.builder()
                        .account(account).fund(fund).units(BigDecimal.ZERO).build());
    }

    private String generateRef(LocalDate tradeDate) {
        LocalDate base = tradeDate != null ? tradeDate : LocalDate.now();
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "SI-%s-%s".formatted(base.format(REF_DATE), suffix);
    }
}
