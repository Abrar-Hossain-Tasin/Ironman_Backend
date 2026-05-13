package com.ironman.dto.user;

import com.ironman.dto.order.IssueResponse;
import com.ironman.dto.order.OrderResponse;
import com.ironman.dto.payment.PaymentResponse;
import com.ironman.dto.payment.RefundResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Single-shot envelope for the admin "view customer" page. Saves the frontend
 * from issuing one /orders/{id}/issues call per order to reconstruct the same
 * picture client-side.
 */
public record CustomerDetailResponse(
    UUID id,
    String fullName,
    String email,
    String phone,
    boolean active,
    boolean emailVerified,
    Instant createdAt,
    List<AddressResponse> addresses,
    long orderCount,
    BigDecimal totalSpent,
    BigDecimal totalPaid,
    BigDecimal totalRefunded,
    long openIssues,
    List<OrderResponse> orders,
    List<PaymentResponse> payments,
    List<RefundResponse> refunds,
    List<IssueResponse> issues
) {}
