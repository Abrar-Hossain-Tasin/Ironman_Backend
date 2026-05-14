package com.ironman.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentWebhookSettlementRequest(
    @NotNull UUID orderId,
    @NotNull @Positive BigDecimal amount,
    @NotBlank String transactionId,
    String eventId,
    String payerPhone,
    String currency
) {
}
