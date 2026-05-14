package com.ironman.repository;

import com.ironman.model.PaymentWebhookEvent;
import com.ironman.model.PaymentWebhookStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, UUID> {
  Optional<PaymentWebhookEvent> findByProviderAndIdempotencyKey(String provider, String idempotencyKey);

  Optional<PaymentWebhookEvent> findByProviderAndEventId(String provider, String eventId);

  List<PaymentWebhookEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);

  List<PaymentWebhookEvent> findByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
      PaymentWebhookStatus status,
      Instant nextRetryAt,
      Pageable pageable
  );

  long countByStatus(PaymentWebhookStatus status);
}
