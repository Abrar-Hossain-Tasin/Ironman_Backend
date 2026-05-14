package com.ironman.dto.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PaymentReconciliationResponse(
    Instant generatedAt,
    BigDecimal ledgerTotal,
    BigDecimal verifiedTotal,
    BigDecimal unverifiedTotal,
    BigDecimal unappliedTotal,
    long paymentCount,
    long unverifiedCount,
    long unappliedCount,
    long processedWebhookCount,
    long retryScheduledWebhookCount,
    long failedWebhookCount,
    List<PaymentProviderSummary> providerSummaries,
    List<PaymentWebhookEventResponse> recentWebhookEvents,
    List<PaymentAuditEventResponse> recentAuditEvents
) {
}
