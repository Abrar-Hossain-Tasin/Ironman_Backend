package com.ironman.model;

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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment_audit_events")
public class PaymentAuditEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "payment_id")
  private Payment payment;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id")
  private LaundryOrder order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_id")
  private User actor;

  @Column(name = "actor_type", nullable = false, length = 32)
  private String actorType;

  @Column(nullable = false, length = 80)
  private String action;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "previous_payment_status", columnDefinition = "payment_status")
  private PaymentStatus previousPaymentStatus;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "new_payment_status", columnDefinition = "payment_status")
  private PaymentStatus newPaymentStatus;

  @Column(name = "previous_paid_amount")
  private BigDecimal previousPaidAmount;

  @Column(name = "new_paid_amount")
  private BigDecimal newPaidAmount;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(columnDefinition = "text")
  private String metadata;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
}
