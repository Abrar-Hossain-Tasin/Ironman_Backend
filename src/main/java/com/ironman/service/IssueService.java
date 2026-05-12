package com.ironman.service;

import com.ironman.config.BadRequestException;
import com.ironman.config.NotFoundException;
import com.ironman.dto.order.CreateIssueRequest;
import com.ironman.dto.order.IssueResponse;
import com.ironman.dto.order.ResolveIssueRequest;
import com.ironman.model.IssueStatus;
import com.ironman.model.LaundryOrder;
import com.ironman.model.OrderIssue;
import com.ironman.model.OrderStatus;
import com.ironman.model.User;
import com.ironman.model.UserRole;
import com.ironman.repository.LaundryOrderRepository;
import com.ironman.repository.OrderIssueRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueService {

  private final PrincipalService principalService;
  private final OrderIssueRepository issueRepository;
  private final LaundryOrderRepository orderRepository;
  private final NotificationService notificationService;

  @Transactional
  public IssueResponse create(UUID orderId, CreateIssueRequest request) {
    User customer = principalService.currentUser();
    LaundryOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Order not found"));
    if (customer.getRole() != UserRole.customer
        || !order.getCustomer().getId().equals(customer.getId())) {
      throw new NotFoundException("Order not found");
    }
    if (order.getStatus() != OrderStatus.delivered
        && order.getStatus() != OrderStatus.disputed) {
      throw new BadRequestException(
          "Issues can only be reported for delivered or disputed orders");
    }

    var issue = new OrderIssue();
    issue.setOrder(order);
    issue.setReportedBy(customer);
    issue.setType(request.type());
    issue.setDescription(request.description());
    if (request.photoUrls() != null && !request.photoUrls().isEmpty()) {
      issue.setPhotoUrls(String.join(",", request.photoUrls()));
    }
    issue = issueRepository.save(issue);

    if (order.getStatus() == OrderStatus.delivered) {
      order.setStatus(OrderStatus.disputed);
      order.setUpdatedAt(Instant.now());
      orderRepository.save(order);
    }

    notificationService.notifyAdmins(
        "Issue reported — " + order.getOrderNumber(),
        customer.getFullName() + " filed a " + request.type().name() + " complaint.",
        "issue_created", order.getId());
    return IssueResponse.from(issue);
  }

  @Transactional(readOnly = true)
  public List<IssueResponse> forOrder(UUID orderId) {
    User user = principalService.currentUser();
    LaundryOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Order not found"));
    if (user.getRole() == UserRole.customer
        && !order.getCustomer().getId().equals(user.getId())) {
      throw new NotFoundException("Order not found");
    }
    return issueRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
        .map(IssueResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public List<IssueResponse> mine() {
    User user = principalService.currentUser();
    return issueRepository.findByReportedByIdOrderByCreatedAtDesc(user.getId()).stream()
        .map(IssueResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public List<IssueResponse> listAdmin(IssueStatus status) {
    List<OrderIssue> issues = status == null
        ? issueRepository.findAllByOrderByCreatedAtDesc()
        : issueRepository.findByStatusOrderByCreatedAtDesc(status);
    return issues.stream().map(IssueResponse::from).toList();
  }

  @Transactional
  public IssueResponse resolve(UUID id, ResolveIssueRequest request) {
    User admin = principalService.currentUser();
    OrderIssue issue = issueRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Issue not found"));
    issue.setStatus(request.status());
    issue.setResolutionNotes(request.resolutionNotes());
    issue.setRefundAmount(request.refundAmount());
    issue.setResolvedBy(admin);
    if (request.status() == IssueStatus.resolved
        || request.status() == IssueStatus.rejected) {
      issue.setResolvedAt(Instant.now());
    }
    issue = issueRepository.save(issue);

    String label = switch (request.status()) {
      case open -> "is being reviewed";
      case in_review -> "is under review";
      case resolved -> "has been resolved";
      case rejected -> "was reviewed and not accepted";
    };
    notificationService.notifyUser(issue.getReportedBy(),
        "Complaint update — " + issue.getOrder().getOrderNumber(),
        "Your complaint " + label + ".",
        "issue_updated", issue.getOrder().getId());

    return IssueResponse.from(issue);
  }
}
