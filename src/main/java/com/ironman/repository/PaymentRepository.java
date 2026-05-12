package com.ironman.repository;

import com.ironman.model.Payment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
  List<Payment> findByOrderIdOrderByCollectedAtDesc(UUID orderId);

  List<Payment> findByCollectedByIdOrderByCollectedAtDesc(UUID staffId);

  List<Payment> findAllByOrderByCollectedAtDesc();

  boolean existsByPaymentReferenceIgnoreCase(String paymentReference);
}
