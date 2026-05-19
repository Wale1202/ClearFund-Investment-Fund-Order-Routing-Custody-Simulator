package com.clearfund.service;

import com.clearfund.audit.AuditService;
import com.clearfund.config.SettlementProperties;
import com.clearfund.dto.AuditEventResponse;
import com.clearfund.dto.CreateOrderRequest;
import com.clearfund.dto.OrderResponse;
import com.clearfund.dto.PagedResponse;
import com.clearfund.entity.Account;
import com.clearfund.entity.CashBalance;
import com.clearfund.entity.Fund;
import com.clearfund.entity.FundOrder;
import com.clearfund.entity.Holding;
import com.clearfund.enums.OrderStatus;
import com.clearfund.enums.OrderType;
import com.clearfund.exception.BusinessRuleException;
import com.clearfund.exception.InvalidOrderStateException;
import com.clearfund.exception.ResourceNotFoundException;
import com.clearfund.mapper.OrderMapper;
import com.clearfund.repository.AccountRepository;
import com.clearfund.repository.AuditEventRepository;
import com.clearfund.repository.CashBalanceRepository;
import com.clearfund.repository.FundOrderRepository;
import com.clearfund.repository.FundRepository;
import com.clearfund.repository.HoldingRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Core order lifecycle. Each public lifecycle method performs exactly one
 * step so the state machine can be driven individually via the API:
 *
 * <pre>
 * placeOrder    -> RECEIVED
 * validateOrder -> VALIDATED            (or REJECTED with a reason)
 * routeOrder    -> ROUTED
 * acceptOrder   -> ACCEPTED -> SETTLEMENT_PENDING   (NAV snapshotted here)
 * settleOrder   -> SETTLED              (cash and units actually move here)
 * cancelOrder   -> CANCELLED            (any non-terminal state)
 * </pre>
 *
 * <p>Design note: a missing account/fund throws (we cannot persist an order
 * with no FK), whereas a well-formed order that breaks a business rule —
 * insufficient cash or holdings — ends in REJECTED with a stored reason,
 * because rejection is a normal lifecycle outcome, not an error.</p>
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final int UNIT_SCALE = 6;
    private static final int CASH_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final DateTimeFormatter REF_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AccountRepository accountRepository;
    private final FundRepository fundRepository;
    private final FundOrderRepository fundOrderRepository;
    private final AuditEventRepository auditEventRepository;
    private final CashBalanceRepository cashBalanceRepository;
    private final HoldingRepository holdingRepository;
    private final AuditService auditService;
    private final OrderMapper orderMapper;
    private final SettlementProperties settlementProperties;

    public OrderServiceImpl(AccountRepository accountRepository,
                            FundRepository fundRepository,
                            FundOrderRepository fundOrderRepository,
                            AuditEventRepository auditEventRepository,
                            CashBalanceRepository cashBalanceRepository,
                            HoldingRepository holdingRepository,
                            AuditService auditService,
                            OrderMapper orderMapper,
                            SettlementProperties settlementProperties) {
        this.accountRepository = accountRepository;
        this.fundRepository = fundRepository;
        this.fundOrderRepository = fundOrderRepository;
        this.auditEventRepository = auditEventRepository;
        this.cashBalanceRepository = cashBalanceRepository;
        this.holdingRepository = holdingRepository;
        this.auditService = auditService;
        this.orderMapper = orderMapper;
        this.settlementProperties = settlementProperties;
    }

    // ------------------------------------------------------------------ //
    //  Create / read
    // ------------------------------------------------------------------ //

    @Override
    @Transactional
    public OrderResponse placeOrder(CreateOrderRequest request) {
        Account account = accountRepository.findByAccountRef(request.accountRef())
                .orElseThrow(() -> ResourceNotFoundException.of("Account", request.accountRef()));
        Fund fund = fundRepository.findByFundCode(request.fundCode())
                .orElseThrow(() -> ResourceNotFoundException.of("Fund", request.fundCode()));

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new BusinessRuleException("Account is not active: " + request.accountRef());
        }
        if (!"OPEN".equals(fund.getStatus())) {
            throw new BusinessRuleException("Fund is not open for orders: " + request.fundCode());
        }

        LocalDate tradeDate = LocalDate.now();
        FundOrder order = FundOrder.builder()
                .orderRef(generateOrderRef(tradeDate))
                .account(account)
                .fund(fund)
                .orderType(request.orderType())
                .status(OrderStatus.RECEIVED)
                .cashAmount(request.cashAmount())
                .units(request.units())
                .tradeDate(tradeDate)
                .settlementDate(tradeDate.plusDays(settlementProperties.offsetDays()))
                .build();

        FundOrder saved = fundOrderRepository.save(order);
        auditService.recordTransition(saved, null, OrderStatus.RECEIVED, "Order received");
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> listOrders(OrderStatus status, OrderType type, Pageable pageable) {
        return PagedResponse.from(
                fundOrderRepository.search(status, type, pageable),
                orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        return orderMapper.toResponse(loadOrder(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEventResponse> getAuditEvents(Long id) {
        FundOrder order = loadOrder(id);
        return auditEventRepository.findByOrderRefOrderByCreatedAtAsc(order.getOrderRef()).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    // ------------------------------------------------------------------ //
    //  Lifecycle steps
    // ------------------------------------------------------------------ //

    @Override
    @Transactional
    public OrderResponse validateOrder(Long id) {
        FundOrder order = loadOrder(id);
        requireStatus(order, OrderStatus.RECEIVED);

        String rejectionReason = findRejectionReason(order);
        if (rejectionReason != null) {
            reject(order, rejectionReason);
        } else {
            transition(order, OrderStatus.VALIDATED, "Business validation passed");
        }
        return orderMapper.toResponse(fundOrderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse routeOrder(Long id) {
        FundOrder order = loadOrder(id);
        requireStatus(order, OrderStatus.VALIDATED);
        transition(order, OrderStatus.ROUTED,
                "Routed to fund; settlement date " + order.getSettlementDate());
        return orderMapper.toResponse(fundOrderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse acceptOrder(Long id) {
        FundOrder order = loadOrder(id);
        requireStatus(order, OrderStatus.ROUTED);

        priceOrder(order); // snapshot NAV, compute the derived side
        transition(order, OrderStatus.ACCEPTED, "Accepted at NAV " + order.getNavUsed());
        // No separate endpoint moves the order into the settlement queue,
        // so acceptance places it there directly.
        transition(order, OrderStatus.SETTLEMENT_PENDING,
                "Awaiting settlement on " + order.getSettlementDate());
        return orderMapper.toResponse(fundOrderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse settleOrder(Long id) {
        FundOrder order = loadOrder(id);
        requireStatus(order, OrderStatus.SETTLEMENT_PENDING);

        Account account = order.getAccount();
        Fund fund = order.getFund();

        switch (order.getOrderType()) {
            case SUBSCRIPTION -> {
                // Cash leaves the account; units arrive in custody.
                CashBalance cash = cashBalance(account, fund.getCurrency());
                cash.setAmount(cash.getAmount().subtract(order.getCashAmount()));
                cashBalanceRepository.save(cash);

                Holding holding = holdingOrNew(account, fund);
                holding.setUnits(holding.getUnits().add(order.getUnits()));
                holdingRepository.save(holding);
            }
            case REDEMPTION -> {
                // Units leave custody; cash arrives in the account.
                Holding holding = holdingOrNew(account, fund);
                holding.setUnits(holding.getUnits().subtract(order.getUnits()));
                holdingRepository.save(holding);

                CashBalance cash = cashBalanceOrNew(account, fund.getCurrency());
                cash.setAmount(cash.getAmount().add(order.getCashAmount()));
                cashBalanceRepository.save(cash);
            }
        }

        transition(order, OrderStatus.SETTLED,
                "Settled %s units @ NAV %s".formatted(order.getUnits(), order.getNavUsed()));
        return orderMapper.toResponse(fundOrderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id, String reason) {
        FundOrder order = loadOrder(id);
        if (order.getStatus().isTerminal()) {
            throw new InvalidOrderStateException(order.getOrderRef(),
                    order.getStatus(), OrderStatus.CANCELLED);
        }
        // Reuse rejectReason as the terminal-reason column for cancellations.
        order.setRejectReason(reason);
        transition(order, OrderStatus.CANCELLED, "Cancelled: " + reason);
        return orderMapper.toResponse(fundOrderRepository.save(order));
    }

    // ------------------------------------------------------------------ //
    //  Internals
    // ------------------------------------------------------------------ //

    /** Returns a human-readable reason if the order breaks a business rule, else null. */
    private String findRejectionReason(FundOrder order) {
        Account account = order.getAccount();
        Fund fund = order.getFund();

        return switch (order.getOrderType()) {
            case SUBSCRIPTION -> {
                BigDecimal required = order.getCashAmount();
                BigDecimal available = cashBalanceRepository
                        .findByAccountIdAndCurrency(account.getId(), fund.getCurrency())
                        .map(CashBalance::getAmount)
                        .orElse(BigDecimal.ZERO);
                yield available.compareTo(required) < 0
                        ? "Insufficient cash: required %s %s, available %s"
                                .formatted(required, fund.getCurrency(), available)
                        : null;
            }
            case REDEMPTION -> {
                BigDecimal required = order.getUnits();
                BigDecimal available = holdingRepository
                        .findByAccountIdAndFundId(account.getId(), fund.getId())
                        .map(Holding::getUnits)
                        .orElse(BigDecimal.ZERO);
                yield available.compareTo(required) < 0
                        ? "Insufficient holdings: required %s units, available %s units"
                                .formatted(required, available)
                        : null;
            }
        };
    }

    /**
     * Snapshots the fund NAV onto the order and computes the side the client
     * did not supply: units for a subscription, cash for a redemption.
     */
    private void priceOrder(FundOrder order) {
        BigDecimal nav = order.getFund().getNavPerUnit();
        order.setNavUsed(nav);
        switch (order.getOrderType()) {
            case SUBSCRIPTION ->
                    order.setUnits(order.getCashAmount().divide(nav, UNIT_SCALE, ROUNDING));
            case REDEMPTION ->
                    order.setCashAmount(order.getUnits().multiply(nav).setScale(CASH_SCALE, ROUNDING));
        }
    }

    private void reject(FundOrder order, String reason) {
        order.setRejectReason(reason);
        transition(order, OrderStatus.REJECTED, reason);
    }

    /** The single choke point for status changes: enforces the state machine and audits. */
    private void transition(FundOrder order, OrderStatus target, String detail) {
        OrderStatus from = order.getStatus();
        if (!from.canTransitionTo(target)) {
            throw new InvalidOrderStateException(order.getOrderRef(), from, target);
        }
        order.setStatus(target);
        auditService.recordTransition(order, from, target, detail);
    }

    private void requireStatus(FundOrder order, OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new InvalidOrderStateException(order.getOrderRef(), order.getStatus(), expected);
        }
    }

    private FundOrder loadOrder(Long id) {
        return fundOrderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", String.valueOf(id)));
    }

    private CashBalance cashBalance(Account account, String currency) {
        return cashBalanceRepository.findByAccountIdAndCurrency(account.getId(), currency)
                .orElseThrow(() -> new BusinessRuleException(
                        "No %s cash balance for account %s".formatted(currency, account.getAccountRef())));
    }

    private CashBalance cashBalanceOrNew(Account account, String currency) {
        return cashBalanceRepository.findByAccountIdAndCurrency(account.getId(), currency)
                .orElseGet(() -> CashBalance.builder()
                        .account(account).currency(currency).amount(BigDecimal.ZERO).build());
    }

    private Holding holdingOrNew(Account account, Fund fund) {
        return holdingRepository.findByAccountIdAndFundId(account.getId(), fund.getId())
                .orElseGet(() -> Holding.builder()
                        .account(account).fund(fund).units(BigDecimal.ZERO).build());
    }

    private String generateOrderRef(LocalDate tradeDate) {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-%s-%s".formatted(tradeDate.format(REF_DATE), suffix);
    }
}
