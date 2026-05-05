package com.ironman.service;

import com.ironman.config.BadRequestException;
import com.ironman.config.NotFoundException;
import com.ironman.dto.order.AssignmentRequest;
import com.ironman.dto.order.AssignmentResponse;
import com.ironman.dto.user.UserSummary;
import com.ironman.model.AssignmentType;
import com.ironman.model.LaundryOrder;
import com.ironman.model.OrderAssignment;
import com.ironman.model.OrderStatus;
import com.ironman.model.User;
import com.ironman.model.UserRole;
import com.ironman.repository.LaundryOrderRepository;
import com.ironman.repository.OrderAssignmentRepository;
import com.ironman.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {
  private final PrincipalService principalService;
  private final UserRepository userRepository;
  private final LaundryOrderRepository orderRepository;
  private final OrderAssignmentRepository assignmentRepository;
  private final NotificationService notificationService;
  private final OrderService orderService;

  @Transactional
  public AssignmentResponse assign(UUID orderId, AssignmentRequest request) {
    User admin = principalService.currentUser();
    LaundryOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Order not found"));
    User staff = userRepository.findById(request.assignedTo())
        .orElseThrow(() -> new NotFoundException("Staff not found"));
    validateStaffRole(request.assignmentType(), staff.getRole());

    var assignment = new OrderAssignment();
    assignment.setOrder(order);
    assignment.setAssignedTo(staff);
    assignment.setAssignmentType(request.assignmentType());
    assignment.setAssignedBy(admin);
    assignment.setNotes(request.notes());
    assignment = assignmentRepository.save(assignment);

    if (request.assignmentType() == AssignmentType.pickup && order.getStatus() == OrderStatus.confirmed) {
      orderService.updateStatus(order, OrderStatus.pickup_assigned, staff.getFullName() + " is coming to pickup", admin);
    }
    if (request.assignmentType() == AssignmentType.delivery && order.getStatus() == OrderStatus.ready) {
      orderService.updateStatus(order, OrderStatus.delivery_assigned, staff.getFullName() + " will deliver soon", admin);
    }

    notificationService.notifyUser(
        staff,
        "New assignment",
        "You have a " + request.assignmentType().name().replace('_', ' ') + " task for " + order.getOrderNumber(),
        "assignment",
        assignment.getId()
    );
    return AssignmentResponse.from(assignment);
  }

  public List<AssignmentResponse> assignments() {
    return assignmentRepository.findAllByOrderByAssignedAtDesc().stream()
        .map(AssignmentResponse::from)
        .toList();
  }

  public List<UserSummary> staff(UserRole role) {
    var roles = List.of(UserRole.delivery_man, UserRole.wash_man, UserRole.iron_man, UserRole.dry_clean_man);
    return (role == null ? userRepository.findByRoleIn(roles) : userRepository.findByRole(role))
        .stream()
        .map(UserSummary::from)
        .toList();
  }

  private void validateStaffRole(AssignmentType type, UserRole role) {
    boolean valid = switch (type) {
      case pickup, delivery -> role == UserRole.delivery_man;
      case wash -> role == UserRole.wash_man;
      case iron -> role == UserRole.iron_man;
      case dry_clean -> role == UserRole.dry_clean_man;
    };
    if (!valid) {
      throw new BadRequestException("Staff role " + role + " cannot handle assignment type " + type);
    }
  }
}
