package com.ironman.repository;

import com.ironman.model.PaymentAuditEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAuditEventRepository extends JpaRepository<PaymentAuditEvent, UUID> {
  List<PaymentAuditEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);

  List<PaymentAuditEvent> findByPaymentIdOrderByCreatedAtDesc(UUID paymentId);
}
