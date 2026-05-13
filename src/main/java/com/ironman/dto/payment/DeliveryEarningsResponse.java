package com.ironman.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Cash collected by the calling delivery agent in a date window. Used by the
 * "today's earnings" panel in the delivery portal.
 */
public record DeliveryEarningsResponse(
    LocalDate from,
    LocalDate to,
    BigDecimal totalCollected,
    long transactionCount,
    List<PaymentResponse> payments
) {}
