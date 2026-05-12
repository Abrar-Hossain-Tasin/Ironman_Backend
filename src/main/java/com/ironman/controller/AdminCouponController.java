package com.ironman.controller;

import com.ironman.dto.common.ApiMessage;
import com.ironman.dto.order.CouponRequest;
import com.ironman.dto.order.CouponResponse;
import com.ironman.service.CouponService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

  private final CouponService couponService;

  @GetMapping
  public List<CouponResponse> list() {
    return couponService.list();
  }

  @PostMapping
  public CouponResponse create(@Valid @RequestBody CouponRequest request) {
    return couponService.create(request);
  }

  @PutMapping("/{id}")
  public CouponResponse update(@PathVariable UUID id,
                               @Valid @RequestBody CouponRequest request) {
    return couponService.update(id, request);
  }

  @DeleteMapping("/{id}")
  public ApiMessage delete(@PathVariable UUID id) {
    couponService.delete(id);
    return new ApiMessage("Coupon deactivated");
  }
}
