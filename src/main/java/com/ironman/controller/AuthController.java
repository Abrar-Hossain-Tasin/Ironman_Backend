package com.ironman.controller;

import com.ironman.dto.auth.AuthResponse;
import com.ironman.dto.auth.CreateStaffRequest;
import com.ironman.dto.auth.CsrfResponse;
import com.ironman.dto.auth.EmailRequest;
import com.ironman.dto.auth.LoginRequest;
import com.ironman.dto.auth.RefreshTokenRequest;
import com.ironman.dto.auth.RegisterRequest;
import com.ironman.dto.auth.ResetPasswordRequest;
import com.ironman.dto.auth.ResetStaffPasswordRequest;
import com.ironman.dto.auth.UpdateStaffRequest;
import com.ironman.dto.auth.VerifyEmailRequest;
import com.ironman.dto.common.ApiMessage;
import com.ironman.dto.user.UserSummary;
import com.ironman.config.AuthCookieService;
import com.ironman.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;
  private final AuthCookieService authCookieService;

  @PostMapping("/register")
  public AuthResponse register(@Valid @RequestBody RegisterRequest request,
                               HttpServletResponse response) {
    AuthResponse auth = authService.register(request);
    return writeAuthResponse(response, auth);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request,
                            HttpServletResponse response) {
    AuthResponse auth = authService.login(request);
    return writeAuthResponse(response, auth);
  }

  @PostMapping("/refresh")
  public AuthResponse refresh(@RequestBody(required = false) RefreshTokenRequest request,
                              @CookieValue(name = AuthCookieService.REFRESH_COOKIE, required = false) String refreshCookie,
                              HttpServletResponse response) {
    String refreshToken = request != null && request.refreshToken() != null
        ? request.refreshToken()
        : refreshCookie;
    AuthResponse auth = authService.refresh(refreshToken);
    return writeAuthResponse(response, auth);
  }

  @GetMapping("/csrf")
  public CsrfResponse csrf(HttpServletResponse response) {
    String csrfToken = authCookieService.newCsrfToken();
    authCookieService.writeCsrfCookie(response, csrfToken);
    return new CsrfResponse(csrfToken);
  }

  @PostMapping("/logout")
  public ApiMessage logout(HttpServletResponse response) {
    authCookieService.clearAuthCookies(response);
    return new ApiMessage("Logged out.");
  }

  // ── Email verification ────────────────────────────────────────────────────

  @PostMapping("/send-verification")
  public ApiMessage sendVerification(@Valid @RequestBody EmailRequest request) {
    authService.sendVerificationCode(request.email());
    return new ApiMessage("If an account exists for that email, a verification code has been sent.");
  }

  @PostMapping("/verify-email")
  public AuthResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request,
                                  HttpServletResponse response) {
    AuthResponse auth = authService.verifyEmail(request);
    return writeAuthResponse(response, auth);
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

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/admin/staff/{id}")
  public UserSummary updateStaff(@PathVariable UUID id,
                                 @Valid @RequestBody UpdateStaffRequest request) {
    return authService.updateStaff(id, request);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/admin/staff/{id}/activate")
  public UserSummary activateStaff(@PathVariable UUID id) {
    return authService.setStaffActive(id, true);
  }

  /**
   * Soft-deactivates the staff account. Login is blocked but the row + history
   * stay so payments/assignments retain a stable foreign key.
   */
  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/admin/staff/{id}")
  public UserSummary deactivateStaff(@PathVariable UUID id) {
    return authService.setStaffActive(id, false);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/admin/staff/{id}/reset-password")
  public ApiMessage resetStaffPassword(@PathVariable UUID id,
                                       @Valid @RequestBody ResetStaffPasswordRequest request) {
    authService.resetStaffPassword(id, request.newPassword());
    return new ApiMessage("Staff password reset. Share the new password securely.");
  }

  private AuthResponse writeAuthResponse(HttpServletResponse response, AuthResponse auth) {
    String csrfToken = authCookieService.newCsrfToken();
    authCookieService.writeAuthCookies(response, auth, csrfToken);
    return auth.withCsrfToken(csrfToken);
  }
}
