package com.clearfund.service;

import com.clearfund.audit.AuditService;
import com.clearfund.config.SettlementProperties;
import com.clearfund.dto.CreateOrderRequest;
import com.clearfund.dto.OrderResponse;
import com.clearfund.entity.Account;
import com.clearfund.entity.Fund;
import com.clearfund.entity.FundOrder;
import com.clearfund.enums.OrderStatus;
import com.clearfund.enums.OrderType;
import com.clearfund.exception.BusinessRuleException;
import com.clearfund.exception.ResourceNotFoundException;
import com.clearfund.mapper.OrderMapper;
import com.clearfund.repository.AccountRepository;
import com.clearfund.repository.AuditEventRepository;
import com.clearfund.repository.FundOrderRepository;
import com.clearfund.repository.FundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private AccountRepository accountRepository;
    @Mock private FundRepository fundRepository;
    @Mock private FundOrderRepository fundOrderRepository;
    @Mock private AuditEventRepository auditEventRepository;
    @Mock private AuditService auditService;

    private final OrderMapper orderMapper = new OrderMapper();
    private final SettlementProperties settlementProperties = new SettlementProperties(2);

    private OrderServiceImpl service() {
        return new OrderServiceImpl(accountRepository, fundRepository, fundOrderRepository,
                auditEventRepository, auditService, orderMapper, settlementProperties);
    }

    private Account activeAccount() {
        return Account.builder().id(1L).accountRef("ACC-1").name("Jane")
                .email("jane@example.com").status("ACTIVE").build();
    }

    private Fund openFund() {
        return Fund.builder().id(10L).fundCode("CFEQ01").fundName("ClearFund Equity")
                .currency("GBP").status("OPEN").navPerUnit(new BigDecimal("12.500000")).build();
    }

    @Test
    void placesSubscriptionOrderInReceivedAndWritesAudit() {
        when(accountRepository.findByAccountRef("ACC-1")).thenReturn(Optional.of(activeAccount()));
        when(fundRepository.findByFundCode("CFEQ01")).thenReturn(Optional.of(openFund()));
        when(fundOrderRepository.save(any(FundOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new CreateOrderRequest("ACC-1", "CFEQ01", OrderType.SUBSCRIPTION,
                new BigDecimal("5000.00"), null);

        OrderResponse response = service().placeOrder(request);

        assertThat(response.status()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(response.orderRef()).startsWith("ORD-");
        assertThat(response.settlementDate()).isEqualTo(response.tradeDate().plusDays(2));
        verify(auditService).recordTransition(any(FundOrder.class), eqNull(), any(), any());
    }

    @Test
    void rejectsOrderForSuspendedAccount() {
        Account suspended = activeAccount();
        suspended.setStatus("SUSPENDED");
        when(accountRepository.findByAccountRef("ACC-1")).thenReturn(Optional.of(suspended));
        when(fundRepository.findByFundCode("CFEQ01")).thenReturn(Optional.of(openFund()));

        var request = new CreateOrderRequest("ACC-1", "CFEQ01", OrderType.SUBSCRIPTION,
                new BigDecimal("5000.00"), null);

        assertThatThrownBy(() -> service().placeOrder(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void throwsWhenFundDoesNotExist() {
        when(accountRepository.findByAccountRef("ACC-1")).thenReturn(Optional.of(activeAccount()));
        when(fundRepository.findByFundCode("NOPE")).thenReturn(Optional.empty());

        var request = new CreateOrderRequest("ACC-1", "NOPE", OrderType.SUBSCRIPTION,
                new BigDecimal("100.00"), null);

        assertThatThrownBy(() -> service().placeOrder(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Fund not found");
    }

    private static OrderStatus eqNull() {
        return org.mockito.ArgumentMatchers.isNull();
    }
}
