package com.clearfund.service;

import com.clearfund.audit.AuditService;
import com.clearfund.config.SettlementProperties;
import com.clearfund.dto.AuditEventResponse;
import com.clearfund.dto.CreateOrderRequest;
import com.clearfund.dto.OrderResponse;
import com.clearfund.entity.Account;
import com.clearfund.entity.Fund;
import com.clearfund.entity.FundOrder;
import com.clearfund.enums.OrderStatus;
import com.clearfund.exception.BusinessRuleException;
import com.clearfund.exception.ResourceNotFoundException;
import com.clearfund.mapper.OrderMapper;
import com.clearfund.repository.AccountRepository;
import com.clearfund.repository.AuditEventRepository;
import com.clearfund.repository.FundOrderRepository;
import com.clearfund.repository.FundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * First-version order service: accepts an order, validates the referenced
 * account and fund, persists it in {@code RECEIVED} and writes the opening
 * audit entry. Validation/routing/execution/settlement transitions are
 * intentionally left for later iterations.
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter REF_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AccountRepository accountRepository;
    private final FundRepository fundRepository;
    private final FundOrderRepository fundOrderRepository;
    private final AuditEventRepository auditEventRepository;
    private final AuditService auditService;
    private final OrderMapper orderMapper;
    private final SettlementProperties settlementProperties;

    public OrderServiceImpl(AccountRepository accountRepository,
                            FundRepository fundRepository,
                            FundOrderRepository fundOrderRepository,
                            AuditEventRepository auditEventRepository,
                            AuditService auditService,
                            OrderMapper orderMapper,
                            SettlementProperties settlementProperties) {
        this.accountRepository = accountRepository;
        this.fundRepository = fundRepository;
        this.fundOrderRepository = fundOrderRepository;
        this.auditEventRepository = auditEventRepository;
        this.auditService = auditService;
        this.orderMapper = orderMapper;
        this.settlementProperties = settlementProperties;
    }

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
    public OrderResponse getOrder(String orderRef) {
        return fundOrderRepository.findByOrderRef(orderRef)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderRef));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEventResponse> getAuditTrail(String orderRef) {
        if (fundOrderRepository.findByOrderRef(orderRef).isEmpty()) {
            throw ResourceNotFoundException.of("Order", orderRef);
        }
        return auditEventRepository.findByOrderRefOrderByCreatedAtAsc(orderRef).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    private String generateOrderRef(LocalDate tradeDate) {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-%s-%s".formatted(tradeDate.format(REF_DATE), suffix);
    }
}
