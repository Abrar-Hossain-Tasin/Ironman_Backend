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
@Table(name = "order_assignments")
public class OrderAssignment {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id")
  private LaundryOrder order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "assigned_to")
  private User assignedTo;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "assignment_type", nullable = false, columnDefinition = "assignment_type")
  private AssignmentType assignmentType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_by")
  private User assignedBy;

  @Column(name = "assigned_at", nullable = false)
  private Instant assignedAt = Instant.now();

  @Column(name = "accepted_at")
  private Instant acceptedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(nullable = false, columnDefinition = "assignment_status")
  private AssignmentStatus status = AssignmentStatus.pending;

  private String notes;
}
