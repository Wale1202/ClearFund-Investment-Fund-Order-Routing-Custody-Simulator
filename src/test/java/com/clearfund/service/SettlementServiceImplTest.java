package com.clearfund.service;

import com.clearfund.audit.AuditService;
import com.clearfund.entity.Account;
import com.clearfund.entity.CashBalance;
import com.clearfund.entity.Fund;
import com.clearfund.entity.FundOrder;
import com.clearfund.entity.Holding;
import com.clearfund.entity.SettlementInstruction;
import com.clearfund.enums.OrderStatus;
import com.clearfund.enums.OrderType;
import com.clearfund.enums.SettlementStatus;
import com.clearfund.exception.InvalidOrderStateException;
import com.clearfund.exception.SettlementException;
import com.clearfund.repository.CashBalanceRepository;
import com.clearfund.repository.FundOrderRepository;
import com.clearfund.repository.HoldingRepository;
import com.clearfund.repository.SettlementInstructionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceImplTest {

    @Mock private FundOrderRepository fundOrderRepository;
    @Mock private CashBalanceRepository cashBalanceRepository;
    @Mock private HoldingRepository holdingRepository;
    @Mock private SettlementInstructionRepository settlementInstructionRepository;
    @Mock private AuditService auditService;

    private SettlementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SettlementServiceImpl(fundOrderRepository, cashBalanceRepository,
                holdingRepository, settlementInstructionRepository, auditService);
    }

    // -------- fixtures -------- //

    private Account account() {
        return Account.builder().id(1L).accountRef("ACC-1").name("Jane")
                .email("jane@example.com").status("ACTIVE").build();
    }

    private Fund fund() {
        return Fund.builder().id(10L).fundCode("CFEQ01").fundName("ClearFund Equity")
                .currency("EUR").status("OPEN").navPerUnit(new BigDecimal("12.500000")).build();
    }

    private FundOrder order(OrderType type) {
        return FundOrder.builder().id(100L).orderRef("ORD-1").account(account()).fund(fund())
                .orderType(type).status(OrderStatus.SETTLEMENT_PENDING)
                .cashAmount(new BigDecimal("5000.00")).units(new BigDecimal("400.000000"))
                .navUsed(new BigDecimal("12.500000")).tradeDate(LocalDate.of(2026, 5, 15))
                .settlementDate(LocalDate.of(2026, 5, 17)).build();
    }

    private SettlementInstruction pendingInstruction() {
        return SettlementInstruction.builder().id(7L).orderId(100L).instructionRef("SI-1")
                .status(SettlementStatus.PENDING).settlementDate(LocalDate.of(2026, 5, 17))
                .amount(new BigDecimal("5000.00")).currency("EUR").build();
    }

    private void orderSaveReturnsArg() {
        when(fundOrderRepository.save(any(FundOrder.class))).thenAnswer(i -> i.getArgument(0));
    }

    // -------- successful subscription -------- //

    @Test
    void settle_subscription_debitsCashIncreasesHoldingAndSettles() {
        FundOrder order = order(OrderType.SUBSCRIPTION);
        SettlementInstruction instruction = pendingInstruction();
        CashBalance cash = CashBalance.builder().amount(new BigDecimal("10000.00")).build();

        when(fundOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(settlementInstructionRepository.findByOrderId(100L)).thenReturn(Optional.of(instruction));
        when(cashBalanceRepository.findByAccountIdAndCurrency(1L, "EUR")).thenReturn(Optional.of(cash));
        when(holdingRepository.findByAccountIdAndFundId(1L, 10L)).thenReturn(Optional.empty());
        orderSaveReturnsArg();

        FundOrder result = service.settle(100L);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.SETTLED);
        assertThat(cash.getAmount()).isEqualByComparingTo("5000.00");          // 10000 - 5000
        assertThat(instruction.getStatus()).isEqualTo(SettlementStatus.SETTLED);
        verify(holdingRepository).save(any(Holding.class));                     // +400 units
        // settlement started + settlement completed
        verify(auditService, times(2)).recordTransition(any(), any(), any(), any());
        verify(auditService).recordTransition(any(), eq(OrderStatus.SETTLEMENT_PENDING),
                eq(OrderStatus.SETTLED), any());
    }

    // -------- successful redemption -------- //

    @Test
    void settle_redemption_reducesHoldingIncreasesCashAndSettles() {
        FundOrder order = order(OrderType.REDEMPTION);
        order.setUnits(new BigDecimal("50.000000"));
        order.setCashAmount(new BigDecimal("625.00"));
        SettlementInstruction instruction = pendingInstruction();
        Holding holding = Holding.builder().units(new BigDecimal("100.000000")).build();
        CashBalance cash = CashBalance.builder().amount(new BigDecimal("1000.00")).build();

        when(fundOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(settlementInstructionRepository.findByOrderId(100L)).thenReturn(Optional.of(instruction));
        when(holdingRepository.findByAccountIdAndFundId(1L, 10L)).thenReturn(Optional.of(holding));
        when(cashBalanceRepository.findByAccountIdAndCurrency(1L, "EUR")).thenReturn(Optional.of(cash));
        orderSaveReturnsArg();

        FundOrder result = service.settle(100L);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.SETTLED);
        assertThat(holding.getUnits()).isEqualByComparingTo("50.000000");   // 100 - 50
        assertThat(cash.getAmount()).isEqualByComparingTo("1625.00");       // 1000 + 625
        assertThat(instruction.getStatus()).isEqualTo(SettlementStatus.SETTLED);
    }

    // -------- failure: insufficient cash -------- //

    @Test
    void settle_subscription_insufficientCash_throwsAndChangesNothing() {
        FundOrder order = order(OrderType.SUBSCRIPTION);
        CashBalance cash = CashBalance.builder().amount(new BigDecimal("100.00")).build();

        when(fundOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(settlementInstructionRepository.findByOrderId(100L))
                .thenReturn(Optional.of(pendingInstruction()));
        when(cashBalanceRepository.findByAccountIdAndCurrency(1L, "EUR")).thenReturn(Optional.of(cash));

        assertThatThrownBy(() -> service.settle(100L))
                .isInstanceOf(SettlementException.class)
                .hasMessageContaining("Insufficient cash");

        assertThat(cash.getAmount()).isEqualByComparingTo("100.00");        // untouched
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SETTLEMENT_PENDING);
        verify(cashBalanceRepository, never()).save(any());
        verify(fundOrderRepository, never()).save(any());
    }

    // -------- failure: insufficient holdings -------- //

    @Test
    void settle_redemption_insufficientHoldings_throwsAndChangesNothing() {
        FundOrder order = order(OrderType.REDEMPTION);
        order.setUnits(new BigDecimal("50.000000"));
        Holding holding = Holding.builder().units(new BigDecimal("10.000000")).build();

        when(fundOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(settlementInstructionRepository.findByOrderId(100L))
                .thenReturn(Optional.of(pendingInstruction()));
        when(holdingRepository.findByAccountIdAndFundId(1L, 10L)).thenReturn(Optional.of(holding));

        assertThatThrownBy(() -> service.settle(100L))
                .isInstanceOf(SettlementException.class)
                .hasMessageContaining("Insufficient holdings");

        assertThat(holding.getUnits()).isEqualByComparingTo("10.000000");   // untouched
        verify(holdingRepository, never()).save(any());
        verify(fundOrderRepository, never()).save(any());
    }

    // -------- guard: wrong state -------- //

    @Test
    void settle_whenOrderNotSettlementPending_throwsInvalidOrderState() {
        FundOrder order = order(OrderType.SUBSCRIPTION);
        order.setStatus(OrderStatus.RECEIVED);
        when(fundOrderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.settle(100L))
                .isInstanceOf(InvalidOrderStateException.class);
    }
}
