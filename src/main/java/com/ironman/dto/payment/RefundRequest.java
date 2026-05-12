package com.ironman.dto.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record RefundRequest(
    @NotNull @Positive BigDecimal amount,
    String reason,
    String transactionReference
) {}
