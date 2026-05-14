package com.ironman.dto.payment;

import java.math.BigDecimal;

public record PaymentProviderSummary(
    String provider,
    BigDecimal total,
    BigDecimal verifiedTotal,
    long paymentCount,
    long unverifiedCount,
    long unappliedCount
) {
}
