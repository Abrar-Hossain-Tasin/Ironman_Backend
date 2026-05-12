package com.ironman.dto.order;

import com.ironman.dto.user.UserSummary;
import com.ironman.model.Address;
import com.ironman.model.AssignmentStatus;
import com.ironman.model.AssignmentType;
import com.ironman.model.OrderAssignment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(
    UUID id,
    UUID orderId,
    String orderNumber,
    UserSummary assignedTo,
    String customerName,
    String address,
    AssignmentType assignmentType,
    AssignmentStatus status,
    Instant assignedAt,
    Instant acceptedAt,
    Instant completedAt,
    String preferredTime,
    BigDecimal amountDue,
    String notes
) {
  public static AssignmentResponse from(OrderAssignment assignment) {
    var order = assignment.getOrder();
    boolean customerFacing = assignment.getAssignmentType() == AssignmentType.pickup
        || assignment.getAssignmentType() == AssignmentType.delivery;
    Address targetAddress = assignment.getAssignmentType() == AssignmentType.pickup
        ? order.getPickupAddress()
        : assignment.getAssignmentType() == AssignmentType.delivery
            ? order.getDeliveryAddress()
            : null;
    BigDecimal amountDue = order.getTotalAmount().subtract(order.getPaidAmount());

    return new AssignmentResponse(
        assignment.getId(),
        order.getId(),
        order.getOrderNumber(),
        UserSummary.from(assignment.getAssignedTo()),
        order.getCustomer().getFullName(),
        targetAddress == null ? "Processing center" : formatAddress(targetAddress),
        assignment.getAssignmentType(),
        assignment.getStatus(),
        assignment.getAssignedAt(),
        assignment.getAcceptedAt(),
        assignment.getCompletedAt(),
        preferredTime(assignment),
        customerFacing && amountDue.signum() > 0 ? amountDue : null,
        assignment.getNotes()
    );
  }

  private static String preferredTime(OrderAssignment assignment) {
    var order = assignment.getOrder();
    if (assignment.getAssignmentType() == AssignmentType.pickup) {
      return order.getPreferredPickupDate() + " " + order.getPreferredPickupTimeSlot();
    }
    if (assignment.getAssignmentType() == AssignmentType.delivery) {
      return order.getPreferredDeliveryDate() + " " + order.getPreferredDeliveryTimeSlot();
    }
    return null;
  }

  private static String formatAddress(Address address) {
    return java.util.stream.Stream.of(
            address.getAddressLine1(),
            address.getAddressLine2(),
            address.getArea(),
            address.getCity(),
            address.getPostalCode()
        )
        .filter(part -> part != null && !part.isBlank())
        .reduce((left, right) -> left + ", " + right)
        .orElse("Address unavailable");
  }
}
