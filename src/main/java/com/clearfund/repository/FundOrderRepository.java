package com.clearfund.repository;

import com.clearfund.entity.FundOrder;
import com.clearfund.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FundOrderRepository extends JpaRepository<FundOrder, Long> {

    Optional<FundOrder> findByOrderRef(String orderRef);

    List<FundOrder> findByAccountId(Long accountId);

    /** Used by the settlement engine to pick up due, executed orders. */
    List<FundOrder> findByStatusAndSettlementDateLessThanEqual(OrderStatus status, LocalDate date);
}
