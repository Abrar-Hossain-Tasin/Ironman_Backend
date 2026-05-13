package com.ironman.repository;

import com.ironman.model.IssueStatus;
import com.ironman.model.OrderIssue;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderIssueRepository extends JpaRepository<OrderIssue, UUID> {
  List<OrderIssue> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

  List<OrderIssue> findAllByOrderByCreatedAtDesc();

  List<OrderIssue> findByStatusOrderByCreatedAtDesc(IssueStatus status);

  List<OrderIssue> findByReportedByIdOrderByCreatedAtDesc(UUID reportedById);

  /** Bulk: how many open/in_review issues does each of these customers have? */
  @Query(
      "select i.reportedBy.id, count(i) from OrderIssue i "
          + "where i.reportedBy.id in ?1 "
          + "  and i.status in (com.ironman.model.IssueStatus.open, com.ironman.model.IssueStatus.in_review) "
          + "group by i.reportedBy.id")
  List<Object[]> openIssueCountsByReporters(Collection<UUID> reporterIds);
}
