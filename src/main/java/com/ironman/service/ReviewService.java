package com.ironman.service;

import com.ironman.config.BadRequestException;
import com.ironman.config.NotFoundException;
import com.ironman.dto.order.ReviewRequest;
import com.ironman.dto.order.ReviewResponse;
import com.ironman.model.AssignmentType;
import com.ironman.model.LaundryOrder;
import com.ironman.model.OrderAssignment;
import com.ironman.model.OrderReview;
import com.ironman.model.OrderStatus;
import com.ironman.model.User;
import com.ironman.model.UserRole;
import com.ironman.repository.LaundryOrderRepository;
import com.ironman.repository.OrderAssignmentRepository;
import com.ironman.repository.OrderReviewRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

  private final PrincipalService principalService;
  private final OrderReviewRepository reviewRepository;
  private final LaundryOrderRepository orderRepository;
  private final OrderAssignmentRepository assignmentRepository;

  @Transactional
  public ReviewResponse submit(UUID orderId, ReviewRequest request) {
    User customer = principalService.currentUser();
    LaundryOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Order not found"));
    if (customer.getRole() != UserRole.customer
        || !order.getCustomer().getId().equals(customer.getId())) {
      throw new NotFoundException("Order not found");
    }
    if (order.getStatus() != OrderStatus.delivered) {
      throw new BadRequestException(
          "Only delivered orders can be reviewed");
    }
    if (reviewRepository.findByOrderId(orderId).isPresent()) {
      throw new BadRequestException("Order already reviewed");
    }

    User deliveryMan = assignmentRepository
        .findFirstByOrderIdAndAssignmentTypeOrderByAssignedAtDesc(
            order.getId(), AssignmentType.delivery)
        .map(OrderAssignment::getAssignedTo)
        .orElse(null);

    var review = new OrderReview();
    review.setOrder(order);
    review.setCustomer(customer);
    review.setDeliveryMan(deliveryMan);
    review.setOverallRating(request.overallRating());
    review.setServiceRating(request.serviceRating());
    review.setDeliveryRating(request.deliveryRating());
    review.setComment(request.comment());
    return ReviewResponse.from(reviewRepository.save(review));
  }

  @Transactional(readOnly = true)
  public ReviewResponse forOrder(UUID orderId) {
    User user = principalService.currentUser();
    LaundryOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Order not found"));
    if (user.getRole() == UserRole.customer
        && !order.getCustomer().getId().equals(user.getId())) {
      throw new NotFoundException("Order not found");
    }
    OrderReview review = reviewRepository.findByOrderId(orderId)
        .orElseThrow(() -> new NotFoundException("Review not found"));
    return ReviewResponse.from(review);
  }

  @Transactional(readOnly = true)
  public List<ReviewResponse> forStaff(UUID staffId) {
    return reviewRepository.findByDeliveryManIdOrderByCreatedAtDesc(staffId).stream()
        .map(ReviewResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public Double averageForStaff(UUID staffId) {
    Double avg = reviewRepository.averageRatingForStaff(staffId);
    return avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0;
  }
}
