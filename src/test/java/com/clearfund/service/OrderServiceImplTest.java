package com.clearfund.service;

import com.clearfund.audit.AuditService;
import com.clearfund.config.SettlementProperties;
import com.clearfund.dto.CreateOrderRequest;
import com.clearfund.dto.OrderResponse;
import com.clearfund.entity.Account;
import com.clearfund.entity.CashBalance;
import com.clearfund.entity.Fund;
import com.clearfund.entity.FundOrder;
import com.clearfund.entity.Holding;
import com.clearfund.enums.OrderStatus;
import com.clearfund.enums.OrderType;
import com.clearfund.exception.InvalidOrderStateException;
import com.clearfund.mapper.OrderMapper;
import com.clearfund.repository.AccountRepository;
import com.clearfund.repository.AuditEventRepository;
import com.clearfund.repository.CashBalanceRepository;
import com.clearfund.repository.FundOrderRepository;
import com.clearfund.repository.FundRepository;
import com.clearfund.repository.HoldingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class OrderServiceImplTest {

    @Mock private AccountRepository accountRepository;
    @Mock private FundRepository fundRepository;
    @Mock private FundOrderRepository fundOrderRepository;
    @Mock private AuditEventRepository auditEventRepository;
    @Mock private CashBalanceRepository cashBalanceRepository;
    @Mock private HoldingRepository holdingRepository;
    @Mock private AuditService auditService;

    private final OrderMapper orderMapper = new OrderMapper();
    private final SettlementProperties settlementProperties = new SettlementProperties(2);

    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(accountRepository, fundRepository, fundOrderRepository,
                auditEventRepository, cashBalanceRepository, holdingRepository,
                auditService, orderMapper, settlementProperties);
    }

    // -------- fixtures -------- //

    private Account account() {
        return Account.builder().id(1L).accountRef("ACC-1").name("Jane")
                .email("jane@example.com").status("ACTIVE").build();
    }

    private Fund fund() {
        return Fund.builder().id(10L).fundCode("CFEQ01").fundName("ClearFund Equity")
                .currency("GBP").status("OPEN").navPerUnit(new BigDecimal("12.500000")).build();
    }

    private FundOrder subscription(OrderStatus status, BigDecimal cash) {
        return FundOrder.builder().id(100L).orderRef("ORD-1").account(account()).fund(fund())
                .orderType(OrderType.SUBSCRIPTION).status(status).cashAmount(cash).build();
    }

    private FundOrder redemption(OrderStatus status, BigDecimal units) {
        return FundOrder.builder().id(101L).orderRef("ORD-2").account(account()).fund(fund())
                .orderType(OrderType.REDEMPTION).status(status).units(units).build();
    }

    private void saveReturnsArg() {
        when(fundOrderRepository.save(any(FundOrder.class))).thenAnswer(i -> i.getArgument(0));
    }

    // -------- placeOrder -------- //

    @Test
    void placeOrder_persistsReceivedAndWritesAudit() {
        when(accountRepository.findByAccountRef("ACC-1")).thenReturn(Optional.of(account()));
        when(fundRepository.findByFundCode("CFEQ01")).thenReturn(Optional.of(fund()));
        saveReturnsArg();

        var request = new CreateOrderRequest("ACC-1", "CFEQ01", OrderType.SUBSCRIPTION,
                new BigDecimal("5000.00"), null);

        OrderResponse response = service.placeOrder(request);

        assertThat(response.status()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(response.settlementDate()).isEqualTo(response.tradeDate().plusDays(2));
        verify(auditService).recordTransition(any(), eq(null), eq(OrderStatus.RECEIVED), any());
    }

    // -------- processOrder happy path -------- //

    @Test
    void processOrder_subscriptionWithEnoughCash_reachesSettlementPendingAndPricesUnits() {
        FundOrder order = subscription(OrderStatus.RECEIVED, new BigDecimal("5000.00"));
        when(fundOrderRepository.findByOrderRef("ORD-1")).thenReturn(Optional.of(order));
        when(cashBalanceRepository.findByAccountIdAndCurrency(1L, "GBP"))
                .thenReturn(Optional.of(CashBalance.builder().amount(new BigDecimal("10000.00")).build()));
        saveReturnsArg();

        OrderResponse response = service.processOrder("ORD-1");

        assertThat(response.status()).isEqualTo(OrderStatus.SETTLEMENT_PENDING);
        assertThat(response.navUsed()).isEqualByComparingTo("12.500000");
        // 5000 / 12.5 = 400 units
        assertThat(response.units()).isEqualByComparingTo("400.000000");
        // RECEIVED->VALIDATED->ROUTED->ACCEPTED->SETTLEMENT_PENDING
        verify(auditService, times(4)).recordTransition(any(), any(), any(), any());
    }

    // -------- processOrder rejection paths -------- //

    @Test
    void processOrder_subscriptionWithoutEnoughCash_isRejectedWithReason() {
        FundOrder order = subscription(OrderStatus.RECEIVED, new BigDecimal("5000.00"));
        when(fundOrderRepository.findByOrderRef("ORD-1")).thenReturn(Optional.of(order));
        when(cashBalanceRepository.findByAccountIdAndCurrency(1L, "GBP"))
                .thenReturn(Optional.of(CashBalance.builder().amount(new BigDecimal("100.00")).build()));
        saveReturnsArg();

        OrderResponse response = service.processOrder("ORD-1");

        assertThat(response.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(response.rejectReason()).contains("Insufficient cash");
        verify(auditService, times(1))
                .recordTransition(any(), eq(OrderStatus.RECEIVED), eq(OrderStatus.REJECTED), any());
    }

    @Test
    void processOrder_redemptionWithoutEnoughHoldings_isRejectedWithReason() {
        FundOrder order = redemption(OrderStatus.RECEIVED, new BigDecimal("50.000000"));
        when(fundOrderRepository.findByOrderRef("ORD-2")).thenReturn(Optional.of(order));
        when(holdingRepository.findByAccountIdAndFundId(1L, 10L))
                .thenReturn(Optional.of(Holding.builder().units(new BigDecimal("10.000000")).build()));
        saveReturnsArg();

        OrderResponse response = service.processOrder("ORD-2");

        assertThat(response.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(response.rejectReason()).contains("Insufficient holdings");
    }

    @Test
    void processOrder_whenNotInReceived_throwsInvalidOrderState() {
        FundOrder order = subscription(OrderStatus.SETTLED, new BigDecimal("5000.00"));
        when(fundOrderRepository.findByOrderRef("ORD-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.processOrder("ORD-1"))
                .isInstanceOf(InvalidOrderStateException.class);
        verify(auditService, never()).recordTransition(any(), any(), any(), any());
    }

    // -------- settleOrder -------- //

    @Test
    void settleOrder_subscription_debitsCashIncreasesHoldingAndSettles() {
        FundOrder order = subscription(OrderStatus.SETTLEMENT_PENDING, new BigDecimal("5000.00"));
        order.setUnits(new BigDecimal("400.000000"));
        order.setNavUsed(new BigDecimal("12.500000"));
        CashBalance cash = CashBalance.builder().amount(new BigDecimal("10000.00")).build();

        when(fundOrderRepository.findByOrderRef("ORD-1")).thenReturn(Optional.of(order));
        when(cashBalanceRepository.findByAccountIdAndCurrency(1L, "GBP")).thenReturn(Optional.of(cash));
        when(holdingRepository.findByAccountIdAndFundId(1L, 10L)).thenReturn(Optional.empty());
        saveReturnsArg();

        OrderResponse response = service.settleOrder("ORD-1");

        assertThat(response.status()).isEqualTo(OrderStatus.SETTLED);
        assertThat(cash.getAmount()).isEqualByComparingTo("5000.00");
        verify(holdingRepository).save(any(Holding.class));
        verify(cashBalanceRepository).save(cash);
    }

    // -------- cancelOrder -------- //

    @Test
    void cancelOrder_nonTerminal_setsCancelledAndAudits() {
        FundOrder order = subscription(OrderStatus.ROUTED, new BigDecimal("5000.00"));
        when(fundOrderRepository.findByOrderRef("ORD-1")).thenReturn(Optional.of(order));
        saveReturnsArg();

        OrderResponse response = service.cancelOrder("ORD-1", "client changed mind");

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.rejectReason()).isEqualTo("client changed mind");
        verify(auditService)
                .recordTransition(any(), eq(OrderStatus.ROUTED), eq(OrderStatus.CANCELLED), any());
    }

    @Test
    void cancelOrder_terminalOrder_throwsInvalidOrderState() {
        FundOrder order = subscription(OrderStatus.SETTLED, new BigDecimal("5000.00"));
        when(fundOrderRepository.findByOrderRef("ORD-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelOrder("ORD-1", "too late"))
                .isInstanceOf(InvalidOrderStateException.class);
    }
}
