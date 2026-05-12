package com.ironman.controller;

import com.ironman.dto.order.AssignmentResponse;
import com.ironman.dto.order.ReviewResponse;
import com.ironman.dto.user.UserSummary;
import com.ironman.model.UserRole;
import com.ironman.service.AdminService;
import com.ironman.service.ReviewService;
import java.util.List;
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
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
  private final AdminService adminService;
  private final ReviewService reviewService;

  @GetMapping("/assignments")
  public List<AssignmentResponse> assignments() {
    return adminService.assignments();
  }

  @GetMapping("/staff")
  public List<UserSummary> staff(@RequestParam(required = false) UserRole role) {
    return adminService.staff(role);
  }

  @GetMapping("/staff/{id}/reviews")
  public List<ReviewResponse> staffReviews(@PathVariable UUID id) {
    return reviewService.forStaff(id);
  }

  @GetMapping("/staff/{id}/rating")
  public Double staffRating(@PathVariable UUID id) {
    return reviewService.averageForStaff(id);
  }
}
