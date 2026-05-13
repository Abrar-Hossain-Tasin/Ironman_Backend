package com.ironman.repository;

import com.ironman.model.Payment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
  List<Payment> findByOrderIdOrderByCollectedAtDesc(UUID orderId);

  List<Payment> findByCollectedByIdOrderByCollectedAtDesc(UUID staffId);

  List<Payment> findAllByOrderByCollectedAtDesc();

  boolean existsByPaymentReferenceIgnoreCase(String paymentReference);

  /** Payments for any of these orders. Used by the customer-detail envelope. */
  List<Payment> findByOrderIdInOrderByCollectedAtDesc(java.util.Collection<UUID> orderIds);

  /** Sum of payments a given staff member collected in a date window. */
  @Query(
      "select coalesce(sum(p.amount), 0) from Payment p "
          + "where p.collectedBy.id = ?1 and p.collectedAt >= ?2 and p.collectedAt < ?3")
  BigDecimal sumCollectedBy(UUID staffId, Instant from, Instant to);

  @Query(
      "select count(p) from Payment p "
          + "where p.collectedBy.id = ?1 and p.collectedAt >= ?2 and p.collectedAt < ?3")
  long countCollectedBy(UUID staffId, Instant from, Instant to);

  @Query(
      "select coalesce(sum(p.amount), 0) from Payment p "
          + "where p.collectedAt >= ?1 and p.collectedAt < ?2")
  BigDecimal sumBetween(Instant from, Instant to);
}
