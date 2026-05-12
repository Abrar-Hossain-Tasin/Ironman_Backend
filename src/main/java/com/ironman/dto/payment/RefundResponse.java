package com.ironman.dto.payment;

import com.ironman.model.PaymentMethod;
import com.ironman.model.Refund;
import com.ironman.model.RefundStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RefundResponse(
    UUID id,
    UUID orderId,
    BigDecimal amount,
    String reason,
    RefundStatus status,
    PaymentMethod originalMethod,
    String transactionReference,
    UUID requestedBy,
    UUID processedBy,
    Instant requestedAt,
    Instant processedAt
) {
  public static RefundResponse from(Refund refund) {
    return new RefundResponse(
        refund.getId(),
        refund.getOrder().getId(),
        refund.getAmount(),
        refund.getReason(),
        refund.getStatus(),
        refund.getOriginalMethod(),
        refund.getTransactionReference(),
        refund.getRequestedBy() == null ? null : refund.getRequestedBy().getId(),
        refund.getProcessedBy() == null ? null : refund.getProcessedBy().getId(),
        refund.getRequestedAt(),
        refund.getProcessedAt()
    );
  }
}
