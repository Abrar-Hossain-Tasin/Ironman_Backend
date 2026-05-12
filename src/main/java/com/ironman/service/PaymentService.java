package com.ironman.service;

import com.ironman.config.BadRequestException;
import com.ironman.config.NotFoundException;
import com.ironman.dto.payment.BkashMerchantPaymentRequest;
import com.ironman.dto.payment.CodPaymentStatusResponse;
import com.ironman.dto.payment.PaymentRecordRequest;
import com.ironman.dto.payment.PaymentResponse;
import com.ironman.dto.payment.RefundRequest;
import com.ironman.dto.payment.RefundResponse;
import com.ironman.model.AssignmentStatus;
import com.ironman.model.AssignmentType;
import com.ironman.model.CodConfirmationStatus;
import com.ironman.model.LaundryOrder;
import com.ironman.model.OrderAssignment;
import com.ironman.model.Payment;
import com.ironman.model.PaymentMethod;
import com.ironman.model.PaymentStatus;
import com.ironman.model.PaymentType;
import com.ironman.model.Refund;
import com.ironman.model.RefundStatus;
import com.ironman.model.User;
import com.ironman.model.UserRole;
import com.ironman.repository.LaundryOrderRepository;
import com.ironman.repository.OrderAssignmentRepository;
import com.ironman.repository.PaymentRepository;
import com.ironman.repository.RefundRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {
  private final PrincipalService principalService;
  private final PaymentRepository paymentRepository;
  private final LaundryOrderRepository orderRepository;
  private final OrderAssignmentRepository assignmentRepository;
  private final NotificationService notificationService;
  private final RefundRepository refundRepository;

  @Transactional
  public PaymentResponse record(PaymentRecordRequest request) {
    User collector = principalService.currentUser();
    LaundryOrder order = orderRepository.findById(request.orderId())
        .orElseThrow(() -> new NotFoundException("Order not found"));
    if (request.paymentType() != PaymentType.cod_pickup && request.paymentType() != PaymentType.cod_delivery) {
      throw new BadRequestException("Delivery staff can only confirm direct customer payments");
    }

    var payment = new Payment();
    payment.setOrder(order);
    payment.setCollectedBy(collector);
    payment.setAmount(request.amount());
    payment.setPaymentType(request.paymentType());
    payment.setPaymentReference(blankToNull(request.paymentReference()));
    payment.setPayerPhone(blankToNull(request.payerPhone()));
    payment.setNotes(request.notes());
    payment.setVerified(true);
    payment.setVerifiedBy(collector);
    payment.setVerifiedAt(Instant.now());
    payment = paymentRepository.save(payment);

    applyPaidAmount(order, request.amount());

    return PaymentResponse.from(payment);
  }

  @Transactional
  public PaymentResponse recordMerchantBkash(UUID orderId, BkashMerchantPaymentRequest request) {
    User customer = principalService.currentUser();
    LaundryOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Order not found"));
    if (customer.getRole() != UserRole.customer || !order.getCustomer().getId().equals(customer.getId())) {
      throw new NotFoundException("Order not found");
    }

    String transactionId = blankToNull(request.transactionId());
    if (transactionId == null) {
      throw new BadRequestException("bKash transaction ID is required");
    }
    if (paymentRepository.existsByPaymentReferenceIgnoreCase(transactionId)) {
      throw new BadRequestException("This bKash transaction ID has already been submitted");
    }

    var payment = new Payment();
    payment.setOrder(order);
    payment.setAmount(request.amount());
    payment.setPaymentType(PaymentType.bkash_merchant);
    payment.setPaymentReference(transactionId);
    payment.setPayerPhone(blankToNull(request.payerPhone()));
    payment.setNotes(request.notes());
    payment = paymentRepository.save(payment);

    applyPaidAmount(order, request.amount());

    return PaymentResponse.from(payment);
  }

  @Transactional(readOnly = true)
  public List<PaymentResponse> forOrder(UUID orderId) {
    return paymentRepository.findByOrderIdOrderByCollectedAtDesc(orderId).stream()
        .map(PaymentResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<PaymentResponse> forOrderScoped(UUID orderId) {
    User user = principalService.currentUser();
    LaundryOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Order not found"));
    if (user.getRole() == UserRole.customer && !order.getCustomer().getId().equals(user.getId())) {
      throw new NotFoundException("Order not found");
    }
    return forOrder(orderId);
  }

  @Transactional(readOnly = true)
  public List<PaymentResponse> forStaff(UUID staffId) {
    return paymentRepository.findByCollectedByIdOrderByCollectedAtDesc(staffId).stream()
        .map(PaymentResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<PaymentResponse> ledger() {
    return paymentRepository.findAllByOrderByCollectedAtDesc().stream()
        .map(PaymentResponse::from)
        .toList();
  }

  @Transactional
  public PaymentResponse verify(UUID id) {
    User admin = principalService.currentUser();
    Payment payment = paymentRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Payment not found"));
    payment.setVerified(true);
    payment.setVerifiedBy(admin);
    payment.setVerifiedAt(Instant.now());
    return PaymentResponse.from(paymentRepository.save(payment));
  }

  private void applyPaidAmount(LaundryOrder order, java.math.BigDecimal amount) {
    java.math.BigDecimal due = order.getTotalAmount().subtract(order.getPaidAmount());
    if (due.signum() <= 0) {
      throw new BadRequestException("Order is already paid");
    }
    if (amount.compareTo(due) > 0) {
      throw new BadRequestException("Payment cannot exceed remaining due: " + due);
    }
    var paid = order.getPaidAmount().add(amount);
    order.setPaidAmount(paid);
    order.setPaymentStatus(
        paid.compareTo(order.getTotalAmount()) >= 0 ? PaymentStatus.paid : PaymentStatus.partial
    );
    orderRepository.save(order);
  }

  // ── Pay-on-Delivery dual confirmation ────────────────────────────────────────

  @Transactional
  public CodPaymentStatusResponse customerConfirmCodPayment(UUID orderId) {
    User customer = principalService.currentUser();
    LaundryOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Order not found"));

    if (customer.getRole() != UserRole.customer
        || !order.getCustomer().getId().equals(customer.getId())) {
      throw new NotFoundException("Order not found");
    }
    if (order.getPaymentMethod() != PaymentMethod.cod) {
      throw new BadRequestException("Order is not cash on delivery");
    }
    if (order.getCodConfirmationStatus() != CodConfirmationStatus.pending) {
      throw new BadRequestException("Payment already confirmed");
    }

    order.setCodConfirmationStatus(CodConfirmationStatus.customer_confirmed);
    order.setCustomerConfirmedAt(Instant.now());
    order = orderRepository.save(order);

    OrderAssignment deliveryAssignment = assignedDelivery(order)
        .orElseThrow(() -> new BadRequestException(
            "No delivery man is assigned to this order yet"));

    notificationService.notifyUser(
        deliveryAssignment.getAssignedTo(),
        "Customer confirmed payment — " + order.getOrderNumber(),
        "Customer has handed over the payment. Please confirm receipt.",
        "cod_customer_confirmed",
        order.getId()
    );

    return CodPaymentStatusResponse.from(order);
  }

  @Transactional
  public CodPaymentStatusResponse deliveryConfirmCodPayment(UUID orderId) {
    User deliveryMan = principalService.currentUser();
    LaundryOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Order not found"));

    if (order.getPaymentMethod() != PaymentMethod.cod) {
      throw new BadRequestException("Order is not cash on delivery");
    }

    boolean assignedToThisDelivery = assignmentRepository
        .existsByOrderIdAndAssignedToIdAndAssignmentTypeInAndStatusIn(
            order.getId(),
            deliveryMan.getId(),
            List.of(AssignmentType.delivery),
            List.of(AssignmentStatus.pending, AssignmentStatus.accepted,
                AssignmentStatus.in_progress, AssignmentStatus.completed));
    if (!assignedToThisDelivery) {
      throw new NotFoundException("Order not found");
    }

    if (order.getCodConfirmationStatus() == CodConfirmationStatus.delivery_confirmed) {
      throw new BadRequestException("Payment already confirmed by delivery");
    }
    if (order.getCodConfirmationStatus() == CodConfirmationStatus.pending) {
      throw new BadRequestException(
          "Customer has not yet confirmed handing over the payment");
    }

    order.setCodConfirmationStatus(CodConfirmationStatus.delivery_confirmed);
    order.setDeliveryConfirmedAt(Instant.now());
    order = orderRepository.save(order);

    notificationService.notifyUser(
        order.getCustomer(),
        "Payment received — " + order.getOrderNumber(),
        "Delivery man has confirmed receiving your payment. Thank you!",
        "cod_delivery_confirmed",
        order.getId()
    );

    return CodPaymentStatusResponse.from(order);
  }

  @Transactional(readOnly = true)
  public CodPaymentStatusResponse codPaymentStatus(UUID orderId) {
    User user = principalService.currentUser();
    LaundryOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Order not found"));

    boolean allowed = switch (user.getRole()) {
      case admin -> true;
      case customer -> order.getCustomer().getId().equals(user.getId());
      case delivery_man -> assignmentRepository
          .existsByOrderIdAndAssignedToIdAndAssignmentTypeInAndStatusIn(
              order.getId(),
              user.getId(),
              List.of(AssignmentType.delivery),
              List.of(AssignmentStatus.pending, AssignmentStatus.accepted,
                  AssignmentStatus.in_progress, AssignmentStatus.completed));
      default -> false;
    };
    if (!allowed) {
      throw new NotFoundException("Order not found");
    }

    return CodPaymentStatusResponse.from(order);
  }

  private java.util.Optional<OrderAssignment> assignedDelivery(LaundryOrder order) {
    return assignmentRepository
        .findFirstByOrderIdAndAssignmentTypeOrderByAssignedAtDesc(
            order.getId(), AssignmentType.delivery);
  }

  // ── Refunds (admin) ──────────────────────────────────────────────────────────

  @Transactional
  public RefundResponse requestRefund(UUID orderId, RefundRequest request) {
    User admin = principalService.currentUser();
    LaundryOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Order not found"));

    BigDecimal alreadyRefunded = refundRepository.totalProcessedForOrder(orderId);
    BigDecimal remaining = order.getPaidAmount().subtract(alreadyRefunded);
    if (request.amount().compareTo(remaining) > 0) {
      throw new BadRequestException(
          "Refund cannot exceed refundable amount: " + remaining);
    }

    Refund refund = new Refund();
    refund.setOrder(order);
    refund.setAmount(request.amount());
    refund.setReason(request.reason());
    refund.setOriginalMethod(order.getPaymentMethod());
    refund.setTransactionReference(request.transactionReference());
    refund.setRequestedBy(admin);
    refund = refundRepository.save(refund);

    notificationService.notifyUser(order.getCustomer(),
        "Refund initiated — " + order.getOrderNumber(),
        "A refund of BDT " + request.amount() + " is being processed.",
        "refund_requested", order.getId());

    return RefundResponse.from(refund);
  }

  @Transactional
  public RefundResponse processRefund(UUID refundId) {
    User admin = principalService.currentUser();
    Refund refund = refundRepository.findById(refundId)
        .orElseThrow(() -> new NotFoundException("Refund not found"));
    if (refund.getStatus() != RefundStatus.pending) {
      throw new BadRequestException("Refund is not pending");
    }

    LaundryOrder order = refund.getOrder();
    BigDecimal newPaid = order.getPaidAmount().subtract(refund.getAmount());
    order.setPaidAmount(newPaid.max(BigDecimal.ZERO));
    if (newPaid.compareTo(BigDecimal.ZERO) <= 0) {
      order.setPaymentStatus(PaymentStatus.pending);
    } else if (newPaid.compareTo(order.getTotalAmount()) < 0) {
      order.setPaymentStatus(PaymentStatus.partial);
    }
    orderRepository.save(order);

    refund.setStatus(RefundStatus.processed);
    refund.setProcessedBy(admin);
    refund.setProcessedAt(Instant.now());
    refund = refundRepository.save(refund);

    notificationService.notifyUser(order.getCustomer(),
        "Refund processed — " + order.getOrderNumber(),
        "BDT " + refund.getAmount() + " has been refunded.",
        "refund_processed", order.getId());

    return RefundResponse.from(refund);
  }

  @Transactional
  public RefundResponse failRefund(UUID refundId, String reason) {
    User admin = principalService.currentUser();
    Refund refund = refundRepository.findById(refundId)
        .orElseThrow(() -> new NotFoundException("Refund not found"));
    if (refund.getStatus() != RefundStatus.pending) {
      throw new BadRequestException("Refund is not pending");
    }
    refund.setStatus(RefundStatus.failed);
    refund.setProcessedBy(admin);
    refund.setProcessedAt(Instant.now());
    if (reason != null && !reason.isBlank()) {
      refund.setReason((refund.getReason() == null ? "" : refund.getReason() + " | ")
          + "Failed: " + reason);
    }
    return RefundResponse.from(refundRepository.save(refund));
  }

  @Transactional(readOnly = true)
  public List<RefundResponse> refundsForOrder(UUID orderId) {
    User user = principalService.currentUser();
    LaundryOrder order = orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Order not found"));
    if (user.getRole() == UserRole.customer
        && !order.getCustomer().getId().equals(user.getId())) {
      throw new NotFoundException("Order not found");
    }
    return refundRepository.findByOrderIdOrderByRequestedAtDesc(orderId).stream()
        .map(RefundResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public List<RefundResponse> refundLedger() {
    return refundRepository.findAllByOrderByRequestedAtDesc().stream()
        .map(RefundResponse::from).toList();
  }

  private String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
