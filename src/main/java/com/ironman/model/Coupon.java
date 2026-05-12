package com.ironman.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "coupons")
public class Coupon {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 64)
  private String code;

  @Column(columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "discount_type", nullable = false, columnDefinition = "discount_type")
  private DiscountType discountType;

  @Column(name = "discount_value", nullable = false)
  private BigDecimal discountValue;

  @Column(name = "min_order_amount", nullable = false)
  private BigDecimal minOrderAmount = BigDecimal.ZERO;

  @Column(name = "max_discount_amount")
  private BigDecimal maxDiscountAmount;

  @Column(name = "valid_from")
  private Instant validFrom;

  @Column(name = "valid_to")
  private Instant validTo;

  @Column(name = "max_uses")
  private Integer maxUses;

  @Column(name = "current_uses", nullable = false)
  private int currentUses = 0;

  @Column(name = "max_uses_per_user", nullable = false)
  private int maxUsesPerUser = 1;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();
}
