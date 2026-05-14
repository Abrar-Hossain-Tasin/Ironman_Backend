package com.ironman.controller;

import com.ironman.dto.payment.PaymentAuditEventResponse;
import com.ironman.dto.payment.PaymentReconciliationResponse;
import com.ironman.dto.payment.PaymentResponse;
import com.ironman.dto.payment.PaymentWebhookEventResponse;
import com.ironman.dto.payment.PaymentWebhookResponse;
import com.ironman.service.PaymentService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentsController {
  private final PaymentService paymentService;

  @GetMapping
  public List<PaymentResponse> ledger() {
    return paymentService.ledger();
  }

  @GetMapping("/reconciliation")
  public PaymentReconciliationResponse reconciliation() {
    return paymentService.reconciliation();
  }

  @GetMapping("/audit")
  public List<PaymentAuditEventResponse> audit(
      @RequestParam(defaultValue = "100") int limit) {
    return paymentService.paymentAudit(limit);
  }

  @GetMapping("/webhook-events")
  public List<PaymentWebhookEventResponse> webhookEvents(
      @RequestParam(defaultValue = "100") int limit) {
    return paymentService.webhookEvents(limit);
  }

  @GetMapping("/orders/{orderId}")
  public List<PaymentResponse> forOrder(@PathVariable UUID orderId) {
    return paymentService.forOrder(orderId);
  }

  @GetMapping("/staff/{staffId}")
  public List<PaymentResponse> forStaff(@PathVariable UUID staffId) {
    return paymentService.forStaff(staffId);
  }

  @PutMapping("/{id}/verify")
  public PaymentResponse verify(@PathVariable UUID id) {
    return paymentService.verify(id);
  }

  @PostMapping("/webhook-events/{id}/retry")
  public PaymentWebhookResponse retryWebhook(@PathVariable UUID id) {
    return paymentService.retryWebhookEvent(id);
  }
}
