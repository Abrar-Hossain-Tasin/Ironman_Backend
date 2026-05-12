package com.ironman.controller;

import com.ironman.dto.payment.RefundRequest;
import com.ironman.dto.payment.RefundResponse;
import com.ironman.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/admin/refunds")
@RequiredArgsConstructor
public class AdminRefundController {

  private final PaymentService paymentService;

  @GetMapping
  public List<RefundResponse> ledger() {
    return paymentService.refundLedger();
  }

  @PostMapping("/orders/{orderId}")
  public RefundResponse request(@PathVariable UUID orderId,
                                @Valid @RequestBody RefundRequest request) {
    return paymentService.requestRefund(orderId, request);
  }

  @PutMapping("/{id}/process")
  public RefundResponse process(@PathVariable UUID id) {
    return paymentService.processRefund(id);
  }

  @PutMapping("/{id}/fail")
  public RefundResponse fail(@PathVariable UUID id,
                             @RequestParam(required = false) String reason) {
    return paymentService.failRefund(id, reason);
  }
}
