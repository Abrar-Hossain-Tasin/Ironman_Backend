package com.ironman.service;

import com.ironman.config.BadRequestException;
import com.ironman.model.AuthCode;
import com.ironman.model.AuthCodePurpose;
import com.ironman.model.User;
import com.ironman.repository.AuthCodeRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and validates short-lived 6-digit codes used for email
 * verification and password resets. Codes are stored hashed; only the
 * plaintext form is ever emailed to the user.
 *
 * Replays are blocked by marking a code consumed; brute force is rate-limited
 * by capping attempts per row at {@link #MAX_ATTEMPTS}.
 */
@Service
@RequiredArgsConstructor
public class AuthCodeService {

  public static final int CODE_TTL_MINUTES = 15;
  public static final int MAX_ATTEMPTS = 5;
  private static final SecureRandom RNG = new SecureRandom();

  private final AuthCodeRepository repository;
  private final PasswordEncoder passwordEncoder;

  /**
   * Generates a new code, invalidates any earlier outstanding ones for this
   * (user, purpose) pair, and returns the plaintext code to be emailed.
   */
  @Transactional
  public String issue(User user, AuthCodePurpose purpose) {
    repository.findByUserAndPurposeAndConsumedAtIsNull(user, purpose).forEach(existing -> {
      existing.setConsumedAt(Instant.now());
      repository.save(existing);
    });

    String code = randomSixDigitCode();
    AuthCode entity = new AuthCode();
    entity.setUser(user);
    entity.setPurpose(purpose);
    entity.setCodeHash(passwordEncoder.encode(code));
    entity.setExpiresAt(Instant.now().plus(CODE_TTL_MINUTES, ChronoUnit.MINUTES));
    repository.save(entity);
    return code;
  }

  /**
   * Verifies the code and marks it consumed. Throws {@link BadRequestException}
   * with a user-safe message on any failure mode.
   */
  @Transactional
  public void consume(User user, AuthCodePurpose purpose, String submittedCode) {
    if (submittedCode == null || submittedCode.isBlank()) {
      throw new BadRequestException("Verification code is required");
    }
    AuthCode entity = repository
        .findTopByUserAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(user, purpose)
        .orElseThrow(() -> new BadRequestException("No active verification code — request a new one"));

    if (entity.getExpiresAt().isBefore(Instant.now())) {
      entity.setConsumedAt(Instant.now());
      repository.save(entity);
      throw new BadRequestException("Verification code expired — request a new one");
    }

    if (entity.getAttempts() >= MAX_ATTEMPTS) {
      entity.setConsumedAt(Instant.now());
      repository.save(entity);
      throw new BadRequestException("Too many attempts — request a new code");
    }

    if (!passwordEncoder.matches(submittedCode.trim(), entity.getCodeHash())) {
      entity.setAttempts(entity.getAttempts() + 1);
      repository.save(entity);
      throw new BadRequestException("Invalid verification code");
    }

    entity.setConsumedAt(Instant.now());
    repository.save(entity);
  }

  private static String randomSixDigitCode() {
    return String.format("%06d", RNG.nextInt(1_000_000));
  }
}
