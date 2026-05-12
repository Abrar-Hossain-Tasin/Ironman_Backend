package com.ironman.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_reviews")
public class OrderReview {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", unique = true)
  private LaundryOrder order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id")
  private User customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "delivery_man_id")
  private User deliveryMan;

  @Column(name = "overall_rating", nullable = false)
  private int overallRating;

  @Column(name = "service_rating")
  private Integer serviceRating;

  @Column(name = "delivery_rating")
  private Integer deliveryRating;

  @Column(columnDefinition = "text")
  private String comment;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
}
