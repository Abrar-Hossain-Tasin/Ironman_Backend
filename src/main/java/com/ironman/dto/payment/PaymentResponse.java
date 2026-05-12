package com.ironman.dto.payment;

import com.ironman.model.Payment;
import com.ironman.model.PaymentType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    UUID orderId,
    String orderNumber,
    UUID collectedBy,
    String collectedByName,
    BigDecimal amount,
    PaymentType paymentType,
    Instant collectedAt,
    String paymentReference,
    String payerPhone,
    String notes,
    boolean verified,
    UUID verifiedBy,
    Instant verifiedAt
) {
  public static PaymentResponse from(Payment payment) {
    var collector = payment.getCollectedBy();
    var verifier = payment.getVerifiedBy();
    return new PaymentResponse(
        payment.getId(),
        payment.getOrder().getId(),
        payment.getOrder().getOrderNumber(),
        collector == null ? null : collector.getId(),
        collector == null ? null : collector.getFullName(),
        payment.getAmount(),
        payment.getPaymentType(),
        payment.getCollectedAt(),
        payment.getPaymentReference(),
        payment.getPayerPhone(),
        payment.getNotes(),
        payment.isVerified(),
        verifier == null ? null : verifier.getId(),
        payment.getVerifiedAt()
    );
  }
}
