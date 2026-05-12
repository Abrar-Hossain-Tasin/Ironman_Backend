package com.ironman.repository;

import com.ironman.model.CodConfirmationStatus;
import com.ironman.model.LaundryOrder;
import com.ironman.model.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LaundryOrderRepository
    extends JpaRepository<LaundryOrder, UUID>, JpaSpecificationExecutor<LaundryOrder> {
  List<LaundryOrder> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

  List<LaundryOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);

  List<LaundryOrder> findAllByOrderByCreatedAtDesc();

  List<LaundryOrder> findByCodConfirmationStatusOrderByCreatedAtDesc(
      CodConfirmationStatus codConfirmationStatus);

  Optional<LaundryOrder> findByOrderNumber(String orderNumber);

  long countByCreatedAtBetween(Instant start, Instant end);
}
