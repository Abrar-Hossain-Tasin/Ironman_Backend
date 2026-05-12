package com.ironman.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record BkashMerchantPaymentRequest(
    @Positive BigDecimal amount,
    @NotBlank String transactionId,
    String payerPhone,
    String notes
) {}
