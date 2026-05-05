package com.ironman.config;

import com.ironman.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final String jwtSecret;
  private final long accessTokenMinutes;
  private final long refreshTokenDays;

  public JwtService(
      @Value("${app.jwt.secret}") String jwtSecret,
      @Value("${app.jwt.access-token-minutes}") long accessTokenMinutes,
      @Value("${app.jwt.refresh-token-days}") long refreshTokenDays
  ) {
    this.jwtSecret = jwtSecret;
    this.accessTokenMinutes = accessTokenMinutes;
    this.refreshTokenDays = refreshTokenDays;
  }

  public String generateAccessToken(User user) {
    return generateToken(user, Instant.now().plus(accessTokenMinutes, ChronoUnit.MINUTES), "access");
  }

  public String generateRefreshToken(User user) {
    return generateToken(user, Instant.now().plus(refreshTokenDays, ChronoUnit.DAYS), "refresh");
  }

  public String subject(String token) {
    return Jwts.parser()
        .verifyWith(signingKey())
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }

  public boolean isValid(String token, User user) {
    try {
      return user.getEmail().equalsIgnoreCase(subject(token));
    } catch (RuntimeException ex) {
      return false;
    }
  }

  private String generateToken(User user, Instant expiresAt, String tokenUse) {
    return Jwts.builder()
        .subject(user.getEmail())
        .claim("uid", user.getId().toString())
        .claim("role", user.getRole().name())
        .claim("token_use", tokenUse)
        .issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(expiresAt))
        .signWith(signingKey())
        .compact();
  }

  private SecretKey signingKey() {
    byte[] bytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < 32) {
      bytes = sha256(bytes);
    }
    return Keys.hmacShaKeyFor(bytes);
  }

  private byte[] sha256(byte[] bytes) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(bytes);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is unavailable", ex);
    }
  }
}
