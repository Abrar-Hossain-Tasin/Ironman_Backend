package com.ironman.service;

import com.ironman.config.BadRequestException;
import com.ironman.config.JwtService;
import com.ironman.dto.auth.AuthResponse;
import com.ironman.dto.auth.CreateStaffRequest;
import com.ironman.dto.auth.LoginRequest;
import com.ironman.dto.auth.RefreshTokenRequest;
import com.ironman.dto.auth.RegisterRequest;
import com.ironman.dto.auth.ResetPasswordRequest;
import com.ironman.dto.auth.VerifyEmailRequest;
import com.ironman.dto.user.UserSummary;
import com.ironman.model.AuthCodePurpose;
import com.ironman.model.CustomerProfile;
import com.ironman.model.User;
import com.ironman.model.UserRole;
import com.ironman.repository.CustomerProfileRepository;
import com.ironman.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final UserRepository userRepository;
  private final CustomerProfileRepository customerProfileRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final AuthCodeService authCodeService;
  private final EmailService emailService;

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmailIgnoreCase(request.email())) {
      throw new BadRequestException("Email is already registered");
    }

    var user = new User();
    user.setFullName(request.fullName());
    user.setEmail(request.email());
    user.setPhone(request.phone());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setRole(UserRole.customer);
    user.setEmailVerified(false);
    user = userRepository.save(user);

    var profile = new CustomerProfile();
    profile.setUser(user);
    customerProfileRepository.save(profile);

    // Fire-and-forget welcome + verification email.
    emailService.sendWelcome(user.getEmail(), user.getFullName());
    String code = authCodeService.issue(user, AuthCodePurpose.email_verification);
    emailService.sendOtpVerification(user.getEmail(), user.getFullName(),
        code, AuthCodeService.CODE_TTL_MINUTES);

    return tokens(user);
  }

  public AuthResponse login(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password())
    );
    var user = userRepository.findByEmailIgnoreCase(request.email())
        .orElseThrow(() -> new BadRequestException("Invalid email or password"));
    return tokens(user);
  }

  public AuthResponse refresh(RefreshTokenRequest request) {
    String email = jwtService.subject(request.refreshToken());
    var user = userRepository.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
    if (!jwtService.isValid(request.refreshToken(), user)) {
      throw new BadRequestException("Invalid refresh token");
    }
    return tokens(user);
  }

  @Transactional
  public UserSummary createStaff(CreateStaffRequest request) {
    if (request.role() == UserRole.customer) {
      throw new BadRequestException("Staff role cannot be customer");
    }
    if (userRepository.existsByEmailIgnoreCase(request.email())) {
      throw new BadRequestException("Email is already registered");
    }
    var user = new User();
    user.setFullName(request.fullName());
    user.setEmail(request.email());
    user.setPhone(request.phone());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setRole(request.role());
    // Staff accounts created by an admin are trusted — skip verification.
    user.setEmailVerified(true);
    return UserSummary.from(userRepository.save(user));
  }

  // ── Email verification ───────────────────────────────────────────────────

  /** Re-sends a verification code. Idempotent (any previous unconsumed code is invalidated). */
  @Transactional
  public void sendVerificationCode(String email) {
    var user = userRepository.findByEmailIgnoreCase(email).orElse(null);
    // Don’t leak account existence — succeed silently if the user is unknown.
    if (user == null || user.isEmailVerified()) return;
    String code = authCodeService.issue(user, AuthCodePurpose.email_verification);
    emailService.sendOtpVerification(user.getEmail(), user.getFullName(),
        code, AuthCodeService.CODE_TTL_MINUTES);
  }

  @Transactional
  public AuthResponse verifyEmail(VerifyEmailRequest request) {
    var user = userRepository.findByEmailIgnoreCase(request.email())
        .orElseThrow(() -> new BadRequestException("Invalid verification code"));
    if (user.isEmailVerified()) {
      return tokens(user);
    }
    authCodeService.consume(user, AuthCodePurpose.email_verification, request.code());
    user.setEmailVerified(true);
    user = userRepository.save(user);
    return tokens(user);
  }

  // ── Password reset ───────────────────────────────────────────────────────

  /** Sends a reset code. Always succeeds publicly to avoid account enumeration. */
  @Transactional
  public void requestPasswordReset(String email) {
    var user = userRepository.findByEmailIgnoreCase(email).orElse(null);
    if (user == null) return;
    String code = authCodeService.issue(user, AuthCodePurpose.password_reset);
    emailService.sendPasswordReset(user.getEmail(), user.getFullName(),
        code, AuthCodeService.CODE_TTL_MINUTES);
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    var user = userRepository.findByEmailIgnoreCase(request.email())
        .orElseThrow(() -> new BadRequestException("Invalid reset code"));
    authCodeService.consume(user, AuthCodePurpose.password_reset, request.code());
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
    emailService.sendPasswordChanged(user.getEmail(), user.getFullName());
  }

  private AuthResponse tokens(User user) {
    return new AuthResponse(
        jwtService.generateAccessToken(user),
        jwtService.generateRefreshToken(user),
        "Bearer",
        UserSummary.from(user)
    );
  }
}
