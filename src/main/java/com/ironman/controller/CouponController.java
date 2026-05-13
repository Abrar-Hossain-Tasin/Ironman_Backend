package com.ironman.controller;

import com.ironman.dto.order.CouponResponse;
import com.ironman.service.CouponService;
import com.ironman.service.PrincipalService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer-facing coupon discovery. {@code /admin/coupons} (CRUD) lives in
 * {@code AdminCouponController}; this controller only exposes what a logged-in
 * customer is allowed to see.
 */
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {
  private final CouponService couponService;
  private final PrincipalService principalService;

  @PreAuthorize("hasRole('CUSTOMER')")
  @GetMapping("/active")
  public List<CouponResponse> active() {
    return couponService.activeForCustomer(principalService.currentUser());
  }
}
