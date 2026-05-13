package com.ironman.controller;

import com.ironman.dto.auth.AuthResponse;
import com.ironman.dto.auth.CreateStaffRequest;
import com.ironman.dto.auth.EmailRequest;
import com.ironman.dto.auth.LoginRequest;
import com.ironman.dto.auth.RefreshTokenRequest;
import com.ironman.dto.auth.RegisterRequest;
import com.ironman.dto.auth.ResetPasswordRequest;
import com.ironman.dto.auth.VerifyEmailRequest;
import com.ironman.dto.common.ApiMessage;
import com.ironman.dto.user.UserSummary;
import com.ironman.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;

  @PostMapping("/register")
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @PostMapping("/refresh")
  public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return authService.refresh(request);
  }

  @PostMapping("/logout")
  public ApiMessage logout() {
    return new ApiMessage("Logged out. Discard the JWT on the client.");
  }

  // ── Email verification ────────────────────────────────────────────────────

  @PostMapping("/send-verification")
  public ApiMessage sendVerification(@Valid @RequestBody EmailRequest request) {
    authService.sendVerificationCode(request.email());
    return new ApiMessage("If an account exists for that email, a verification code has been sent.");
  }

  @PostMapping("/verify-email")
  public AuthResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
    return authService.verifyEmail(request);
  }

  // ── Password reset ────────────────────────────────────────────────────────

  @PostMapping("/forgot-password")
  public ApiMessage forgotPassword(@Valid @RequestBody EmailRequest request) {
    authService.requestPasswordReset(request.email());
    return new ApiMessage("If an account exists for that email, a reset code has been sent.");
  }

  @PostMapping("/reset-password")
  public ApiMessage resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request);
    return new ApiMessage("Password updated. You can now sign in with your new password.");
  }

  // ── Staff (admin only) ────────────────────────────────────────────────────

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping("/admin/create-staff")
  public UserSummary createStaff(@Valid @RequestBody CreateStaffRequest request) {
    return authService.createStaff(request);
  }
}
