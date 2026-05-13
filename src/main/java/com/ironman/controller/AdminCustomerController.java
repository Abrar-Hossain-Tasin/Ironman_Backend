package com.ironman.controller;

import com.ironman.dto.user.CustomerDetailResponse;
import com.ironman.dto.user.CustomerListResponse;
import com.ironman.service.CustomerService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerController {
  private final CustomerService customerService;

  @GetMapping
  public CustomerListResponse list(
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size
  ) {
    return customerService.list(q, page, size);
  }

  @GetMapping("/{id}")
  public CustomerDetailResponse detail(@PathVariable UUID id) {
    return customerService.detail(id);
  }
}
