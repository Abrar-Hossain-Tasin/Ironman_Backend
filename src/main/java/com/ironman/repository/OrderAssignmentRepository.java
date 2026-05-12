package com.ironman.repository;

import com.ironman.model.AssignmentStatus;
import com.ironman.model.AssignmentType;
import com.ironman.model.OrderAssignment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderAssignmentRepository extends JpaRepository<OrderAssignment, UUID> {
  List<OrderAssignment> findByAssignedToIdOrderByAssignedAtDesc(UUID userId);

  List<OrderAssignment> findByStatusOrderByAssignedAtDesc(AssignmentStatus status);

  List<OrderAssignment> findAllByOrderByAssignedAtDesc();

  boolean existsByOrderIdAndAssignedToIdAndAssignmentTypeInAndStatusIn(
      UUID orderId,
      UUID assignedToId,
      Collection<AssignmentType> assignmentTypes,
      Collection<AssignmentStatus> statuses
  );

  Optional<OrderAssignment> findFirstByOrderIdAndAssignmentTypeOrderByAssignedAtDesc(
      UUID orderId,
      AssignmentType assignmentType
  );
}
