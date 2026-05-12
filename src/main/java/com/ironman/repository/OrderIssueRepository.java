package com.ironman.repository;

import com.ironman.model.IssueStatus;
import com.ironman.model.OrderIssue;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderIssueRepository extends JpaRepository<OrderIssue, UUID> {
  List<OrderIssue> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

  List<OrderIssue> findAllByOrderByCreatedAtDesc();

  List<OrderIssue> findByStatusOrderByCreatedAtDesc(IssueStatus status);

  List<OrderIssue> findByReportedByIdOrderByCreatedAtDesc(UUID reportedById);
}
