package com.ironman.service;

import com.ironman.model.PaymentWebhookStatus;
import com.ironman.repository.PaymentWebhookEventRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookRetryService {
  private final PaymentWebhookEventRepository webhookEventRepository;
  private final PaymentService paymentService;

  @Scheduled(fixedDelayString = "${app.payments.webhooks.retry-fixed-delay-ms:60000}")
  public void retryDueWebhooks() {
    var dueEvents = webhookEventRepository
        .findByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
            PaymentWebhookStatus.retry_scheduled,
            Instant.now(),
            PageRequest.of(0, 25)
        );
    for (var event : dueEvents) {
      try {
        paymentService.retryWebhookEvent(event.getId());
      } catch (RuntimeException ex) {
        log.warn("Payment webhook retry failed for event {}", event.getId(), ex);
      }
    }
  }
}
