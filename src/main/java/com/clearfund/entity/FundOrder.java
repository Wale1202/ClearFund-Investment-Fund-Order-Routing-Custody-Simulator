package com.clearfund.entity;

import com.clearfund.enums.OrderStatus;
import com.clearfund.enums.OrderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A subscription or redemption order moving through the lifecycle.
 *
 * <p>For a SUBSCRIPTION the investor supplies {@code cashAmount} and
 * {@code units} is computed at execution. For a REDEMPTION the investor
 * supplies {@code units} and {@code cashAmount} is computed. {@code navUsed}
 * is the NAV snapshot taken at execution time.</p>
 *
 * <p>Table named {@code fund_order} because ORDER is a reserved word in
 * Oracle (and SQL in general).</p>
 */
@Entity
@Table(name = "fund_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundOrder extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fund_order_seq")
    @SequenceGenerator(name = "fund_order_seq", sequenceName = "fund_order_seq", allocationSize = 1)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "order_ref", nullable = false, unique = true, length = 24)
    private String orderRef;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fund_id", nullable = false)
    private Fund fund;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 12)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "cash_amount", precision = 18, scale = 2)
    private BigDecimal cashAmount;

    @Column(name = "units", precision = 18, scale = 6)
    private BigDecimal units;

    @Column(name = "nav_used", precision = 18, scale = 6)
    private BigDecimal navUsed;

    @Column(name = "trade_date")
    private LocalDate tradeDate;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    @Column(name = "reject_reason", length = 400)
    private String rejectReason;
}
