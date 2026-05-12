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
@Table(name = "refunds")
public class Refund {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id")
  private LaundryOrder order;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(columnDefinition = "text")
  private String reason;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(nullable = false, columnDefinition = "refund_status")
  private RefundStatus status = RefundStatus.pending;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "original_method", columnDefinition = "payment_method")
  private PaymentMethod originalMethod;

  @Column(name = "transaction_reference")
  private String transactionReference;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requested_by")
  private User requestedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processed_by")
  private User processedBy;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt = Instant.now();

  @Column(name = "processed_at")
  private Instant processedAt;
}
