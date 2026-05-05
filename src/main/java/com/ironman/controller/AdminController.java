package com.ironman.controller;

import com.ironman.dto.order.AssignmentResponse;
import com.ironman.dto.user.UserSummary;
import com.ironman.model.UserRole;
import com.ironman.service.AdminService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
  private final AdminService adminService;

  @GetMapping("/assignments")
  public List<AssignmentResponse> assignments() {
    return adminService.assignments();
  }

  @GetMapping("/staff")
  public List<UserSummary> staff(@RequestParam(required = false) UserRole role) {
    return adminService.staff(role);
  }
}
