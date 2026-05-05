package com.ironman.dto.payment;

import com.ironman.model.PaymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRecordRequest(
    @NotNull UUID orderId,
    @Positive BigDecimal amount,
    @NotNull PaymentType paymentType,
    String notes
) {}
