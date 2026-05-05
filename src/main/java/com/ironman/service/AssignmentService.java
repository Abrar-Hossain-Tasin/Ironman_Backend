package com.ironman.service;

import com.ironman.config.BadRequestException;
import com.ironman.config.NotFoundException;
import com.ironman.dto.order.AssignmentActionRequest;
import com.ironman.dto.order.AssignmentResponse;
import com.ironman.model.AssignmentStatus;
import com.ironman.model.AssignmentType;
import com.ironman.model.OrderAssignment;
import com.ironman.model.OrderStatus;
import com.ironman.model.User;
import com.ironman.repository.OrderAssignmentRepository;
import java.time.Instant;
import java.util.List;
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
