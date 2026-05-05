package com.ironman.controller;

import com.ironman.dto.common.ApiMessage;
import com.ironman.dto.order.OrderItemResponse;
import com.ironman.dto.order.OrderResponse;
import com.ironman.dto.order.PlaceOrderRequest;
import com.ironman.dto.order.TrackingResponse;
import com.ironman.dto.payment.PaymentResponse;
import com.ironman.service.OrderService;
import com.ironman.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
  private final OrderService orderService;
  private final PaymentService paymentService;

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

  @PutMapping("/{id}/cancel")
  public ApiMessage cancel(@PathVariable UUID id) {
    orderService.cancel(id);
    return new ApiMessage("Order cancelled");
  }
}
