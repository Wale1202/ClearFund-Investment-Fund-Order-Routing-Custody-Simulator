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

import java.time.Instant;

/**
 * Append-only audit record. One row is written for every order state
 * transition. Deliberately holds {@code orderId}/{@code orderRef} by value
 * (not a JPA relationship) so the trail is immutable and decoupled.
 */
@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_event_seq")
    @SequenceGenerator(name = "audit_event_seq", sequenceName = "audit_event_seq", allocationSize = 1)
    @Column(name = "audit_id")
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_ref", nullable = false, length = 24)
    private String orderRef;

    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 20)
    private String toStatus;

    @Column(name = "detail", length = 400)
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
