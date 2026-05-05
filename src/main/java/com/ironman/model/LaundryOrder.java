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
import java.time.LocalDate;
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
@Table(name = "orders")
public class LaundryOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "order_number", nullable = false, unique = true, length = 32)
  private String orderNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id")
  private User customer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pickup_address_id")
  private Address pickupAddress;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "delivery_address_id")
  private Address deliveryAddress;

  @Column(name = "preferred_pickup_date", nullable = false)
  private LocalDate preferredPickupDate;

  @Column(name = "preferred_pickup_time_slot", nullable = false, length = 32)
  private String preferredPickupTimeSlot;

  @Column(name = "preferred_delivery_date", nullable = false)
  private LocalDate preferredDeliveryDate;

  @Column(name = "preferred_delivery_time_slot", nullable = false, length = 32)
  private String preferredDeliveryTimeSlot;

  @Column(name = "special_instructions")
  private String specialInstructions;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(nullable = false, columnDefinition = "order_status")
  private OrderStatus status = OrderStatus.pending;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "payment_method", nullable = false, columnDefinition = "payment_method")
  private PaymentMethod paymentMethod = PaymentMethod.cod;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "payment_status", nullable = false, columnDefinition = "payment_status")
  private PaymentStatus paymentStatus = PaymentStatus.pending;

  @Column(name = "total_amount", nullable = false)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  @Column(name = "paid_amount", nullable = false)
  private BigDecimal paidAmount = BigDecimal.ZERO;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();
}
