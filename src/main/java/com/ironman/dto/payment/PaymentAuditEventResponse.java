package com.ironman.dto.payment;

import com.ironman.model.PaymentAuditEvent;
import com.ironman.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentAuditEventResponse(
    UUID id,
    UUID paymentId,
    UUID orderId,
    String orderNumber,
    UUID actorId,
    String actorName,
    String actorType,
    String action,
    PaymentStatus previousPaymentStatus,
    PaymentStatus newPaymentStatus,
    BigDecimal previousPaidAmount,
    BigDecimal newPaidAmount,
    String notes,
    String metadata,
    Instant createdAt
) {
  public static PaymentAuditEventResponse from(PaymentAuditEvent event) {
    var actor = event.getActor();
    var payment = event.getPayment();
    var order = event.getOrder();
    return new PaymentAuditEventResponse(
        event.getId(),
        payment == null ? null : payment.getId(),
        order.getId(),
        order.getOrderNumber(),
        actor == null ? null : actor.getId(),
        actor == null ? null : actor.getFullName(),
        event.getActorType(),
        event.getAction(),
        event.getPreviousPaymentStatus(),
        event.getNewPaymentStatus(),
        event.getPreviousPaidAmount(),
        event.getNewPaidAmount(),
        event.getNotes(),
        event.getMetadata(),
        event.getCreatedAt()
    );
  }
}
