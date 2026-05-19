package com.clearfund.repository;

import com.clearfund.entity.FundOrder;
import com.clearfund.enums.OrderStatus;
import com.clearfund.enums.OrderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FundOrderRepository extends JpaRepository<FundOrder, Long> {

    Optional<FundOrder> findByOrderRef(String orderRef);

    /** Used by the settlement engine to pick up due orders. */
    List<FundOrder> findByStatusAndSettlementDateLessThanEqual(OrderStatus status, LocalDate date);

    /**
     * Paginated order search. Both filters are optional: a null parameter
     * means "do not filter on this field".
     */
    @Query("""
            select o from FundOrder o
            where (:status is null or o.status = :status)
              and (:type   is null or o.orderType = :type)
            """)
    Page<FundOrder> search(@Param("status") OrderStatus status,
                           @Param("type") OrderType type,
                           Pageable pageable);

    /** [status, count] rows for the health summary. */
    @Query("select o.status, count(o) from FundOrder o group by o.status")
    List<Object[]> countGroupedByStatus();
}
