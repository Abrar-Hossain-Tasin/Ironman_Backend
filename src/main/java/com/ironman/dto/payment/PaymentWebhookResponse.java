package com.ironman.dto.payment;

import com.ironman.model.PaymentWebhookEvent;
import com.ironman.model.PaymentWebhookStatus;
import java.time.Instant;
import java.util.UUID;

public record PaymentWebhookResponse(
    UUID eventLogId,
    String provider,
    PaymentWebhookStatus status,
    String eventId,
    String idempotencyKey,
    UUID orderId,
    UUID paymentId,
    String message,
    int attemptCount,
    Instant nextRetryAt
) {
  public static PaymentWebhookResponse from(PaymentWebhookEvent event, String message) {
    var order = event.getOrder();
    var payment = event.getPayment();
    return new PaymentWebhookResponse(
        event.getId(),
        event.getProvider(),
        event.getStatus(),
        event.getEventId(),
        event.getIdempotencyKey(),
        order == null ? null : order.getId(),
        payment == null ? null : payment.getId(),
        message,
        event.getAttemptCount(),
        event.getNextRetryAt()
    );
  }
}
