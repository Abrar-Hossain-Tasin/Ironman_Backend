package com.ironman.repository;

import com.ironman.model.Refund;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RefundRepository extends JpaRepository<Refund, UUID> {
  List<Refund> findByOrderIdOrderByRequestedAtDesc(UUID orderId);

  List<Refund> findAllByOrderByRequestedAtDesc();

  @Query("select coalesce(sum(r.amount), 0) from Refund r "
      + "where r.order.id = ?1 and r.status = com.ironman.model.RefundStatus.processed")
  BigDecimal totalProcessedForOrder(UUID orderId);
}
