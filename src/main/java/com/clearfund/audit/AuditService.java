package com.clearfund.audit;

import com.clearfund.entity.AuditEvent;
import com.clearfund.entity.FundOrder;
import com.clearfund.enums.OrderStatus;
import com.clearfund.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Writes the append-only audit trail. Every order state transition should
 * go through {@link #recordTransition} so the trail is complete.
 */
@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public void recordTransition(FundOrder order, OrderStatus from, OrderStatus to, String detail) {
        AuditEvent event = AuditEvent.builder()
                .orderId(order.getId())
                .orderRef(order.getOrderRef())
                .fromStatus(from == null ? null : from.name())
                .toStatus(to.name())
                .detail(detail)
                .createdAt(Instant.now())
                .build();
        auditEventRepository.save(event);
    }
}
