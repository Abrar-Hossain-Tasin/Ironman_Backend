package com.ironman.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "service_pricing")
public class ServicePricing {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "service_category_id")
  private ServiceCategory serviceCategory;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "clothing_type_id")
  private ClothingType clothingType;

  @Column(nullable = false)
  private BigDecimal price;

  @Column(nullable = false, length = 3)
  private String currency = "BDT";

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom = LocalDate.now();

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "set_by_admin_id")
  private User setByAdmin;

  @Column(name = "is_current", nullable = false)
  private boolean current = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();
}
