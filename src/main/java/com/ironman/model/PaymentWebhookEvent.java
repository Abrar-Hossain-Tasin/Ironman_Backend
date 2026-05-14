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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
@Table(name = "payment_webhook_events")
public class PaymentWebhookEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 32)
  private String provider;

  @Column(name = "event_id", length = 160)
  private String eventId;

  @Column(name = "idempotency_key", nullable = false, length = 200)
  private String idempotencyKey;

  @Column(name = "payload_sha256", nullable = false, length = 64)
  private String payloadSha256;

  @Column(name = "signature_header", columnDefinition = "text")
  private String signatureHeader;

  @Column(name = "request_headers", columnDefinition = "text")
  private String requestHeaders;

  @Column(name = "raw_payload", nullable = false, columnDefinition = "text")
  private String rawPayload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PaymentWebhookStatus status = PaymentWebhookStatus.received;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "payment_id")
  private Payment payment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  private LaundryOrder order;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(name = "last_error", columnDefinition = "text")
  private String lastError;

  @Column(name = "next_retry_at")
  private Instant nextRetryAt;

  @Column(name = "processed_at")
  private Instant processedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @PrePersist
  void prePersist() {
    Instant now = Instant.now();
    createdAt = createdAt == null ? now : createdAt;
    updatedAt = now;
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = Instant.now();
  }
}
