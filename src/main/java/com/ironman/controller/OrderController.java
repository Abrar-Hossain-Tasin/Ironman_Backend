package com.ironman.controller;

import com.ironman.dto.common.ApiMessage;
import com.ironman.dto.order.CreateIssueRequest;
import com.ironman.dto.order.IssueResponse;
import com.ironman.dto.order.OrderItemResponse;
import com.ironman.dto.order.OrderResponse;
import com.ironman.dto.order.OrderSearchResponse;
import com.ironman.dto.order.PlaceOrderRequest;
import com.ironman.dto.order.QuoteRequest;
import com.ironman.dto.order.QuoteResponse;
import com.ironman.dto.order.RescheduleRequest;
import com.ironman.dto.order.ReviewRequest;
import com.ironman.dto.order.ReviewResponse;
import com.ironman.dto.order.TrackingResponse;
import com.ironman.dto.payment.BkashMerchantPaymentRequest;
import com.ironman.dto.payment.CodPaymentStatusResponse;
import com.ironman.dto.payment.PaymentResponse;
import com.ironman.dto.payment.RefundResponse;
import com.ironman.model.OrderStatus;
import com.ironman.service.EmailService;
import com.ironman.service.IssueService;
import com.ironman.service.OrderService;
import com.ironman.service.PaymentService;
import com.ironman.service.ReceiptService;
import com.ironman.service.ReviewService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
  private final OrderService orderService;
  private final PaymentService paymentService;
  private final ReceiptService receiptService;
  private final EmailService emailService;
  private final IssueService issueService;
  private final ReviewService reviewService;

  @PostMapping
  public OrderResponse place(@Valid @RequestBody PlaceOrderRequest request) {
    return orderService.placeOrder(request);
  }

  @GetMapping
  public List<OrderResponse> list() {
    return orderService.listMineOrAll();
  }

  @GetMapping("/{id}")
  public OrderResponse detail(@PathVariable UUID id) {
    return orderService.detail(id);
  }

  @GetMapping("/{id}/tracking")
  public List<TrackingResponse> tracking(@PathVariable UUID id) {
    return orderService.tracking(id);
  }

  @GetMapping("/{id}/items")
  public List<OrderItemResponse> items(@PathVariable UUID id) {
    return orderService.items(id);
  }

  @GetMapping("/{id}/payments")
  public List<PaymentResponse> payments(@PathVariable UUID id) {
    return paymentService.forOrderScoped(id);
  }

  @PostMapping("/{id}/payments/bkash")
  public PaymentResponse submitBkashPayment(
      @PathVariable UUID id,
      @Valid @RequestBody BkashMerchantPaymentRequest request
  ) {
    return paymentService.recordMerchantBkash(id, request);
  }

  @PutMapping("/{id}/cancel")
  public ApiMessage cancel(@PathVariable UUID id) {
    orderService.cancel(id);
    return new ApiMessage("Order cancelled");
  }

  @PostMapping("/quote")
  public QuoteResponse quote(@Valid @RequestBody QuoteRequest request) {
    return orderService.quote(request);
  }

  @PatchMapping("/{id}/reschedule")
  public OrderResponse reschedule(@PathVariable UUID id,
                                  @RequestBody RescheduleRequest request) {
    return orderService.reschedule(id, request);
  }

  @GetMapping("/search")
  public OrderSearchResponse search(
      @RequestParam(required = false) OrderStatus status,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(required = false) BigDecimal minAmount,
      @RequestParam(required = false) BigDecimal maxAmount,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    return orderService.search(status, from, to, minAmount, maxAmount, page, size);
  }

  @PostMapping("/{id}/issues")
  public IssueResponse reportIssue(@PathVariable UUID id,
                                   @Valid @RequestBody CreateIssueRequest request) {
    return issueService.create(id, request);
  }

  @GetMapping("/{id}/issues")
  public List<IssueResponse> issuesForOrder(@PathVariable UUID id) {
    return issueService.forOrder(id);
  }

  @GetMapping("/issues/mine")
  public List<IssueResponse> myIssues() {
    return issueService.mine();
  }

  @PostMapping("/{id}/review")
  public ReviewResponse review(@PathVariable UUID id,
                               @Valid @RequestBody ReviewRequest request) {
    return reviewService.submit(id, request);
  }

  @GetMapping("/{id}/review")
  public ReviewResponse reviewForOrder(@PathVariable UUID id) {
    return reviewService.forOrder(id);
  }

  @GetMapping("/{id}/refunds")
  public List<RefundResponse> refundsForOrder(@PathVariable UUID id) {
    return paymentService.refundsForOrder(id);
  }

  @PatchMapping("/{id}/customer-payment-confirm")
  public CodPaymentStatusResponse customerPaymentConfirm(@PathVariable UUID id) {
    return paymentService.customerConfirmCodPayment(id);
  }

  @PatchMapping("/{id}/delivery-payment-confirm")
  public CodPaymentStatusResponse deliveryPaymentConfirm(@PathVariable UUID id) {
    return paymentService.deliveryConfirmCodPayment(id);
  }

  @GetMapping("/{id}/payment-status")
  public CodPaymentStatusResponse paymentStatus(@PathVariable UUID id) {
    return paymentService.codPaymentStatus(id);
  }

  @GetMapping("/{id}/receipt")
  public ResponseEntity<byte[]> receipt(@PathVariable UUID id) {
    ReceiptService.RenderedReceipt rendered = receiptService.generate(id);
    String filename = "receipt_" + rendered.order().getId() + "_"
        + LocalDateTime.now(ZoneId.of("Asia/Dhaka"))
              .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
        + ".pdf";
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + filename + "\"")
        .body(rendered.pdf());
  }

  @PostMapping("/{id}/send-receipt")
  public ApiMessage sendReceipt(@PathVariable UUID id) {
    ReceiptService.RenderedReceipt rendered = receiptService.generate(id);
    var customer = rendered.order().getCustomer();
    String filename = "receipt_" + rendered.order().getOrderNumber() + ".pdf";
    String body = """
        Dear %s,

        Please find attached the payment receipt for your order %s.
        Voucher No: %s

        — Ironman Laundry
        """.formatted(customer.getFullName(),
            rendered.order().getOrderNumber(),
            rendered.receipt().getReceiptNumber());
    emailService.sendWithAttachment(
        customer.getEmail(),
        "Your receipt — " + rendered.order().getOrderNumber(),
        body,
        filename,
        rendered.pdf(),
        MediaType.APPLICATION_PDF_VALUE);
    return new ApiMessage("Receipt sent to " + customer.getEmail());
  }
}
