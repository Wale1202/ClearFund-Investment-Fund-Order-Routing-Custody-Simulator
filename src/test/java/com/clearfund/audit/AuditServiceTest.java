package com.clearfund.audit;

import com.clearfund.entity.AuditEvent;
import com.clearfund.entity.FundOrder;
import com.clearfund.enums.OrderStatus;
import com.clearfund.repository.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditEventRepository auditEventRepository;

    private FundOrder order() {
        return FundOrder.builder().id(100L).orderRef("ORD-1").build();
    }

    @Test
    void recordsTransitionWithBothStatuses() {
        AuditService service = new AuditService(auditEventRepository);

        service.recordTransition(order(), OrderStatus.RECEIVED, OrderStatus.VALIDATED,
                "Business validation passed");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEvent saved = captor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(100L);
        assertThat(saved.getOrderRef()).isEqualTo("ORD-1");
        assertThat(saved.getFromStatus()).isEqualTo("RECEIVED");
        assertThat(saved.getToStatus()).isEqualTo("VALIDATED");
        assertThat(saved.getDetail()).isEqualTo("Business validation passed");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void recordsCreationEventWithNullFromStatus() {
        AuditService service = new AuditService(auditEventRepository);

        service.recordTransition(order(), null, OrderStatus.RECEIVED, "Order received");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEvent saved = captor.getValue();
        assertThat(saved.getFromStatus()).isNull();
        assertThat(saved.getToStatus()).isEqualTo("RECEIVED");
    }
}
