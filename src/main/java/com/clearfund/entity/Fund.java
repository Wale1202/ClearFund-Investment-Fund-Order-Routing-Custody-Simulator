package com.clearfund.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * An investment fund. {@code navPerUnit} is a stored price (seed-loaded for
 * the simulator) and is the only value used in subscription/redemption math.
 */
@Entity
@Table(name = "funds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fund extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fund_seq")
    @SequenceGenerator(name = "fund_seq", sequenceName = "fund_seq", allocationSize = 1)
    @Column(name = "fund_id")
    private Long id;

    @Column(name = "fund_code", nullable = false, unique = true, length = 12)
    private String fundCode;

    @Column(name = "fund_name", nullable = false, length = 160)
    private String fundName;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** OPEN / CLOSED. Kept as a String to keep the first version small. */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "nav_per_unit", nullable = false, precision = 18, scale = 6)
    private BigDecimal navPerUnit;
}
