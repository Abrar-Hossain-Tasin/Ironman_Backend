package com.ironman.repository;

import com.ironman.model.OrderReceipt;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderReceiptRepository extends JpaRepository<OrderReceipt, UUID> {
  Optional<OrderReceipt> findByOrderId(UUID orderId);

  long countByGeneratedAtBetween(Instant start, Instant end);
}
