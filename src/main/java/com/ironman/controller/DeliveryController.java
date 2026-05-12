package com.ironman.controller;

import com.ironman.dto.location.LocationResponse;
import com.ironman.dto.location.LocationUpdateRequest;
import com.ironman.dto.order.AssignmentActionRequest;
import com.ironman.dto.order.AssignmentResponse;
import com.ironman.dto.order.PickupReconcileRequest;
import com.ironman.dto.payment.PaymentRecordRequest;
import com.ironman.dto.payment.PaymentResponse;
import com.ironman.service.AssignmentService;
import com.ironman.service.LocationService;
import com.ironman.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('DELIVERY_MAN')")
@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
public class DeliveryController {

  private final AssignmentService assignmentService;
  private final PaymentService paymentService;
  private final LocationService locationService;   // ← NEW

  @GetMapping("/assignments")
  public List<AssignmentResponse> assignments() {
    return assignmentService.mine();
  }

  @PutMapping("/assignments/{id}/accept")
  public AssignmentResponse accept(@PathVariable UUID id) {
    return assignmentService.accept(id);
  }

  @PutMapping("/assignments/{id}/start")
  public AssignmentResponse start(@PathVariable UUID id) {
    return assignmentService.start(id);
  }

  @PutMapping("/assignments/{id}/complete")
  public AssignmentResponse complete(@PathVariable UUID id,
                                     @RequestBody(required = false) AssignmentActionRequest request) {
    return assignmentService.complete(id, request);
  }

  @PutMapping("/assignments/{id}/reconcile")
  public AssignmentResponse reconcile(@PathVariable UUID id,
                                      @Valid @RequestBody PickupReconcileRequest request) {
    return assignmentService.reconcilePickup(id, request);
  }

  @PostMapping("/payments")
  public PaymentResponse recordPayment(
          @Valid @RequestBody PaymentRecordRequest request) {
    return paymentService.record(request);
  }

  // ── NEW: delivery man pushes their GPS position ───────────────────────────
  @PostMapping("/location")
  public LocationResponse updateLocation(
          @Valid @RequestBody LocationUpdateRequest request) {
    return locationService.updateMyLocation(request);
  }
}
