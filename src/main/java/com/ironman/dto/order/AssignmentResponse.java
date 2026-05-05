package com.ironman.dto.order;

import com.ironman.dto.user.UserSummary;
import com.ironman.model.AssignmentStatus;
import com.ironman.model.AssignmentType;
import com.ironman.model.OrderAssignment;
import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(
    UUID id,
    UUID orderId,
    String orderNumber,
    UserSummary assignedTo,
    AssignmentType assignmentType,
    AssignmentStatus status,
    Instant assignedAt,
    Instant acceptedAt,
    Instant completedAt,
    String notes
) {
  public static AssignmentResponse from(OrderAssignment assignment) {
    return new AssignmentResponse(
        assignment.getId(),
        assignment.getOrder().getId(),
        assignment.getOrder().getOrderNumber(),
        UserSummary.from(assignment.getAssignedTo()),
        assignment.getAssignmentType(),
        assignment.getStatus(),
        assignment.getAssignedAt(),
        assignment.getAcceptedAt(),
        assignment.getCompletedAt(),
        assignment.getNotes()
    );
  }
}
