package com.ironman.dto.user;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight row for the admin customer list. Aggregate counters are computed
 * server-side so the admin UI doesn't pull every order to derive them.
 */
public record CustomerSummary(
    UUID id,
    String fullName,
    String email,
    String phone,
    boolean active,
    boolean emailVerified,
    Instant createdAt,
    long orderCount,
    BigDecimal totalSpent,
    BigDecimal totalPaid,
    long openIssues,
    Instant lastOrderAt
) {}
