package com.ironman.controller;

import com.ironman.dto.order.TrackingResponse;
import com.ironman.repository.OrderTrackingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/admin/audit")
@RequiredArgsConstructor
public class AdminAuditController {
  private final OrderTrackingRepository orderTrackingRepository;

  @GetMapping("/order-tracking")
  @Transactional(readOnly = true)
  public List<TrackingResponse> orderTracking(
      @RequestParam(defaultValue = "100") int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 200));
    return orderTrackingRepository
        .findAllByOrderByTimestampDesc(PageRequest.of(0, safeLimit))
        .stream()
        .map(TrackingResponse::from)
        .toList();
  }
}
