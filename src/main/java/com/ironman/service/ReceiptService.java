package com.ironman.service;

import com.ironman.config.NotFoundException;
import com.ironman.model.AssignmentStatus;
import com.ironman.model.AssignmentType;
import com.ironman.model.LaundryOrder;
import com.ironman.model.OrderAssignment;
import com.ironman.model.OrderItem;
import com.ironman.model.OrderReceipt;
import com.ironman.model.User;
import com.ironman.model.UserRole;
import com.ironman.repository.LaundryOrderRepository;
import com.ironman.repository.OrderAssignmentRepository;
import com.ironman.repository.OrderItemRepository;
import com.ironman.repository.OrderReceiptRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReceiptService {

  private static final ZoneId DHAKA = ZoneId.of("Asia/Dhaka");
  private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

  private final PrincipalService principalService;
  private final LaundryOrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final OrderReceiptRepository receiptRepository;
  private final OrderAssignmentRepository assignmentRepository;
  private final ReceiptPdfService receiptPdfService;

  public record RenderedReceipt(OrderReceipt receipt, LaundryOrder order, byte[] pdf) {}

  @Transactional
  public RenderedReceipt generate(UUID orderId) {
    User user = principalService.currentUser();
    LaundryOrder order = scopedOrder(orderId, user);

    OrderReceipt receipt = receiptRepository.findByOrderId(order.getId())
        .orElseGet(() -> createReceipt(order, user));

    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
    User deliveryMan = assignmentRepository
        .findFirstByOrderIdAndAssignmentTypeOrderByAssignedAtDesc(
            order.getId(), AssignmentType.delivery)
        .map(OrderAssignment::getAssignedTo)
        .orElse(null);

    byte[] pdf = receiptPdfService.render(order, receipt, items, deliveryMan);
    return new RenderedReceipt(receipt, order, pdf);
  }

  // ── Scoping & creation ────────────────────────────────────────────────────

  private LaundryOrder scopedOrder(UUID orderId, User user) {
    LaundryOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Order not found"));
    boolean allowed = switch (user.getRole()) {
      case admin -> true;
      case customer -> order.getCustomer().getId().equals(user.getId());
      case delivery_man -> assignmentRepository
          .existsByOrderIdAndAssignedToIdAndAssignmentTypeInAndStatusIn(
              order.getId(),
              user.getId(),
              List.of(AssignmentType.delivery),
              List.of(AssignmentStatus.pending, AssignmentStatus.accepted,
                  AssignmentStatus.in_progress, AssignmentStatus.completed));
      default -> false;
    };
    if (!allowed) {
      throw new NotFoundException("Order not found");
    }
    return order;
  }

  private OrderReceipt createReceipt(LaundryOrder order, User generator) {
    OrderReceipt receipt = new OrderReceipt();
    receipt.setOrder(order);
    receipt.setReceiptNumber(nextReceiptNumber());
    receipt.setGeneratedAt(Instant.now());
    receipt.setGeneratedBy(generator);
    return receiptRepository.save(receipt);
  }

  private String nextReceiptNumber() {
    LocalDate today = LocalDate.now(DHAKA);
    Instant start = today.atStartOfDay(DHAKA).toInstant();
    Instant end = today.plusDays(1).atStartOfDay(DHAKA).toInstant();
    long sequence = receiptRepository.countByGeneratedAtBetween(start, end) + 1;
    return "REC-" + today.format(DATE) + "-" + String.format("%04d", sequence);
  }

  // For unit tests / admin re-issue (kept package-private).
  Optional<OrderReceipt> findExisting(UUID orderId) {
    return receiptRepository.findByOrderId(orderId);
  }

  public boolean canEmail(UUID orderId) {
    return scopedOrder(orderId, principalService.currentUser()).getCustomer().getEmail() != null;
  }
}
