package com.ironman.controller;

import com.ironman.dto.order.AssignmentRequest;
import com.ironman.dto.order.AssignmentResponse;
import com.ironman.dto.order.OrderResponse;
import com.ironman.dto.order.StatusUpdateRequest;
import com.ironman.dto.user.UserSummary;
import com.ironman.model.CodConfirmationStatus;
import com.ironman.model.OrderStatus;
import com.ironman.model.UserRole;
import com.ironman.service.AdminService;
import com.ironman.service.OrderService;
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
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {
  private final OrderService orderService;
  private final AdminService adminService;

  @GetMapping
  public List<OrderResponse> orders(
      @RequestParam(required = false) OrderStatus status,
      @RequestParam(name = "codConfirmationStatus", required = false)
          CodConfirmationStatus codConfirmationStatus
  ) {
    if (codConfirmationStatus != null) {
      return orderService.listAdminByCodConfirmation(codConfirmationStatus);
    }
    return orderService.listAdmin(status);
  }

  @PutMapping("/{id}/status")
  public OrderResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody StatusUpdateRequest request) {
    return orderService.updateStatus(id, request.status(), request.reason());
  }

  @PostMapping("/{id}/assign")
  public AssignmentResponse assign(@PathVariable UUID id, @Valid @RequestBody AssignmentRequest request) {
    return adminService.assign(id, request);
  }

  @GetMapping("/assignments")
  public List<AssignmentResponse> assignments() {
    return adminService.assignments();
  }

  @GetMapping("/staff")
  public List<UserSummary> staff(@RequestParam(required = false) UserRole role) {
    return adminService.staff(role);
  }
}
