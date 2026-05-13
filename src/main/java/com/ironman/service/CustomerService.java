package com.ironman.service;

import com.ironman.config.NotFoundException;
import com.ironman.dto.order.IssueResponse;
import com.ironman.dto.order.OrderItemResponse;
import com.ironman.dto.order.OrderResponse;
import com.ironman.dto.payment.PaymentResponse;
import com.ironman.dto.payment.RefundResponse;
import com.ironman.dto.user.AddressResponse;
import com.ironman.dto.user.CustomerDetailResponse;
import com.ironman.dto.user.CustomerListResponse;
import com.ironman.dto.user.CustomerSummary;
import com.ironman.model.LaundryOrder;
import com.ironman.model.User;
import com.ironman.model.UserRole;
import com.ironman.repository.AddressRepository;
import com.ironman.repository.LaundryOrderRepository;
import com.ironman.repository.OrderIssueRepository;
import com.ironman.repository.OrderItemRepository;
import com.ironman.repository.PaymentRepository;
import com.ironman.repository.RefundRepository;
import com.ironman.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-facing customer queries — list page with aggregates and a one-shot
 * detail envelope (orders + payments + refunds + issues + addresses). Keeps
 * the admin UI from issuing one extra request per row.
 */
@Service
@RequiredArgsConstructor
public class CustomerService {
  private final UserRepository userRepository;
  private final LaundryOrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final PaymentRepository paymentRepository;
  private final RefundRepository refundRepository;
  private final OrderIssueRepository issueRepository;
  private final AddressRepository addressRepository;

  @Transactional(readOnly = true)
  public CustomerListResponse list(String query, int page, int size) {
    int safePage = Math.max(0, page);
    int safeSize = Math.min(100, Math.max(1, size));
    var users = userRepository.searchCustomers(
        query == null ? "" : query.trim(),
        PageRequest.of(safePage, safeSize));

    List<UUID> ids = users.getContent().stream().map(User::getId).toList();
    Map<UUID, long[]> issueCounts = ids.isEmpty()
        ? Map.of()
        : issueRepository.openIssueCountsByReporters(ids).stream()
            .collect(java.util.stream.Collectors.toMap(
                row -> (UUID) row[0],
                row -> new long[] { ((Number) row[1]).longValue() },
                (a, b) -> a));

    Map<UUID, Object[]> orderAggs = new HashMap<>();
    if (!ids.isEmpty()) {
      for (Object[] row : orderRepository.aggregatesByCustomerIds(ids)) {
        orderAggs.put((UUID) row[0], row);
      }
    }
    Map<UUID, Long> orderCounts = new HashMap<>();
    if (!ids.isEmpty()) {
      for (Object[] row : orderRepository.countByCustomerIds(ids)) {
        orderCounts.put((UUID) row[0], ((Number) row[1]).longValue());
      }
    }

    List<CustomerSummary> summaries = users.getContent().stream()
        .map(user -> {
          Object[] agg = orderAggs.get(user.getId());
          BigDecimal totalSpent = agg == null ? BigDecimal.ZERO : (BigDecimal) agg[1];
          BigDecimal totalPaid = agg == null ? BigDecimal.ZERO : (BigDecimal) agg[2];
          Instant lastOrderAt = agg == null ? null : (Instant) agg[3];
          long count = orderCounts.getOrDefault(user.getId(), 0L);
          long open = issueCounts.getOrDefault(user.getId(), new long[] { 0L })[0];
          return new CustomerSummary(
              user.getId(),
              user.getFullName(),
              user.getEmail(),
              user.getPhone(),
              user.isActive(),
              user.isEmailVerified(),
              user.getCreatedAt(),
              count,
              totalSpent,
              totalPaid,
              open,
              lastOrderAt);
        })
        .toList();

    return new CustomerListResponse(
        summaries,
        users.getNumber(),
        users.getSize(),
        users.getTotalElements(),
        users.getTotalPages());
  }

  @Transactional(readOnly = true)
  public CustomerDetailResponse detail(UUID id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Customer not found"));
    if (user.getRole() != UserRole.customer) {
      throw new NotFoundException("Customer not found");
    }

    List<AddressResponse> addresses = addressRepository
        .findByUserIdOrderByDefaultAddressDescCreatedAtDesc(user.getId())
        .stream()
        .map(AddressResponse::from)
        .toList();

    List<LaundryOrder> orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(user.getId());
    List<OrderResponse> orderResponses = orders.stream()
        .map(order -> OrderResponse.from(order,
            orderItemRepository.findByOrderId(order.getId()).stream()
                .map(OrderItemResponse::from)
                .toList()))
        .toList();

    List<UUID> orderIds = orders.stream().map(LaundryOrder::getId).toList();
    List<PaymentResponse> payments = orderIds.isEmpty()
        ? List.of()
        : paymentRepository.findByOrderIdInOrderByCollectedAtDesc(orderIds).stream()
            .map(PaymentResponse::from)
            .toList();
    List<RefundResponse> refunds = orderIds.isEmpty()
        ? List.of()
        : refundRepository.findByOrderIdInOrderByRequestedAtDesc(orderIds).stream()
            .map(RefundResponse::from)
            .toList();
    List<IssueResponse> issues = issueRepository
        .findByReportedByIdOrderByCreatedAtDesc(user.getId())
        .stream()
        .map(IssueResponse::from)
        .toList();

    BigDecimal totalSpent = orderRepository.sumTotalByCustomer(user.getId());
    BigDecimal totalPaid = orderRepository.sumPaidByCustomer(user.getId());
    BigDecimal totalRefunded = refunds.stream()
        .filter(r -> r.status() == com.ironman.model.RefundStatus.processed)
        .map(RefundResponse::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    long openIssues = issues.stream()
        .filter(i -> i.status() == com.ironman.model.IssueStatus.open
            || i.status() == com.ironman.model.IssueStatus.in_review)
        .count();

    return new CustomerDetailResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        user.getPhone(),
        user.isActive(),
        user.isEmailVerified(),
        user.getCreatedAt(),
        addresses,
        orders.size(),
        totalSpent,
        totalPaid,
        totalRefunded,
        openIssues,
        orderResponses,
        payments,
        refunds,
        issues);
  }
}
