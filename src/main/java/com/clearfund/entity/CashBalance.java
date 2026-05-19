package com.clearfund.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * An account's spendable cash in a single currency. One row per
 * (account, currency). {@code version} guards against concurrent updates
 * during settlement.
 */
@Entity
@Table(
        name = "cash_balances",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_cash_account_ccy",
                columnNames = {"account_id", "currency"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashBalance extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cash_balance_seq")
    @SequenceGenerator(name = "cash_balance_seq", sequenceName = "cash_balance_seq", allocationSize = 1)
    @Column(name = "cash_balance_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
