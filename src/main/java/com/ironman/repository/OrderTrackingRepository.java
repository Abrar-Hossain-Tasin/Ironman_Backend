package com.ironman.repository;

import com.ironman.model.OrderTracking;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderTrackingRepository extends JpaRepository<OrderTracking, UUID> {
  List<OrderTracking> findByOrderIdOrderByTimestampAsc(UUID orderId);

  Page<OrderTracking> findAllByOrderByTimestampDesc(Pageable pageable);
}
