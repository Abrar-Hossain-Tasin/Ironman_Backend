package com.ironman.controller;

import com.ironman.dto.payment.PaymentWebhookResponse;
import com.ironman.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payment-webhooks")
@RequiredArgsConstructor
public class PaymentWebhookController {
  private final PaymentService paymentService;

  @PostMapping("/{provider}/settlements")
  public ResponseEntity<PaymentWebhookResponse> settle(
      @PathVariable String provider,
      @RequestHeader HttpHeaders headers,
      @RequestBody String rawPayload) {
    PaymentWebhookResponse response =
        paymentService.settleProviderWebhook(provider, rawPayload, headers);
    HttpStatus status = switch (response.status()) {
      case processed, duplicate -> HttpStatus.OK;
      case received, retry_scheduled, failed -> HttpStatus.ACCEPTED;
    };
    return ResponseEntity.status(status).body(response);
  }
}
