package com.ironman.dto.order;

import com.ironman.model.OrderReview;
import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
    UUID id,
    UUID orderId,
    UUID customerId,
    String customerName,
    UUID deliveryManId,
    int overallRating,
    Integer serviceRating,
    Integer deliveryRating,
    String comment,
    Instant createdAt
) {
  public static ReviewResponse from(OrderReview review) {
    return new ReviewResponse(
        review.getId(),
        review.getOrder().getId(),
        review.getCustomer().getId(),
        review.getCustomer().getFullName(),
        review.getDeliveryMan() == null ? null : review.getDeliveryMan().getId(),
        review.getOverallRating(),
        review.getServiceRating(),
        review.getDeliveryRating(),
        review.getComment(),
        review.getCreatedAt()
    );
  }
}
