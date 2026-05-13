package com.ironman.repository;

import com.ironman.model.CodConfirmationStatus;
import com.ironman.model.LaundryOrder;
import com.ironman.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface LaundryOrderRepository
    extends JpaRepository<LaundryOrder, UUID>, JpaSpecificationExecutor<LaundryOrder> {
  List<LaundryOrder> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

  List<LaundryOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);

  List<LaundryOrder> findAllByOrderByCreatedAtDesc();

  List<LaundryOrder> findByCodConfirmationStatusOrderByCreatedAtDesc(
      CodConfirmationStatus codConfirmationStatus);

  Optional<LaundryOrder> findByOrderNumber(String orderNumber);

  long countByCreatedAtBetween(Instant start, Instant end);

  long countByCustomerId(UUID customerId);

  // ── Aggregates for /admin/customers and /admin/reports/summary ───────────

  @Query("select coalesce(sum(o.totalAmount), 0) from LaundryOrder o where o.customer.id = ?1")
  BigDecimal sumTotalByCustomer(UUID customerId);

  @Query("select coalesce(sum(o.paidAmount), 0) from LaundryOrder o where o.customer.id = ?1")
  BigDecimal sumPaidByCustomer(UUID customerId);

  @Query("select max(o.createdAt) from LaundryOrder o where o.customer.id = ?1")
  Instant lastOrderAt(UUID customerId);

  List<LaundryOrder> findByCreatedAtGreaterThanEqual(Instant from);

  // For pickup/delivery slot capacity counters.
  @Query(
      "select count(o) from LaundryOrder o "
          + "where o.preferredPickupDate = ?1 and o.preferredPickupTimeSlot = ?2 "
          + "  and o.status not in (com.ironman.model.OrderStatus.cancelled, com.ironman.model.OrderStatus.returned)")
  long countActivePickupsForSlot(java.time.LocalDate date, String slot);

  @Query(
      "select count(o) from LaundryOrder o "
          + "where o.preferredDeliveryDate = ?1 and o.preferredDeliveryTimeSlot = ?2 "
          + "  and o.status not in (com.ironman.model.OrderStatus.cancelled, com.ironman.model.OrderStatus.returned)")
  long countActiveDeliveriesForSlot(java.time.LocalDate date, String slot);

  /** Bulk: how many orders does each of these customers have? */
  @Query(
      "select o.customer.id, count(o) from LaundryOrder o "
          + "where o.customer.id in ?1 group by o.customer.id")
  List<Object[]> countByCustomerIds(Collection<UUID> ids);

  @Query(
      "select o.customer.id, coalesce(sum(o.totalAmount), 0), "
          + "       coalesce(sum(o.paidAmount), 0), max(o.createdAt) "
          + "  from LaundryOrder o where o.customer.id in ?1 group by o.customer.id")
  List<Object[]> aggregatesByCustomerIds(Collection<UUID> ids);
}
