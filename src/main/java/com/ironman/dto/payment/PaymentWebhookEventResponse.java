package com.ironman.dto.payment;

import com.ironman.model.PaymentWebhookEvent;
import com.ironman.model.PaymentWebhookStatus;
import java.time.Instant;
import java.util.UUID;

public record PaymentWebhookEventResponse(
    UUID id,
    String provider,
    String eventId,
    String idempotencyKey,
    String payloadSha256,
    PaymentWebhookStatus status,
    UUID orderId,
    String orderNumber,
    UUID paymentId,
    int attemptCount,
    String lastError,
    Instant nextRetryAt,
    Instant processedAt,
    Instant createdAt,
    Instant updatedAt
) {
  public static PaymentWebhookEventResponse from(PaymentWebhookEvent event) {
    var order = event.getOrder();
    var payment = event.getPayment();
    return new PaymentWebhookEventResponse(
        event.getId(),
        event.getProvider(),
        event.getEventId(),
        event.getIdempotencyKey(),
        event.getPayloadSha256(),
        event.getStatus(),
        order == null ? null : order.getId(),
        order == null ? null : order.getOrderNumber(),
        payment == null ? null : payment.getId(),
        event.getAttemptCount(),
        event.getLastError(),
        event.getNextRetryAt(),
        event.getProcessedAt(),
        event.getCreatedAt(),
        event.getUpdatedAt()
    );
  }
}
