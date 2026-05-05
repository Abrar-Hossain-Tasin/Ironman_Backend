package com.ironman.controller;

import com.ironman.dto.payment.PaymentResponse;
import com.ironman.service.PaymentService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
