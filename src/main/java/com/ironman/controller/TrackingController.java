package com.ironman.controller;

import com.ironman.dto.order.TrackingResponse;
import com.ironman.service.OrderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tracking")
@RequiredArgsConstructor
public class TrackingController {
  private final OrderService orderService;

  @GetMapping("/{orderNumber}")
  public List<TrackingResponse> byOrderNumber(@PathVariable String orderNumber) {
    return orderService.publicTracking(orderNumber);
  }
}
