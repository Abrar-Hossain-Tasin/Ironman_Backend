package com.ironman.service;

import com.ironman.dto.common.ReportSummaryResponse;
import com.ironman.dto.common.ReportSummaryResponse.CollectorRow;
import com.ironman.dto.common.ReportSummaryResponse.DailyBucket;
import com.ironman.dto.common.ReportSummaryResponse.TopService;
import com.ironman.model.LaundryOrder;
import com.ironman.model.OrderItem;
import com.ironman.model.OrderStatus;
import com.ironman.model.Payment;
import com.ironman.repository.LaundryOrderRepository;
import com.ironman.repository.OrderItemRepository;
import com.ironman.repository.PaymentRepository;
import com.ironman.repository.RefundRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Server-side aggregation behind /admin/reports/summary so the admin dashboard
 * doesn't have to pull every order + payment + refund to the browser to
 * compute its own rollups.
 */
@Service
@RequiredArgsConstructor
public class ReportService {
  private static final ZoneId DHAKA = ZoneId.of("Asia/Dhaka");
  private static final Set<OrderStatus> COMPLETED = Set.of(OrderStatus.delivered);
  private static final Set<OrderStatus> FAILED =
      Set.of(OrderStatus.delivery_failed, OrderStatus.returned, OrderStatus.cancelled);

  private final LaundryOrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final PaymentRepository paymentRepository;
  private final RefundRepository refundRepository;

  @Transactional(readOnly = true)
  public ReportSummaryResponse summary(int windowDays) {
    int days = Math.max(1, Math.min(365, windowDays));
    LocalDate today = LocalDate.now(DHAKA);
    LocalDate startDay = today.minusDays(days - 1L);
    Instant from = startDay.atStartOfDay(DHAKA).toInstant();
    Instant to = today.plusDays(1).atStartOfDay(DHAKA).toInstant();

    List<LaundryOrder> orders = orderRepository.findByCreatedAtGreaterThanEqual(from);
    long orderCount = orders.size();
    long delivered = orders.stream().filter(o -> COMPLETED.contains(o.getStatus())).count();
    long failed = orders.stream().filter(o -> FAILED.contains(o.getStatus())).count();
    long finalized = delivered + failed;
    Double successPct = finalized == 0 ? null : (delivered * 100.0) / finalized;

    BigDecimal grossRevenue = paymentRepository.sumBetween(from, to);
    BigDecimal refunded = refundRepository.sumProcessedBetween(from, to);
    BigDecimal net = grossRevenue.subtract(refunded);
    BigDecimal aov = orderCount == 0
        ? BigDecimal.ZERO
        : orders.stream()
            .map(LaundryOrder::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(orderCount), 0, RoundingMode.HALF_UP);

    // Top services by item quantity over the window.
    Map<String, Long> serviceCounts = new HashMap<>();
    for (LaundryOrder order : orders) {
      for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
        String name = item.getServiceCategory().getName();
        serviceCounts.merge(name, (long) item.getQuantity(), Long::sum);
      }
    }
    List<TopService> topServices = serviceCounts.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(5)
        .map(entry -> new TopService(entry.getKey(), entry.getValue()))
        .toList();

    // Cash collectors leaderboard.
    Map<String, BigDecimal[]> collectorTotals = new HashMap<>();
    Map<String, long[]> collectorCounts = new HashMap<>();
    for (Payment payment : paymentRepository.findAllByOrderByCollectedAtDesc()) {
      if (payment.getCollectedAt() == null || payment.getCollectedAt().isBefore(from)
          || !payment.getCollectedAt().isBefore(to)) continue;
      String name = payment.getCollectedBy() == null ? "Unknown" : payment.getCollectedBy().getFullName();
      collectorTotals.computeIfAbsent(name, k -> new BigDecimal[] { BigDecimal.ZERO })[0] =
          collectorTotals.get(name)[0].add(payment.getAmount());
      collectorCounts.computeIfAbsent(name, k -> new long[] { 0L })[0]++;
    }
    List<CollectorRow> collectors = collectorTotals.entrySet().stream()
        .map(entry -> new CollectorRow(
            entry.getKey(),
            entry.getValue()[0],
            collectorCounts.getOrDefault(entry.getKey(), new long[] { 0L })[0]))
        .sorted(Comparator.comparing(CollectorRow::total).reversed())
        .limit(5)
        .toList();

    // Daily series: one bucket per day in [startDay, today].
    Map<LocalDate, long[]> dailyOrders = new HashMap<>();
    Map<LocalDate, BigDecimal[]> dailyRevenue = new HashMap<>();
    for (LaundryOrder order : orders) {
      LocalDate day = order.getCreatedAt().atZone(DHAKA).toLocalDate();
      dailyOrders.computeIfAbsent(day, k -> new long[] { 0L })[0]++;
    }
    for (Payment payment : paymentRepository.findAllByOrderByCollectedAtDesc()) {
      if (payment.getCollectedAt() == null || payment.getCollectedAt().isBefore(from)
          || !payment.getCollectedAt().isBefore(to)) continue;
      LocalDate day = payment.getCollectedAt().atZone(DHAKA).toLocalDate();
      dailyRevenue.computeIfAbsent(day, k -> new BigDecimal[] { BigDecimal.ZERO })[0] =
          dailyRevenue.get(day)[0].add(payment.getAmount());
    }
    List<DailyBucket> daily = new ArrayList<>();
    for (LocalDate d = startDay; !d.isAfter(today); d = d.plusDays(1)) {
      long count = dailyOrders.getOrDefault(d, new long[] { 0L })[0];
      BigDecimal rev = dailyRevenue.getOrDefault(d, new BigDecimal[] { BigDecimal.ZERO })[0];
      daily.add(new DailyBucket(d, count, rev));
    }

    return new ReportSummaryResponse(
        days, orderCount, delivered, failed, successPct,
        grossRevenue, refunded, net, aov,
        topServices, collectors, daily);
  }
}
