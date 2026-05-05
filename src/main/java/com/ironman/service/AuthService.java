package com.ironman.service;

import com.ironman.config.BadRequestException;
import com.ironman.config.JwtService;
import com.ironman.dto.auth.AuthResponse;
import com.ironman.dto.auth.CreateStaffRequest;
import com.ironman.dto.auth.LoginRequest;
import com.ironman.dto.auth.RefreshTokenRequest;
import com.ironman.dto.auth.RegisterRequest;
import com.ironman.dto.user.UserSummary;
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
    user = userRepository.save(user);

    var profile = new CustomerProfile();
    profile.setUser(user);
    customerProfileRepository.save(profile);

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
    return UserSummary.from(userRepository.save(user));
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
