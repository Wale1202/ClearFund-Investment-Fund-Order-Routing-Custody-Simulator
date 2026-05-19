package com.clearfund.integration;

import com.clearfund.dto.CreateOrderRequest;
import com.clearfund.dto.OrderResponse;
import com.clearfund.entity.FundOrder;
import com.clearfund.enums.OrderStatus;
import com.clearfund.enums.OrderType;
import com.clearfund.enums.SettlementStatus;
import com.clearfund.repository.AccountRepository;
import com.clearfund.repository.AuditEventRepository;
import com.clearfund.repository.CashBalanceRepository;
import com.clearfund.repository.FundOrderRepository;
import com.clearfund.repository.FundRepository;
import com.clearfund.repository.HoldingRepository;
import com.clearfund.repository.SettlementInstructionRepository;
import com.clearfund.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test against a real PostgreSQL: Flyway runs V1-V3 (so this also
 * verifies the migrations and that Hibernate {@code validate} accepts the
 * mappings), then a subscription is driven through the full lifecycle and the
 * resulting custody/cash state is asserted.
 *
 * <p>Uses the V2 seed data: account ACC-1002 (ACTIVE, 10000 EUR) buying into
 * fund LU0292096186 (NAV 10.25).</p>
 *
 * <p>{@code disabledWithoutDocker = true} → the class is skipped (not failed)
 * on machines/CI without Docker, so the normal unit-test build stays green.</p>
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class OrderLifecycleIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private OrderService orderService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private FundRepository fundRepository;
    @Autowired private FundOrderRepository fundOrderRepository;
    @Autowired private CashBalanceRepository cashBalanceRepository;
    @Autowired private HoldingRepository holdingRepository;
    @Autowired private SettlementInstructionRepository settlementInstructionRepository;
    @Autowired private AuditEventRepository auditEventRepository;

    @Test
    void subscriptionFlowsFromReceivedToSettledAndUpdatesCustody() {
        Long accountId = accountRepository.findByAccountRef("ACC-1002").orElseThrow().getId();
        Long fundId = fundRepository.findByFundCode("LU0292096186").orElseThrow().getId();

        // 1. Place
        OrderResponse placed = orderService.placeOrder(new CreateOrderRequest(
                "ACC-1002", "LU0292096186", OrderType.SUBSCRIPTION,
                new BigDecimal("2000.00"), null));
        assertThat(placed.status()).isEqualTo(OrderStatus.RECEIVED);

        Long orderId = fundOrderRepository.findByOrderRef(placed.orderRef())
                .map(FundOrder::getId).orElseThrow();

        // 2. Drive the lifecycle
        orderService.validateOrder(orderId);
        orderService.routeOrder(orderId);
        orderService.acceptOrder(orderId);
        OrderResponse settled = orderService.settleOrder(orderId);

        // 3. Order is SETTLED and priced (2000 / 10.25 = 195.121951)
        assertThat(settled.status()).isEqualTo(OrderStatus.SETTLED);
        assertThat(settled.units()).isEqualByComparingTo("195.121951");

        // 4. Cash debited: 10000 - 2000 = 8000
        assertThat(cashBalanceRepository.findByAccountIdAndCurrency(accountId, "EUR")
                .orElseThrow().getAmount()).isEqualByComparingTo("8000.00");

        // 5. Holding created with the bought units
        assertThat(holdingRepository.findByAccountIdAndFundId(accountId, fundId)
                .orElseThrow().getUnits()).isEqualByComparingTo("195.121951");

        // 6. Settlement instruction settled
        assertThat(settlementInstructionRepository.findByOrderId(orderId)
                .orElseThrow().getStatus()).isEqualTo(SettlementStatus.SETTLED);

        // 7. Audit trail recorded, ending in SETTLED
        var trail = auditEventRepository.findByOrderRefOrderByCreatedAtAsc(placed.orderRef());
        assertThat(trail).isNotEmpty();
        assertThat(trail.get(trail.size() - 1).getToStatus()).isEqualTo("SETTLED");
    }
}
