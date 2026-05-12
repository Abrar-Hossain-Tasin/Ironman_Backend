package com.ironman.repository;

import com.ironman.model.OrderReview;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderReviewRepository extends JpaRepository<OrderReview, UUID> {
  Optional<OrderReview> findByOrderId(UUID orderId);

  List<OrderReview> findByDeliveryManIdOrderByCreatedAtDesc(UUID deliveryManId);

  List<OrderReview> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

  @Query("select avg(r.overallRating) from OrderReview r where r.deliveryMan.id = ?1")
  Double averageRatingForStaff(UUID staffId);
}
