package com.ironman.service;

import com.ironman.config.BadRequestException;
import com.ironman.config.NotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ironman.dto.payment.BkashMerchantPaymentRequest;
import com.ironman.dto.payment.CodPaymentStatusResponse;
import com.ironman.dto.payment.PaymentAuditEventResponse;
import com.ironman.dto.payment.PaymentProviderSummary;
import com.ironman.dto.payment.PaymentRecordRequest;
import com.ironman.dto.payment.PaymentReconciliationResponse;
import com.ironman.dto.payment.PaymentResponse;
import com.ironman.dto.payment.PaymentWebhookEventResponse;
import com.ironman.dto.payment.PaymentWebhookSettlementRequest;
import com.ironman.dto.payment.PaymentWebhookResponse;
import com.ironman.dto.payment.RefundRequest;
import com.ironman.dto.payment.RefundResponse;
import com.ironman.model.AssignmentStatus;
import com.ironman.model.AssignmentType;
import com.ironman.model.CodConfirmationStatus;
import com.ironman.model.LaundryOrder;
import com.ironman.model.OrderAssignment;
import com.ironman.model.Payment;
import com.ironman.model.PaymentAuditEvent;
import com.ironman.model.PaymentMethod;
import com.ironman.model.PaymentStatus;
import com.ironman.model.PaymentType;
import com.ironman.model.PaymentWebhookEvent;
import com.ironman.model.PaymentWebhookStatus;
import com.ironman.model.Refund;
import com.ironman.model.RefundStatus;
import com.ironman.model.User;
import com.ironman.model.UserRole;
import com.ironman.repository.LaundryOrderRepository;
import com.ironman.repository.OrderAssignmentRepository;
import com.ironman.repository.PaymentAuditEventRepository;
import com.ironman.repository.PaymentRepository;
import com.ironman.repository.PaymentWebhookEventRepository;
import com.ironman.repository.RefundRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
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
  private final PaymentWebhookEventRepository webhookEventRepository;
  private final PaymentAuditEventRepository auditEventRepository;
  private final PaymentWebhookSignatureVerifier webhookSignatureVerifier;
  private final com.ironman.config.PaymentWebhookProperties webhookProperties;
  private final ObjectMapper objectMapper;

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
    payment.setAppliedToBalance(false);
    payment = paymentRepository.save(payment);

    applyPaymentToOrder(order, payment, collector, "delivery_payment_recorded",
        "Delivery staff recorded a direct customer payment");

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
    payment.setVerified(false);
    payment.setAppliedToBalance(false);
    payment = paymentRepository.save(payment);

    auditPaymentChange(
        order,
        payment,
        customer,
        "customer_payment_submitted",
        order.getPaymentStatus(),
        order.getPaymentStatus(),
        order.getPaidAmount(),
        order.getPaidAmount(),
        "Customer submitted bKash reference for admin verification",
        metadata("transactionId", transactionId)
    );

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

  @Transactional(readOnly = true)
  public PaymentReconciliationResponse reconciliation() {
    List<Payment> payments = paymentRepository.findAllByOrderByCollectedAtDesc();
    BigDecimal ledgerTotal = payments.stream()
        .map(Payment::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal verifiedTotal = payments.stream()
        .filter(Payment::isVerified)
        .map(Payment::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal unverifiedTotal = payments.stream()
        .filter(payment -> !payment.isVerified())
        .map(Payment::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal unappliedTotal = payments.stream()
        .filter(payment -> !payment.isAppliedToBalance())
        .map(Payment::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    Map<String, List<Payment>> byProvider = payments.stream()
        .collect(java.util.stream.Collectors.groupingBy(payment -> providerName(payment.getPaymentType())));
    List<PaymentProviderSummary> providerSummaries = byProvider.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> {
          List<Payment> providerPayments = entry.getValue();
          BigDecimal total = providerPayments.stream()
              .map(Payment::getAmount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
          BigDecimal providerVerified = providerPayments.stream()
              .filter(Payment::isVerified)
              .map(Payment::getAmount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
          long unverified = providerPayments.stream().filter(payment -> !payment.isVerified()).count();
          long unapplied = providerPayments.stream().filter(payment -> !payment.isAppliedToBalance()).count();
          return new PaymentProviderSummary(
              entry.getKey(), total, providerVerified, providerPayments.size(), unverified, unapplied);
        })
        .toList();

    var page = PageRequest.of(0, 25);
    return new PaymentReconciliationResponse(
        Instant.now(),
        ledgerTotal,
        verifiedTotal,
        unverifiedTotal,
        unappliedTotal,
        payments.size(),
        payments.stream().filter(payment -> !payment.isVerified()).count(),
        payments.stream().filter(payment -> !payment.isAppliedToBalance()).count(),
        webhookEventRepository.countByStatus(PaymentWebhookStatus.processed),
        webhookEventRepository.countByStatus(PaymentWebhookStatus.retry_scheduled),
        webhookEventRepository.countByStatus(PaymentWebhookStatus.failed),
        providerSummaries,
        webhookEventRepository.findAllByOrderByCreatedAtDesc(page).stream()
            .map(PaymentWebhookEventResponse::from)
            .toList(),
        auditEventRepository.findAllByOrderByCreatedAtDesc(page).stream()
            .map(PaymentAuditEventResponse::from)
            .toList()
    );
  }

  @Transactional(readOnly = true)
  public List<PaymentAuditEventResponse> paymentAudit(int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 200));
    return auditEventRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit))
        .stream()
        .map(PaymentAuditEventResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<PaymentWebhookEventResponse> webhookEvents(int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 200));
    return webhookEventRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit))
        .stream()
        .map(PaymentWebhookEventResponse::from)
        .toList();
  }

  @Transactional
  public PaymentResponse verify(UUID id) {
    User admin = principalService.currentUser();
    Payment payment = paymentRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Payment not found"));
    boolean wasVerified = payment.isVerified();
    payment.setVerified(true);
    payment.setVerifiedBy(admin);
    payment.setVerifiedAt(Instant.now());
    payment = paymentRepository.save(payment);
    if (!payment.isAppliedToBalance()) {
      applyPaymentToOrder(
          payment.getOrder(),
          payment,
          admin,
          "admin_payment_verified",
          "Admin verified payment reference " + safeReference(payment)
      );
    } else if (!wasVerified) {
      auditPaymentChange(
          payment.getOrder(),
          payment,
          admin,
          "admin_payment_verified",
          payment.getOrder().getPaymentStatus(),
          payment.getOrder().getPaymentStatus(),
          payment.getOrder().getPaidAmount(),
          payment.getOrder().getPaidAmount(),
          "Admin verified already-settled payment " + safeReference(payment),
          null
      );
    }
    return PaymentResponse.from(payment);
  }

  @Transactional
  public PaymentWebhookResponse settleProviderWebhook(
      String provider,
      String rawPayload,
      HttpHeaders headers) {
    var verification = webhookSignatureVerifier.verify(provider, rawPayload, headers);
    PaymentWebhookSettlementRequest request = parseWebhookPayload(rawPayload);
    return processWebhookSettlement(verification, rawPayload, request);
  }

  @Transactional
  public PaymentWebhookResponse retryWebhookEvent(UUID eventId) {
    PaymentWebhookEvent event = webhookEventRepository.findById(eventId)
        .orElseThrow(() -> new NotFoundException("Webhook event not found"));
    if (event.getStatus() == PaymentWebhookStatus.processed
        || event.getStatus() == PaymentWebhookStatus.duplicate) {
      return PaymentWebhookResponse.from(event, "Webhook event is already closed");
    }
    var verification = new PaymentWebhookSignatureVerifier.VerifiedWebhook(
        event.getProvider(),
        event.getEventId(),
        event.getIdempotencyKey(),
        event.getSignatureHeader(),
        null,
        null,
        null,
        event.getPayloadSha256()
    );
    return processWebhookSettlement(
        verification,
        event.getRawPayload(),
        parseWebhookPayload(event.getRawPayload()),
        event
    );
  }

  private PaymentWebhookResponse processWebhookSettlement(
      PaymentWebhookSignatureVerifier.VerifiedWebhook verification,
      String rawPayload,
      PaymentWebhookSettlementRequest request
  ) {
    String idempotencyKey = webhookIdempotencyKey(verification, request);
    PaymentWebhookEvent event = findExistingWebhookEvent(verification, request, idempotencyKey)
        .orElseGet(() -> newWebhookEvent(verification, rawPayload, request, idempotencyKey));
    return processWebhookSettlement(verification, rawPayload, request, event);
  }

  private PaymentWebhookResponse processWebhookSettlement(
      PaymentWebhookSignatureVerifier.VerifiedWebhook verification,
      String rawPayload,
      PaymentWebhookSettlementRequest request,
      PaymentWebhookEvent event
  ) {
    PaymentType paymentType = providerPaymentType(verification.provider());
    String transactionId = blankToNull(request.transactionId());
    if (transactionId == null) {
      throw new BadRequestException("Transaction ID is required");
    }

    if (event.getStatus() == PaymentWebhookStatus.processed
        || event.getStatus() == PaymentWebhookStatus.duplicate) {
      return PaymentWebhookResponse.from(event, "Webhook event already handled");
    }

    var existing = paymentRepository.findByPaymentReferenceIgnoreCase(transactionId);
    if (existing.isPresent()) {
      Payment existingPayment = existing.get();
      event.setPayment(existingPayment);
      event.setOrder(existingPayment.getOrder());
      event.setStatus(PaymentWebhookStatus.duplicate);
      event.setProcessedAt(Instant.now());
      event.setNextRetryAt(null);
      event.setLastError(null);
      event = webhookEventRepository.save(event);
      return PaymentWebhookResponse.from(event, "Duplicate payment reference ignored");
    }

    event.setAttemptCount(event.getAttemptCount() + 1);
    webhookEventRepository.save(event);
    try {
      LaundryOrder order = orderRepository.findById(request.orderId())
          .orElseThrow(() -> new NotFoundException("Order not found"));
      ensurePaymentCanApply(order, request.amount());
      var payment = new Payment();
      payment.setOrder(order);
      payment.setAmount(request.amount());
      payment.setPaymentType(paymentType);
      payment.setPaymentReference(transactionId);
      payment.setPayerPhone(blankToNull(request.payerPhone()));
      payment.setNotes(verification.provider() + " webhook"
          + (blankToNull(request.eventId()) == null ? "" : " event " + request.eventId()));
      payment.setVerified(true);
      payment.setVerifiedAt(Instant.now());
      payment.setAppliedToBalance(false);
      payment = paymentRepository.save(payment);
      applyPaymentToOrder(order, payment, null, "provider_webhook_settled",
          verification.provider() + " webhook settled payment " + transactionId);

      event.setPayment(payment);
      event.setOrder(order);
      event.setStatus(PaymentWebhookStatus.processed);
      event.setProcessedAt(Instant.now());
      event.setNextRetryAt(null);
      event.setLastError(null);
      event = webhookEventRepository.save(event);

      notifyPaymentSettled(order, verification.provider());

      return PaymentWebhookResponse.from(event, "Payment settled");
    } catch (RuntimeException ex) {
      event.setLastError(ex.getMessage());
      scheduleWebhookRetry(event);
      event = webhookEventRepository.save(event);
      return PaymentWebhookResponse.from(event, "Webhook processing failed and was queued for retry");
    }
  }

  private void applyPaymentToOrder(
      LaundryOrder order,
      Payment payment,
      User actor,
      String action,
      String note
  ) {
    if (payment.isAppliedToBalance()) {
      return;
    }
    ensurePaymentCanApply(order, payment.getAmount());
    BigDecimal previousPaid = order.getPaidAmount();
    PaymentStatus previousStatus = order.getPaymentStatus();
    BigDecimal amount = payment.getAmount();
    var paid = order.getPaidAmount().add(amount);
    order.setPaidAmount(paid);
    order.setPaymentStatus(
        paid.compareTo(order.getTotalAmount()) >= 0 ? PaymentStatus.paid : PaymentStatus.partial
    );
    orderRepository.save(order);
    payment.setAppliedToBalance(true);
    paymentRepository.save(payment);
    auditPaymentChange(
        order,
        payment,
        actor,
        action,
        previousStatus,
        order.getPaymentStatus(),
        previousPaid,
        order.getPaidAmount(),
        note,
        null
    );
  }

  private void ensurePaymentCanApply(LaundryOrder order, BigDecimal amount) {
    BigDecimal due = order.getTotalAmount().subtract(order.getPaidAmount());
    if (due.signum() <= 0) {
      throw new BadRequestException("Order is already paid");
    }
    if (amount.compareTo(due) > 0) {
      throw new BadRequestException("Payment cannot exceed remaining due: " + due);
    }
  }

  private PaymentType providerPaymentType(String provider) {
    String normalized = provider == null ? "" : provider.trim().toLowerCase();
    return switch (normalized) {
      case "bkash" -> PaymentType.bkash_merchant;
      case "nagad" -> PaymentType.nagad_merchant;
      case "rocket" -> PaymentType.rocket_merchant;
      case "card" -> PaymentType.card;
      default -> throw new BadRequestException("Unsupported payment provider");
    };
  }

  private String providerName(PaymentType paymentType) {
    return switch (paymentType) {
      case bkash_merchant -> "bkash";
      case nagad_merchant -> "nagad";
      case rocket_merchant -> "rocket";
      case card -> "card";
      case cod_pickup, cod_delivery -> "cod";
      case advance, partial -> "manual";
    };
  }

  private PaymentWebhookSettlementRequest parseWebhookPayload(String rawPayload) {
    try {
      PaymentWebhookSettlementRequest request =
          objectMapper.readValue(rawPayload, PaymentWebhookSettlementRequest.class);
      validateWebhookRequest(request);
      return request;
    } catch (JsonProcessingException ex) {
      throw new BadRequestException("Invalid webhook payload");
    }
  }

  private void validateWebhookRequest(PaymentWebhookSettlementRequest request) {
    if (request.orderId() == null) {
      throw new BadRequestException("Order ID is required");
    }
    if (request.amount() == null || request.amount().signum() <= 0) {
      throw new BadRequestException("Payment amount must be positive");
    }
    if (blankToNull(request.transactionId()) == null) {
      throw new BadRequestException("Transaction ID is required");
    }
  }

  private PaymentWebhookEvent newWebhookEvent(
      PaymentWebhookSignatureVerifier.VerifiedWebhook verification,
      String rawPayload,
      PaymentWebhookSettlementRequest request,
      String idempotencyKey
  ) {
    var event = new PaymentWebhookEvent();
    event.setProvider(verification.provider());
    event.setEventId(firstNonBlank(verification.eventId(), request.eventId()));
    event.setIdempotencyKey(idempotencyKey);
    event.setPayloadSha256(verification.payloadSha256());
    event.setSignatureHeader(verification.signature());
    event.setRequestHeaders(webhookHeaderSnapshot(verification));
    event.setRawPayload(rawPayload);
    event.setStatus(PaymentWebhookStatus.received);
    event.setOrder(orderRepository.findById(request.orderId()).orElse(null));
    return webhookEventRepository.save(event);
  }

  private String webhookIdempotencyKey(
      PaymentWebhookSignatureVerifier.VerifiedWebhook verification,
      PaymentWebhookSettlementRequest request
  ) {
    return firstNonBlank(
        verification.idempotencyKey(),
        verification.eventId(),
        request.eventId(),
        request.transactionId(),
        verification.payloadSha256()
    );
  }

  private java.util.Optional<PaymentWebhookEvent> findExistingWebhookEvent(
      PaymentWebhookSignatureVerifier.VerifiedWebhook verification,
      PaymentWebhookSettlementRequest request,
      String idempotencyKey
  ) {
    var byKey = webhookEventRepository
        .findByProviderAndIdempotencyKey(verification.provider(), idempotencyKey);
    if (byKey.isPresent()) {
      return byKey;
    }
    String eventId = firstNonBlank(verification.eventId(), request.eventId());
    return eventId == null
        ? java.util.Optional.empty()
        : webhookEventRepository.findByProviderAndEventId(verification.provider(), eventId);
  }

  private String webhookHeaderSnapshot(PaymentWebhookSignatureVerifier.VerifiedWebhook verification) {
    Map<String, String> headers = new LinkedHashMap<>();
    if (verification.signatureHeader() != null) {
      headers.put(verification.signatureHeader(), verification.signature());
    }
    if (verification.timestampHeader() != null) {
      headers.put(verification.timestampHeader(), verification.timestamp());
    }
    if (verification.idempotencyKey() != null) {
      headers.put("idempotencyKey", verification.idempotencyKey());
    }
    try {
      return objectMapper.writeValueAsString(headers);
    } catch (JsonProcessingException ex) {
      return "{}";
    }
  }

  private void scheduleWebhookRetry(PaymentWebhookEvent event) {
    int maxAttempts = Math.max(1, webhookProperties.getMaxAttempts());
    if (event.getAttemptCount() >= maxAttempts) {
      event.setStatus(PaymentWebhookStatus.failed);
      event.setNextRetryAt(null);
      return;
    }
    long multiplier = 1L << Math.min(10, Math.max(0, event.getAttemptCount() - 1));
    long delaySeconds = Math.min(
        webhookProperties.getRetryBaseDelay().toSeconds() * multiplier,
        java.time.Duration.ofHours(12).toSeconds());
    event.setStatus(PaymentWebhookStatus.retry_scheduled);
    event.setNextRetryAt(Instant.now().plusSeconds(Math.max(60, delaySeconds)));
  }

  private void notifyPaymentSettled(LaundryOrder order, String provider) {
    try {
      notificationService.notifyUser(
          order.getCustomer(),
          "Payment settled - " + order.getOrderNumber(),
          "Your " + provider + " payment has been settled.",
          "payment_settled",
          order.getId()
      );
    } catch (RuntimeException ignored) {
      // Payment settlement must not be retried only because the notification channel failed.
    }
  }

  private void auditPaymentChange(
      LaundryOrder order,
      Payment payment,
      User actor,
      String action,
      PaymentStatus previousStatus,
      PaymentStatus newStatus,
      BigDecimal previousPaid,
      BigDecimal newPaid,
      String notes,
      String metadata
  ) {
    var audit = new PaymentAuditEvent();
    audit.setOrder(order);
    audit.setPayment(payment);
    audit.setActor(actor);
    audit.setActorType(actor == null ? "provider" : actor.getRole().name());
    audit.setAction(action);
    audit.setPreviousPaymentStatus(previousStatus);
    audit.setNewPaymentStatus(newStatus);
    audit.setPreviousPaidAmount(previousPaid);
    audit.setNewPaidAmount(newPaid);
    audit.setNotes(notes);
    audit.setMetadata(metadata);
    auditEventRepository.save(audit);
  }

  private String metadata(String key, String value) {
    if (value == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(Map.of(key, value));
    } catch (JsonProcessingException ex) {
      return null;
    }
  }

  private String safeReference(Payment payment) {
    return payment.getPaymentReference() == null
        ? payment.getId().toString()
        : payment.getPaymentReference();
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      String cleaned = blankToNull(value);
      if (cleaned != null) {
        return cleaned;
      }
    }
    return null;
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
    BigDecimal previousPaid = order.getPaidAmount();
    PaymentStatus previousStatus = order.getPaymentStatus();
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

    auditPaymentChange(
        order,
        null,
        admin,
        "admin_refund_processed",
        previousStatus,
        order.getPaymentStatus(),
        previousPaid,
        order.getPaidAmount(),
        "Refund processed for BDT " + refund.getAmount(),
        metadata("refundId", refund.getId().toString())
    );

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
