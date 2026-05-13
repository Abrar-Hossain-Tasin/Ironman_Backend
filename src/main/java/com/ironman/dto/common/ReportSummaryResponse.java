package com.ironman.dto.common;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Single envelope feeding the admin dashboard. All aggregates are computed
 * server-side over the requested window. Two daily series let the UI draw
 * sparklines without further round-trips.
 */
public record ReportSummaryResponse(
    int windowDays,
    long orderCount,
    long deliveredCount,
    long failedCount,
    Double deliverySuccessPct,
    BigDecimal grossRevenue,
    BigDecimal refundedAmount,
    BigDecimal netRevenue,
    BigDecimal averageOrderValue,
    List<TopService> topServices,
    List<CollectorRow> topCollectors,
    List<DailyBucket> daily
) {
  public record TopService(String name, long quantity) {}
  public record CollectorRow(String name, BigDecimal total, long count) {}
  public record DailyBucket(LocalDate date, long orders, BigDecimal revenue) {}
}
