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
 * Custody position: the number of units an account holds in a fund.
 * One row per (account, fund).
 */
@Entity
@Table(
        name = "holdings",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_holding_account_fund",
                columnNames = {"account_id", "fund_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holding extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "holding_seq")
    @SequenceGenerator(name = "holding_seq", sequenceName = "holding_seq", allocationSize = 1)
    @Column(name = "holding_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fund_id", nullable = false)
    private Fund fund;

    @Column(name = "units", nullable = false, precision = 18, scale = 6)
    private BigDecimal units;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
