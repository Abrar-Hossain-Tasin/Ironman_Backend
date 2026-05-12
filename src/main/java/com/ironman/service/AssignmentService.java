package com.ironman.service;

import com.ironman.config.BadRequestException;
import com.ironman.config.NotFoundException;
import com.ironman.dto.order.AssignmentActionRequest;
import com.ironman.dto.order.AssignmentResponse;
import com.ironman.dto.order.PickupReconcileRequest;
import com.ironman.model.AssignmentStatus;
import com.ironman.model.AssignmentType;
import com.ironman.model.LaundryOrder;
import com.ironman.model.OrderAssignment;
import com.ironman.model.OrderItem;
import com.ironman.model.OrderStatus;
import com.ironman.model.User;
import com.ironman.repository.LaundryOrderRepository;
import com.ironman.repository.OrderAssignmentRepository;
import com.ironman.repository.OrderItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssignmentService {
  private final PrincipalService principalService;
  private final OrderAssignmentRepository assignmentRepository;
  private final OrderService orderService;
  private final OrderItemRepository orderItemRepository;
  private final LaundryOrderRepository orderRepository;
  private final NotificationService notificationService;

  @Transactional(readOnly = true)
  public List<AssignmentResponse> mine() {
    return assignmentRepository.findByAssignedToIdOrderByAssignedAtDesc(principalService.currentUser().getId())
        .stream()
        .map(AssignmentResponse::from)
        .toList();
  }

  @Transactional
  public AssignmentResponse accept(UUID id) {
    OrderAssignment assignment = myAssignment(id);
    if (assignment.getStatus() != AssignmentStatus.pending) {
      throw new BadRequestException("Only pending assignments can be accepted");
    }
    assignment.setStatus(AssignmentStatus.accepted);
    assignment.setAcceptedAt(Instant.now());
    return AssignmentResponse.from(assignmentRepository.save(assignment));
  }

  @Transactional
  public AssignmentResponse start(UUID id) {
    User actor = principalService.currentUser();
    OrderAssignment assignment = myAssignment(id);
    if (assignment.getStatus() != AssignmentStatus.accepted && assignment.getStatus() != AssignmentStatus.pending) {
      throw new BadRequestException("Assignment cannot be started");
    }
    assignment.setStatus(AssignmentStatus.in_progress);
    assignment = assignmentRepository.save(assignment);
    OrderStatus status = startStatus(assignment.getAssignmentType());
    if (assignment.getOrder().getStatus() != status) {
      orderService.updateStatus(assignment.getOrder(), status, actor.getFullName() + " started " + assignment.getAssignmentType().name(), actor);
    }
    return AssignmentResponse.from(assignment);
  }

  @Transactional
  public AssignmentResponse complete(UUID id, AssignmentActionRequest request) {
    User actor = principalService.currentUser();
    OrderAssignment assignment = myAssignment(id);
    if (assignment.getStatus() != AssignmentStatus.in_progress && assignment.getStatus() != AssignmentStatus.accepted) {
      throw new BadRequestException("Assignment cannot be completed");
    }
    assignment.setStatus(AssignmentStatus.completed);
    assignment.setCompletedAt(Instant.now());
    if (request != null && request.notes() != null) {
      assignment.setNotes(request.notes());
    }
    assignment = assignmentRepository.save(assignment);
    OrderStatus status = completionStatus(assignment.getAssignmentType());
    orderService.updateStatus(assignment.getOrder(), status, actor.getFullName() + " completed " + assignment.getAssignmentType().name(), actor);
    return AssignmentResponse.from(assignment);
  }

  @Transactional
  public AssignmentResponse reconcilePickup(UUID assignmentId, PickupReconcileRequest request) {
    User actor = principalService.currentUser();
    OrderAssignment assignment = myAssignment(assignmentId);
    if (assignment.getAssignmentType() != AssignmentType.pickup) {
      throw new BadRequestException("Reconciliation is only for pickup assignments");
    }

    LaundryOrder order = assignment.getOrder();
    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
    Map<UUID, OrderItem> byId = new HashMap<>();
    for (OrderItem item : items) {
      byId.put(item.getId(), item);
    }

    boolean discrepancy = false;
    BigDecimal newTotal = BigDecimal.ZERO;
    for (PickupReconcileRequest.ItemCount count : request.items()) {
      OrderItem item = byId.get(count.itemId());
      if (item == null) {
        throw new BadRequestException("Item " + count.itemId() + " not on this order");
      }
      item.setActualQuantity(count.actualQuantity());
      BigDecimal subtotal = item.getUnitPrice()
          .multiply(BigDecimal.valueOf(count.actualQuantity()));
      item.setSubtotal(subtotal);
      orderItemRepository.save(item);
      if (count.actualQuantity() != item.getQuantity()) {
        discrepancy = true;
      }
      newTotal = newTotal.add(subtotal);
    }

    BigDecimal discount = order.getDiscountAmount() == null
        ? BigDecimal.ZERO : order.getDiscountAmount();
    order.setTotalAmount(newTotal.subtract(discount).max(BigDecimal.ZERO));
    order.setUpdatedAt(Instant.now());
    orderRepository.save(order);

    if (request.notes() != null && !request.notes().isBlank()) {
      assignment.setNotes(request.notes());
      assignmentRepository.save(assignment);
    }

    if (discrepancy) {
      notificationService.notifyUser(order.getCustomer(),
          "Item count adjusted — " + order.getOrderNumber(),
          "Actual item counts have been recorded at pickup. Your total has been updated.",
          "pickup_reconciled", order.getId());
      notificationService.notifyAdmins(
          "Pickup count adjusted — " + order.getOrderNumber(),
          actor.getFullName() + " recorded a count mismatch during pickup.",
          "pickup_reconciled_admin", order.getId());
    }

    return AssignmentResponse.from(assignment);
  }

  private OrderAssignment myAssignment(UUID id) {
    User user = principalService.currentUser();
    return assignmentRepository.findById(id)
        .filter(assignment -> assignment.getAssignedTo().getId().equals(user.getId()))
        .orElseThrow(() -> new NotFoundException("Assignment not found"));
  }

  private OrderStatus startStatus(AssignmentType type) {
    return switch (type) {
      case pickup -> OrderStatus.pickup_assigned;
      case delivery -> OrderStatus.out_for_delivery;
      case wash -> OrderStatus.in_wash;
      case iron -> OrderStatus.in_iron;
      case dry_clean -> OrderStatus.in_dry_clean;
    };
  }

  private OrderStatus completionStatus(AssignmentType type) {
    return switch (type) {
      case pickup -> OrderStatus.picked_up;
      case delivery -> OrderStatus.delivered;
      case wash -> OrderStatus.wash_complete;
      case iron -> OrderStatus.iron_complete;
      case dry_clean -> OrderStatus.dry_clean_complete;
    };
  }
}
