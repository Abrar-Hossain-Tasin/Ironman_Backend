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
@Table(name = "order_issues")
public class OrderIssue {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id")
  private LaundryOrder order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reported_by")
  private User reportedBy;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "issue_type", nullable = false, columnDefinition = "issue_type")
  private IssueType type;

  @Column(nullable = false, columnDefinition = "text")
  private String description;

  @Column(name = "photo_urls", columnDefinition = "text")
  private String photoUrls;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(nullable = false, columnDefinition = "issue_status")
  private IssueStatus status = IssueStatus.open;

  @Column(name = "resolution_notes", columnDefinition = "text")
  private String resolutionNotes;

  @Column(name = "refund_amount")
  private BigDecimal refundAmount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resolved_by")
  private User resolvedBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "resolved_at")
  private Instant resolvedAt;
}
